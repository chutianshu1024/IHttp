package com.mt.ihttp.utils.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.mt.ihttp.IHttp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.*
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.Executors

/**
 * @Description: 文件下载工具
 * @Author: CTS
 * @Date: 2020/12/3 17:12
 * @Note: 每一次都是单独的一个下载单元
 *        每一个下载单元根据功能暂时分为两种：
 *              1.单线程下载
 *              2.多线程异步下载
 *              注意：这里的多线程下载是针对于当前下载单元的，比如一次下载10个文件，这次下载采用多线程下载。而每次单独调用就是单独一个下载单元
 *
 *        失败重试机制：1.文件下载时，失败会重试，重试次数和间隔时间取配置的值
 *                     2.文件下载成功是，md5校验不通过的也会重试，这个重试是固定重试一次
 *
 *        定时检测是否有下载任务，如果超过三次检测都没有，则关闭channel（方案暂时不太好，暂未实现，通道未关闭）
 */
open class IDownloader(
    private val context: Context,
    private val retryCount: Int = 1,//重试次数，默认1次
    private val retryTime: Long = 1000,//重试间隔时间 单位:ms
    private val threadCount: Int = 4//线程数量，用于多线程下载。 默认线程数量暂定为4
) {

    //创建自定义线程池，多线程下载用到
    private var coroutineDispatcher =
        Executors.newFixedThreadPool(threadCount).asCoroutineDispatcher()

    //下载通道
    private val downLoadChannel = Channel<DownloadBean>()

    //下载任务list
    private val downLoadList = arrayListOf<DownloadBean>()

    //是否同步，默认是同步
    private var isSync: Boolean = true

    //文件名，带后缀
    private var fileName = ""

    //文件保存路径
    private var filePath = ""

    //文件保存uri
    private var uri: Uri? = null

    //已重试的url（下载失败重试的）
    private var retryMapByError = hashMapOf<String, Int>()

    //已重试的url（文件完整性校验不通过重试的）
    private var retryMapByCheck = arrayListOf<String>()

    //下载失败的list，只用于多文件下载
    private var errorList = arrayListOf<DownloadBean>()

    /**
     * 外部调用，进行下载
     * @param list 下载文件url列表
     * @param onDownloadListener 下载监听
     */
    fun <T : DownloadBean> startDownTask(
        list: ArrayList<T>,
        onDownloadListener: OnDownloadListener
    ) {
//        Logger.t("测试下载").d("启动下载，任务数：" + list.size)
        downLoadList.clear()
        downLoadList.addAll(list)
        startDownloadByStrategy(onDownloadListener, downLoadList)
    }

    /**
     * 外部调用，进行下载
     * @param bean 下载文件url
     * @param onDownloadListener 下载监听
     */
    fun startDownTask(bean: DownloadBean, onDownloadListener: OnDownloadListener) {
        startDownloadByStrategy(onDownloadListener, arrayListOf(bean))
    }

    /**
     * 内部调用，用于根据配置调用下载（比如同步/异步，重试机制等）
     * @param list 下载数据源
     */
    private fun <T : DownloadBean> startDownloadByStrategy(
        onDownloadListener: OnDownloadListener,
        list: ArrayList<T>
    ) {
        if (isSync) {
            //单线程同步下载
            GlobalScope.launch {
                //这里是下载线程，接收downLoadChannel去下载，相当于单线程顺序下载
                for (item in downLoadChannel) {
                    executeDown(item, onDownloadListener)
                }
            }

        } else {
            //多线程异步下载
            GlobalScope.launch() {
                for (item in downLoadChannel) {
                    async(coroutineDispatcher) {
                        executeDown(item, onDownloadListener)
                    }
                }
            }
        }

        //发送下载任务
        GlobalScope.launch {
            if (!list.isNullOrEmpty()) {
                list.forEach {
//                    Logger.t("测试下载").d("发送下载任务：" + it.url)
                    downLoadChannel.send(it)
                }
            }

//            //任务发送完之后需要关闭管道
//            downLoadChannel.close()
        }

    }

    //下载，内部调用
    private suspend fun executeDown(bean: DownloadBean, onDownloadListener: OnDownloadListener) {
        download(bean)
            .collect {
                when (it) {
                    is DownloadStatus.DownloadError -> {
                        //下载错误
                        //下载失败默认重试一次
                        if (retryMapByError[it.bean.url] == retryCount) {
                            //如果重试次数已达上限
                            onDownloadListener.onError(it.t, true)
                        } else {
                            onDownloadListener.onError(it.t, false)
                            //重试
                            delay(retryTime)
                            downLoadChannel.send(it.bean)

                            if (retryMapByError[it.bean.url] != null) {
                                retryMapByError[it.bean.url] = (retryMapByError[it.bean.url]
                                    ?: 0) + 1
                            } else {
                                retryMapByError[it.bean.url] = 1
                            }
                        }
                    }
                    is DownloadStatus.DownloadSuccess -> {
                        /**
                         * 下载完成
                         * 如果有md5则需要校验下，如果不匹配，会重新下载一次
                         */
                        if (!it.bean.md5.isNullOrBlank()) {
//                            val md5Temp = FileUtils.getFileMD5ToString(it.uri.path)
                            val md5Temp = getMD5Three(it.uri.path) ?: ""
                            if (md5Temp.equals(it.bean.md5, true)) {
                                onDownloadListener.onSuccess(
                                    it.uri, md5Temp, it.bean.extraMap
                                        ?: mutableMapOf()
                                )
                            } else {
                                if (!retryMapByCheck.contains(it.bean.url)) {//仅下载一次
                                    GlobalScope.launch {
                                        downLoadChannel.send(it.bean)
                                    }
                                    retryMapByCheck.add(it.bean.url)
                                }
                            }
                        } else {
                            //如果没有md5，则直接回调
                            onDownloadListener.onSuccess(
                                it.uri, "", it.bean.extraMap
                                    ?: mutableMapOf()
                            )
                        }
                    }
                    is DownloadStatus.DownloadProcess -> {
                        //下载中
                        onDownloadListener.onProcess(it.currentLength, it.length, it.process)
                    }
                }
            }
    }

    /**
     * 下载  内部调用，用于调用retrofit下载并将返回的流写入文件
     */
    private fun download(bean: DownloadBean) = flow {
        try {
            val response = IHttp.instance.createRequest().isAddParams(false).downloadFile(bean.url)

            var ios: InputStream? = null
            var ops: OutputStream? = null
            var bufferedInputStream: BufferedInputStream? = null
            try {
                response.body()?.let { body ->
                    val length = body.contentLength()
                    val contentType = body.contentType().toString()
                    ios = body.byteStream()
                    val info = try {
                        downloadBuildToOutputStream(contentType)
                    } catch (e: Exception) {
                        emit(DownloadStatus.DownloadError(e, bean))
                        DownloadInfo(null)
                        return@flow
                    }
                    ops = info.ops
                    if (ops == null) {
                        emit(DownloadStatus.DownloadError(RuntimeException("下载出错"), bean))
                        return@flow
                    }
                    //下载的长度
                    var currentLength: Int = 0
                    //写入文件
                    val bufferSize = 1024 * 8
                    val buffer = ByteArray(bufferSize)
                    bufferedInputStream = BufferedInputStream(ios, bufferSize)
                    var readLength: Int = 0
                    bufferedInputStream?.let {
                        while (it?.read(buffer, 0, bufferSize)
                                .also { it0 -> readLength = it0 } != -1
                        ) {
                            ops?.write(buffer, 0, readLength)
                            currentLength += readLength
                            emit(
                                DownloadStatus.DownloadProcess(
                                    currentLength.toLong(),
                                    length,
                                    currentLength.toFloat() / length.toFloat()
                                )
                            )
                        }
                    }

                    if (info.uri != null)
                        emit(DownloadStatus.DownloadSuccess(info.uri, bean))
                    else emit(DownloadStatus.DownloadSuccess(Uri.fromFile(info.file), bean))

                } ?: kotlin.run {
                    emit(DownloadStatus.DownloadError(RuntimeException("下载出错"), bean))
                }
            } catch (e: Exception) {
                emit(DownloadStatus.DownloadError(RuntimeException("下载出错"), bean))
            } finally {
                try {
                    bufferedInputStream?.close()
                } catch (e: Exception) {
                }
                try {
                    ops?.close()
                } catch (e: Exception) {
                }
                try {
                    ios?.close()
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            emit(DownloadStatus.DownloadError(RuntimeException("下载出错"), bean))
        }
    }.flowOn(Dispatchers.IO)

    //生成文件输出流和文件名
    private fun downloadBuildToOutputStream(contentType: String): DownloadInfo {
        return if (uri != null) {
            DownloadInfo(context.contentResolver.openOutputStream(uri!!), uri = uri)
        } else {
            val filePathTemp =
                if (!filePath.isNullOrBlank()) filePath else context.getExternalFilesDir(
                    Environment.DIRECTORY_DOWNLOADS
                )

            val mimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
            val fileNameTemp =
                if (!fileName.isNullOrBlank()) fileName else "${System.currentTimeMillis()}${
                    if (!mimeType.isNullOrBlank() && mimeType != "null") {
                        ".$mimeType"
                    } else {
                        ""
                    }
                }"
            val file = File(filePathTemp.toString(), fileNameTemp)
            DownloadInfo(FileOutputStream(file), file)
        }
    }

    private class DownloadInfo(val ops: OutputStream?, val file: File? = null, val uri: Uri? = null)

    //根据url获取下载bean
    private fun getDownLoadBean(url: String): DownloadBean? {
        downLoadList.forEach {
            if (it.url == url) {
                return it
            }
        }
        return null
    }

    /**
     * 设置文件名
     * @Note: 选填，默认的话是时间戳拼接的
     *        如果是多文件下载，文件名后面会拼接自增序号（暂未实现自增）
     */
    fun setFileName(fileName: String): IDownloader {
        this.fileName = fileName
        return this
    }

    /**
     * 设置文件uri
     * @Note: 选填，默认的话是时间戳拼接的
     */
    fun setFileUri(uri: Uri?): IDownloader {
        this.uri = uri
        return this
    }

    /**
     * 设置存储路径，不包含文件名
     * @Note: 选填，默认会保存到私有目录
     */
    fun setFilePath(filePath: String): IDownloader {
        this.filePath = filePath
        return this
    }

    /**
     * 设置下载模式，同步还是异步
     * @param isSync：是否同步  默认true，同步
     */
    fun setIsSync(isSync: Boolean): IDownloader {
        this.isSync = isSync
        return this
    }

    //私有方法，获取md5
    private fun getMD5Three(path: String?): String? {
        var bi: BigInteger? = null
        try {
            val buffer = ByteArray(8192)
            var len = 0
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            val f = File(path)
            val fis = FileInputStream(f)
            while (fis.read(buffer).also { len = it } != -1) {
                md.update(buffer, 0, len)
            }
            fis.close()
            val b: ByteArray = md.digest()
            bi = BigInteger(1, b)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bi?.toString(16)
    }
}

open class DownloadBean(
    var url: String,//下载地址
    var md5: String?,//文件校验md5
    var extraMap: MutableMap<String, String>?,//[extraMap]：拓展字段，将传过来的字段回调回去，用于一些特殊业务逻辑
    var retryCount: Int? = 1 //已重试次数，内部使用 无需重载。
)

//下载监听
interface OnDownloadListener {
    fun onSuccess(
        uri: Uri,
        md5: String,
        extraMap: MutableMap<String, String> = mutableMapOf()
    )//[extraMap]：拓展字段，将传过来的字段回调回去，用于一些特殊业务逻辑

    fun onError(
        e: Throwable,
        isRetryError: Boolean
    )//isRetryError: true 重试达到上限，又失败了；  false：正常失败或者重试失败

    fun onProcess(currentLength: Long, length: Long, process: Float)

    /**
     * 多文件下载结束时的回调（可选）
     * @param isAllSuccess 是否全部下载完成
     * @param errorList 下载失败的任务list
     */
    fun onMultipleDownResult(isAllSuccess: Boolean, errorList: List<DownloadBean>) {}//多文件下载最终结果，
}

sealed class DownloadStatus {
    class DownloadProcess(val currentLength: Long, val length: Long, val process: Float) :
        DownloadStatus()

    class DownloadError(val t: Throwable, val bean: DownloadBean) : DownloadStatus()
    class DownloadSuccess(val uri: Uri, val bean: DownloadBean) : DownloadStatus()
}