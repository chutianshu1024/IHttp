package com.mt.ihttp.utils.upload

import com.mt.ihttp.utils.download.DownloadBean
import com.mt.ihttp.IHttp
import com.mt.ihttp.result.ApiResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * @Description: 文件上传工具
 * @Author: CTS
 * @Date: 2020/12/16 10:11
 */
class IUploader {

    interface OnUploadListener {
        fun onSuccess(url: String)
        fun onError(mes: String)
        fun onProgress(currentLength: Long, length: Long, process: Double, isDone: Boolean)
    }

    interface OnUploadMultipleListener {
        fun onSuccess(successList: ArrayList<String>)
        fun onError(successList: ArrayList<String>, errorList: ArrayList<String>)
        fun onProgress(currentLength: Long, length: Long, process: Double, isDone: Boolean)
    }

//    //上传bean（入参，调用上传时传入的下载bean）
//    data class UploadBean(
//            var url: String
//    )

//    //上传bean（出参，上传结束服务器返回的url）
//    data class UploadResultBean(
//            var url: String
//    )

    //上传通道
    private val upLoadChannel = Channel<String>()

    //上传任务list
    private val downLoadList = arrayListOf<DownloadBean>()

    //上传失败的list，只用于多文件下载
    private var errorList = arrayListOf<DownloadBean>()

    //兼容java方案，仅做中转，禁止添加其他逻辑。下载逻辑修改都要在 startUpload() 里
    fun startUploadWithJava(filePath: String,
                            uploadUrl: String,
                            fileKey: String? = "",
                            params: MutableMap<String, String>?,
                            onUploadListener: OnUploadListener) {
        startUpload(filePath, uploadUrl, fileKey, params,
                successBlock = { apiResult ->
                    onUploadListener.onSuccess(apiResult.data ?: "")
                },
                errorBlock = { error ->
                    onUploadListener.onError(error.mes)
                },
                progress = { currentLength, length, process, isDone ->
                    onUploadListener.onProgress(currentLength, length, process, isDone)
                })
//        GlobalScope.launch {
//            IHttp.instance.createRequest().isAddParams(false).uploadFile(uploadUrl, filePath, fileKey, params,
//                    successBlock = { apiResult ->
////                        Log.d("测试上传 成功", apiResult.data)
//                        onUploadListener.onSuccess(apiResult.data ?: "")
//                    },
//                    errorBlock = { error ->
////                        Log.d("测试上传 失败", error.mes)
//                        onUploadListener.onError(error.mes)
//                    },
//                    progress = { currentLength, length, process, isDone ->
////                        Log.d("测试上传 进度", "currentLength:$currentLength  length:$length  process:$process  isDone:$isDone")
//                        onUploadListener.onProgress(currentLength, length, process, isDone)
//                    })
//        }
    }

    //兼容java方案，仅做中转，禁止添加其他逻辑。
    fun startUploadMultipleWithJava(uploadList: List<String>,
                                    uploadUrl: String,
                                    fileKey: String? = "",
                                    params: MutableMap<String, String>?,
                                    onUploadMultipleListener: OnUploadMultipleListener) {
        startUploadList(uploadList, uploadUrl, fileKey, params,
                successBlock = { apiResult ->
                    onUploadMultipleListener.onSuccess(apiResult)
                },
                errorBlock = { successList, errorList ->
                    onUploadMultipleListener.onError(successList, errorList)
                },
                progress = { currentLength, length, process, isDone ->
                    onUploadMultipleListener.onProgress(currentLength, length, process, isDone)
                })
    }

    //启动上传
    fun startUpload(filePath: String,
                    uploadUrl: String,
                    fileKey: String? = "",
                    params: MutableMap<String, String>?,
                    successBlock: ((apiResult: ApiResult.Success<String>) -> Unit),
                    errorBlock: ((error: ApiResult.Error) -> Unit)? = null,
                    progress: ((currentLength: Long, length: Long, process: Double, isDone: Boolean) -> Unit)? = null
    ) {
        GlobalScope.launch {
            //上传因为老逻辑传的参数和公参不同，所以屏蔽公参，单独在这里传参数
            IHttp.instance.createRequest().isAddParams(false).uploadFile(uploadUrl,
                    filePath, fileKey, params, successBlock, errorBlock, progress)
        }
    }

    /**
     * 多文件上传
     * 实现是：多次调用单文件上传
     */
    //暂未测试，测试过删除此行
    fun startUploadList(uploadList: List<String>,
                        uploadUrl: String,
                        fileKey: String? = "",
                        params: MutableMap<String, String>?,
                        successBlock: ((successList: ArrayList<String>) -> Unit),
                        errorBlock: ((successList: ArrayList<String>, errorList: ArrayList<String>) -> Unit)? = null,
                        progress: ((currentLength: Long, length: Long, process: Double, isDone: Boolean) -> Unit)? = null) {
        GlobalScope.launch {
            var uploadSizeTemp = 0
            //上传失败的list
            var errorList = arrayListOf<String>()
            //上传成功的list
            var successList = arrayListOf<String>()

            for (item in upLoadChannel) {
                startUpload(item, uploadUrl, fileKey, params,
                        successBlock = { apiResult ->
                            successList.add(apiResult.data ?: "")
                            uploadSizeTemp++

                            //如果下载成功的个数等于总任务数量，则全部下载成功
                            if (successList.size == uploadList.size) {
                                successBlock?.let {
                                    it(successList)
                                }
                            }
                        },
                        errorBlock = { error ->
                            errorList.add(item)
                            uploadSizeTemp++

                            //如果失败和成功list个数等于总任务数，则下载完成，返回失败
                            if (successList.size + errorList.size == uploadList.size) {
                                errorBlock?.let {
                                    it(successList, errorList)
                                }
                            }
                        },
                        progress = { currentLength, length, process, isDone ->
                            progress?.let {
                                it(currentLength, length, process + uploadSizeTemp / uploadList.size, successList.size + errorList.size == uploadList.size)
                            }
                        })
            }
        }

        //发送下载任务
        GlobalScope.launch {
            if (!uploadList.isNullOrEmpty()) {
                uploadList.forEach {
//                    Log.d("测试上传", "发送上传任务：" + it.url)
                    upLoadChannel.send(it)
                }
            }
        }
    }
}