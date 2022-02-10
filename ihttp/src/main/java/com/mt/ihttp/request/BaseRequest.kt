package com.mt.ihttp.request

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.mt.ihttp.IHttp
import com.mt.ihttp.IHttp.Companion.instance
import com.mt.ihttp.api.ApiService
import com.mt.ihttp.base.BaseResponse
import com.mt.ihttp.body.ProgressResponseCallBack
import com.mt.ihttp.body.UploadProgressRequestBody
import com.mt.ihttp.cache.ICache
import com.mt.ihttp.cache.converter.IDiskConverter
import com.mt.ihttp.cache.model.CacheMode
import com.mt.ihttp.cache.model.CacheResult
import com.mt.ihttp.exception.ApiException
import com.mt.ihttp.https.HttpsUtils
import com.mt.ihttp.https.HttpsUtils.SSLParams
import com.mt.ihttp.interceptor.*
import com.mt.ihttp.model.HttpHeaders
import com.mt.ihttp.result.ApiResult
import com.mt.ihttp.utils.HttpLog
import com.mt.ihttp.utils.Utils
import kotlinx.coroutines.*
import okhttp3.*
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import java.io.File
import java.io.InputStream
import java.lang.reflect.Type
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier

/**　　┏┓　　　┏┓+ +
 *　┏┛┻━━━┛┻┓ + +
 *　┃　　　　　　　┃ 　
 *　┃　　　━　　　┃ ++ + + +
 * ████━████ ┃+
 *　┃　　　　　　　┃ +
 *　┃　　　┻　　　┃
 *　┃　　　　　　　┃ + +
 *　┗━┓　　　┏━┛
 *　　　┃　　　┃　　　　　　　　　　　
 *　　　┃　　　┃ + + + +
 *　　　┃　　　┃
 *　　　┃　　　┃ +  神兽保佑
 *　　　┃　　　┃    代码无bug　　
 *　　　┃　　　┃　　+　　　　　　　　　
 *　　　┃　 　　┗━━━┓ + +
 *　　　┃ 　　　　　　　┣┓
 *　　　┃ 　　　　　　　┏┛
 *　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　┃┫┫　┃┫┫
 *　　　　┗┻┛　┗┻┛+ + + +
 * @Description: Request（请求体）
 * @Author: CTS
 * @Date: 2020/9/24 16:37
 */
abstract class BaseRequest<R : BaseRequest<R>>() {
    private var baseUrl: String?                                                  //BaseUrl
    private var context: Context? = instance.getContext()

    /**
     * OhHttp相关
     */
    private lateinit var okHttpClient: OkHttpClient

    //OhHttp相关配置变量
    private var cookies: MutableList<Cookie> = ArrayList()                        //用户手动添加的Cookie
    private val networkInterceptors: MutableList<Interceptor> = ArrayList()       //网络拦截器
    private var headers = HttpHeaders()                                           //添加的header
    private var httpParams: MutableMap<String, String> = mutableMapOf()           //添加的param
    private var httpUrl: HttpUrl? = null
    private var proxy: Proxy? = null
    private var sslParams: SSLParams? = null
    private var hostnameVerifier: HostnameVerifier? = null

    /**
     * retrofit
     */
    private lateinit var retrofit: Retrofit
    private var converterFactories: MutableList<Converter.Factory> = ArrayList()
    private lateinit var apiManager: ApiService                                   //通用的的api接口

    /**
     * 缓存相关
     */
    //缓存相关参数
    private var cacheMode: CacheMode = CacheMode.NO_CACHE                         //默认无缓存
    private var cacheTime: Long = -1                                              //缓存时间
    private var cacheKey: String? = null                                          //缓存Key
    private var readTimeOut: Long = 0                                             //读超时
    private var writeTimeOut: Long = 0                                            //写超时
    private var connectTimeout: Long = 0                                          //链接超时

    //缓存相关实例
    private var diskConverter: IDiskConverter? = null                             //设置RxCache磁盘转换器
    private var cache: Cache? = null                                              //OkHttp自带缓存
    private val iCache: ICache by lazy { instance.iCache }                        //RxCache缓存实例

    /**
     * 自定义
     */
    private val interceptors: MutableList<Interceptor> = ArrayList()              //拦截器

    private var sign = true                                                       //是否需要签名
    private var timeStamp = true                                                  //是否需要追加时间戳
    private var accessToken = true                                                //是否需要追加token
    private var isAddParams = true                                                //是否添加公参和其他参数（下载时不需要参数）

    //标识网络请求数据是否已回传，如果网络请求快于硬盘读取则不再加载硬盘缓存
    private var tagIsRemoteFinished = false

    /**
     * 根据当前的请求参数，生成对应的OkClient
     */
    private fun generateOkClient(): OkHttpClient.Builder {
        return if (readTimeOut <= 0 && writeTimeOut <= 0 && connectTimeout <= 0 && sslParams == null && cookies.size == 0 && hostnameVerifier == null && proxy == null && headers.isEmpty) {
            //Log.d("测试启动速度", "创建默认OkClient")
            val builder = instance.getOkHttpClientBuilder()
            //Log.d("测试启动速度", "OkHttpClient.Builder初始化完成")
            for (interceptor in builder!!.interceptors()) {
                if (interceptor is BaseDynamicInterceptor<*>) {
                    interceptor.sign(sign).timeStamp(timeStamp).accessToken(accessToken).addParams(isAddParams)
                }
            }
            //Log.d("测试启动速度", "添加拦截器完成")
            builder
        } else {
            //Log.d("测试启动速度", "创建自定义OkClient")
            val newClientBuilder = instance.getOkHttpClient()!!.newBuilder()
            if (readTimeOut > 0) newClientBuilder.readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
            if (writeTimeOut > 0) newClientBuilder.writeTimeout(writeTimeOut, TimeUnit.MILLISECONDS)
            if (connectTimeout > 0) newClientBuilder.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
            if (hostnameVerifier != null) newClientBuilder.hostnameVerifier(hostnameVerifier)
            if (sslParams != null) newClientBuilder.sslSocketFactory(sslParams!!.sSLSocketFactory, sslParams!!.trustManager)
            if (proxy != null) newClientBuilder.proxy(proxy)
            if (cookies.size > 0) instance.getCookieJar()!!.addCookies(cookies)

            //添加头  头添加放在最前面方便其他拦截器可能会用到
            newClientBuilder.addInterceptor(HeadersInterceptor(headers))
            for (interceptor in interceptors) {
                if (interceptor is BaseDynamicInterceptor<*>) {
                    interceptor.sign(sign).timeStamp(timeStamp).accessToken(accessToken).addParams(isAddParams)
                }
                newClientBuilder.addInterceptor(interceptor)
            }
            for (interceptor in newClientBuilder.interceptors()) {
                if (interceptor is BaseDynamicInterceptor<*>) {
                    interceptor.sign(sign).timeStamp(timeStamp).accessToken(accessToken).addParams(isAddParams)
                }
            }
            if (networkInterceptors.size > 0) {
                for (interceptor in networkInterceptors) {
                    newClientBuilder.addNetworkInterceptor(interceptor)
                }
            }
            newClientBuilder
        }.apply {
            if (cacheMode == CacheMode.DEFAULT) { //okHttp缓存，一般不用
                cache(cache)
            }
        }
    }

    /**
     * 根据当前的请求参数，生成对应的Retrofit
     */
    private fun generateRetrofit(): Retrofit.Builder {
        return if (converterFactories.isEmpty()) {
            //Log.d("测试启动速度", "使用缓存的Retrofit")
            //如果没自定义Retrofit，则直接使用缓存的全局对象
            val builder = instance.getRetrofitBuilder()
            if (!TextUtils.isEmpty(baseUrl)) {
                builder!!.baseUrl(baseUrl)
            }
            builder
        } else {
            //Log.d("测试启动速度", "重新创建新的Retrofit")
            val retrofitBuilder = Retrofit.Builder()
            if (!TextUtils.isEmpty(baseUrl)) retrofitBuilder.baseUrl(baseUrl)
            if (converterFactories.isNotEmpty()) {
                for (converterFactory in converterFactories) {
                    retrofitBuilder.addConverterFactory(converterFactory)
                }
            } else {
                //获取全局的对象重新设置
                val newBuilder = instance.getRetrofitBuilder()
                if (!TextUtils.isEmpty(baseUrl)) {
                    newBuilder!!.baseUrl(baseUrl)
                }
                val listConverterFactory = newBuilder!!.build().converterFactories()
                for (factory in listConverterFactory) {
                    retrofitBuilder.addConverterFactory(factory)
                }
            }
            retrofitBuilder
        }
    }

    /**
     * 根据当前的请求参数，生成对应的RxCache和Cache
     */
    private fun generateRxCache(): ICache.Builder {
        //Log.d("测试启动速度", "初始化RxCache--开始")
        val rxCacheBuilder = instance.getRxCacheBuilder()
        when (cacheMode) {
            CacheMode.NO_CACHE -> {
                val noCacheInterceptor = NoCacheInterceptor()
                interceptors.add(noCacheInterceptor)
                networkInterceptors.add(noCacheInterceptor)
            }
            CacheMode.DEFAULT -> {
                if (cache == null) {
                    var cacheDirectory = instance.getCacheDirectory()
                    if (cacheDirectory == null) {
                        cacheDirectory = File(instance.getContext()!!.cacheDir, "okhttp-cache")
                    } else {
                        if (cacheDirectory.isDirectory && !cacheDirectory.exists()) {
                            cacheDirectory.mkdirs()
                        }
                    }
                    cache = Cache(cacheDirectory, (5 * 1024 * 1024.toLong()).coerceAtLeast(instance.getCacheMaxSize()))
                }
                val cacheControlValue = String.format("max-age=%d", Math.max(-1, cacheTime))
                val rewriteCacheControlInterceptor = CacheInterceptor(instance.getContext(), cacheControlValue)
                val rewriteCacheControlInterceptorOffline = CacheInterceptorOffline(instance.getContext(), cacheControlValue)
                networkInterceptors.add(rewriteCacheControlInterceptor)
                networkInterceptors.add(rewriteCacheControlInterceptorOffline)
                interceptors.add(rewriteCacheControlInterceptorOffline)
            }
            CacheMode.ONLYCACHE, CacheMode.CACHEANDREMOTE, CacheMode.CACHEANDREMOTEDISTINCT -> {
                interceptors.add(NoCacheInterceptor())
                return if (diskConverter == null) {
                    rxCacheBuilder.cachekey(Utils.checkNotNull(cacheKey, "cacheKey == null"))
                            .cacheTime(cacheTime)
                    rxCacheBuilder
                } else {
                    val cacheBuilder = instance.getRxCache().newBuilder()
                    cacheBuilder.diskConverter(diskConverter)
                            .cachekey(Utils.checkNotNull(cacheKey, "cacheKey == null"))
                            .cacheTime(cacheTime)
                    cacheBuilder
                }
            }
        }
        //Log.d("测试启动速度", "初始化RxCache--结束")
        return rxCacheBuilder
    }

    open fun build(): R {
        //Log.d("测试启动速度", "request 开始build--开始")
        val okHttpClientBuilder = generateOkClient()
        okHttpClient = okHttpClientBuilder.build()
        if (cacheMode == CacheMode.DEFAULT) { //okhttp缓存
            okHttpClientBuilder.cache(cache)
        }
        retrofit = generateRetrofit().client(okHttpClient).build()
        //iCache = instance.iCache
        //Log.d("测试启动速度", "request 开始build--apiManager")
        apiManager = retrofit.create(ApiService::class.java)
        //Log.d("测试启动速度", "request 开始build--结束")
        return this as R
    }

    private fun getApiService(): ApiService {
        return build().apiManager
    }

//    //通用的请求，一般post、get啥的都用这个，待删除
//    suspend fun <T : Any> execute(
//            call: suspend (apiService: ApiService, params: MutableMap<String, String>) -> BaseResponse<T>,
//            successBlock: (CoroutineScope.(ApiResult: ApiResult.Success<T>) -> Unit)? = null,
//            errorBlock: (CoroutineScope.(error: ApiResult.Error) -> Unit)? = null) {
//        coroutineScope {
//            try {
//                //Log.d("测试启动速度", "执行请求--开始")
//                async(Dispatchers.IO) {
//                    val response = safeApiCall(call)
//                    //Log.d("测试启动速度", "执行请求--结束")
//                    if (response.isSuccess()) {
//                        tagIsRemoteFinished = true
//
//                        launch(Dispatchers.Main) {
//                            successBlock?.let {
//                                it(ApiResult.Success(response.data))
//                                //Log.d("测试启动速度", "执行请求--结束--回调")
//                            }
//                        }
//                        cacheKey?.let {
//                            iCache.save(it, response.data)
//                        }
//
//                    } else {
//                        tagIsRemoteFinished = true
//                        launch(Dispatchers.Main) {
//                            errorBlock?.let {
//                                it(ApiResult.Error(response.result, response.msg
//                                        ?: "", response.data.toString()))
//                            }
//                        }
//
//                    }
//                }
//
//            } catch (e: Exception) {
//                HttpLog.e(e.message)
//                val ex = ApiException.handleException(e)
//                errorBlock?.let { it(ApiResult.Error(ex.code, ex.message ?: "", "")) }
//                //errorBlock?.let { it(ApiResult.Error(-9999, errorMes, "")) }
//            }
//        }
//    }

    //修改中。。。
    suspend fun <T : Any> executeTest(
            call: suspend (retrofit: Retrofit, params: MutableMap<String, String>) -> BaseResponse<T>,
            successBlock: (CoroutineScope.(ApiResult: ApiResult.Success<T>) -> Unit)? = null,
            errorBlock: (CoroutineScope.(error: ApiResult.Error) -> Unit)? = null) {
        coroutineScope {
            try {
                //Log.d("测试启动速度", "执行请求--开始")
                async(Dispatchers.IO) {
                    val response = safeApiCallTest(call)
                    //Log.d("测试启动速度", "执行请求--结束")
                    if (response.isSuccess()) {
                        tagIsRemoteFinished = true

                        launch(Dispatchers.Main) {
                            successBlock?.let {
                                it(ApiResult.Success(response.data))
                                //Log.d("测试启动速度", "执行请求--结束--回调")
                            }
                        }
                        cacheKey?.let {
                            iCache.save(it, response.data)
                        }

                    } else {
                        tagIsRemoteFinished = true
                        launch(Dispatchers.Main) {
                            errorBlock?.let {
                                it(ApiResult.Error(response.result, response.msg
                                        ?: "", response.data.toString()))
                            }
                        }

                    }
                }

            } catch (e: Exception) {
                HttpLog.e(e.message)
                val ex = ApiException.handleException(e)
                errorBlock?.let { it(ApiResult.Error(ex.code, ex.message ?: "", "")) }
                //errorBlock?.let { it(ApiResult.Error(-9999, errorMes, "")) }
            }
        }
    }

//    //带有返回值的通用的请求，比上面那个多个返回值，待删除
//    suspend fun <T : Any> executeWithResult(
//            call: suspend (apiService: ApiService, params: MutableMap<String, String>) -> BaseResponse<T>,
//            successBlock: (suspend CoroutineScope.(ApiResult: ApiResult.Success<T>) -> Unit)? = null,
//            errorBlock: (suspend CoroutineScope.(error: ApiResult.Error) -> Unit)? = null): ApiResult<T> {
//        return coroutineScope {
//            try {
//                withContext(Dispatchers.IO) {
//                    val response = safeApiCall(call)
//                    if (response.isSuccess()) {
//                        tagIsRemoteFinished = true
//
//                        launch(Dispatchers.Main) {
//                            successBlock?.let {
//                                it(ApiResult.Success(response.data))
//                            }
//                        }
//
//                        cacheKey?.let {
//                            iCache.save(it, response.data)
//                        }
//
//                        ApiResult.Success(response.data)
//
//                    } else {
//                        tagIsRemoteFinished = true
//
//                        launch(Dispatchers.Main) {
//                            errorBlock?.let {
//                                it(ApiResult.Error(response.result, response.msg
//                                        ?: "", response.data.toString()))
//                            }
//                        }
//
//                        ApiResult.Error(response.result, response.msg
//                                ?: "", response.data.toString())
//                    }
//                }
//
//            } catch (e: Exception) {
//                HttpLog.e(e.message)
//                val ex = ApiException.handleException(e)
//                errorBlock?.let { it(ApiResult.Error(ex.code, ex.message ?: "", "")) }
//                ApiResult.Error(ex.code, ex.message ?: "", "")
//            }
//        }
//    }

    //带有返回值的通用的请求，比上面那个多个返回值，修改中。。。
    suspend fun <T : Any> executeWithResultTest(
            call: suspend (retrofit: Retrofit, params: MutableMap<String, String>) -> BaseResponse<T>,
            successBlock: (suspend CoroutineScope.(ApiResult: ApiResult.Success<T>) -> Unit)? = null,
            errorBlock: (suspend CoroutineScope.(error: ApiResult.Error) -> Unit)? = null): ApiResult<T> {
        return coroutineScope {
            try {
                withContext(Dispatchers.IO) {
                    val response = safeApiCallTest(call)
                    if (response.isSuccess()) {
                        tagIsRemoteFinished = true

                        launch(Dispatchers.Main) {
                            successBlock?.let {
                                it(ApiResult.Success(response.data))
                            }
                        }

                        cacheKey?.let {
                            iCache.save(it, response.data)
                        }

                        ApiResult.Success(response.data)

                    } else {
                        tagIsRemoteFinished = true

                        launch(Dispatchers.Main) {
                            errorBlock?.let {
                                it(ApiResult.Error(response.result, response.msg
                                        ?: "", response.data.toString()))
                            }
                        }

                        ApiResult.Error(response.result, response.msg
                                ?: "", response.data.toString())
                    }
                }

            } catch (e: Exception) {
                HttpLog.e(e.message)
                val ex = ApiException.handleException(e)
                errorBlock?.let { it(ApiResult.Error(ex.code, ex.message ?: "", "")) }
                ApiResult.Error(ex.code, ex.message ?: "", "")
            }
        }
    }

    //下载
    suspend fun downloadFile(fileUrl: String): Response<ResponseBody> {
        return coroutineScope {
            getApiService().downloadFile(fileUrl)
        }
    }

    //上传
    suspend fun uploadFile(
            postUrl: String,
            filePath: String,
            fileKey: String? = "fileKey",
            params: MutableMap<String, String>? = mutableMapOf(),
            successBlock: ((apiResult: ApiResult.Success<String>) -> Unit)? = null,
            errorBlock: ((error: ApiResult.Error) -> Unit)? = null,
            progress: ((currentLength: Long, length: Long, process: Double, isDone: Boolean) -> Unit)? = null) {

        try {
            val parts: ArrayList<MultipartBody.Part> = arrayListOf()

            //构建要上传的文件
            val file = File(filePath)
            val requestFile = UploadProgressRequestBody(RequestBody.create(MediaType.parse("application/otcet-stream"), file), ProgressResponseCallBack { bytesWritten, contentLength, currentProgress, done ->
                progress?.let {
                    it(contentLength, bytesWritten, currentProgress, done)
                }
            })
            val body = MultipartBody.Part.createFormData(fileKey, file.name, requestFile)
            parts.add(body)

            //构建参数
            params?.forEach {
                parts.add(MultipartBody.Part.createFormData(it.key, it.value))
            }

            val response = getApiService().uploadFile(postUrl, parts)
            if (response.isSuccess()) {
                successBlock?.let {
                    it(ApiResult.Success(response.data ?: ""))
                }
            } else {
                errorBlock?.let {
                    it(ApiResult.Error(response.result, response.msg ?: "下载失败", ""))
                }
            }
        } catch (e: Exception) {
            HttpLog.e(e.message)
            val ex = ApiException.handleException(e)
            errorBlock?.let { it(ApiResult.Error(ex.code, ex.message ?: "", "")) }
            ApiResult.Error(ex.code, ex.message ?: "", "")
        }
    }

    /**
     * 根据[cacheMode]类型 进行缓存处理
     */
    private suspend fun <T> handleCache(cacheMode: CacheMode, type: Type, successBlock: (suspend CoroutineScope.(success: ApiResult.Success<T>) -> Unit)? = null) {
        when (cacheMode) {
            //其他的暂时没用到，有时间再完善
            CacheMode.CACHEANDREMOTEDISTINCT -> {
                getCacheByKey(type, successBlock)
            }
//            CacheMode.NO_CACHE->{
//
//            }
//            else -> {//默认不缓存
////                Result.Error(-99, errorMes, "")
//            }
        }
    }

    //根据key获取缓存
    private suspend fun <T> getCacheByKey(type: Type, successBlock: (suspend CoroutineScope.(success: ApiResult.Success<T>) -> Unit)? = null) {
        //如果存在缓存，先回调缓存
        try {
            if (!cacheKey.isNullOrBlank() && iCache.containsKey(cacheKey)) {
                withContext(Dispatchers.IO) {
//                    //临时测试
//                    val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0].javaClass
//                    val sss = (javaClass.genericSuperclass as ParameterizedType)
//                    Log.d("临时测试 type:", type.toString())
//                    Log.d("临时测试 ParameterizedType:", sss.ownerType.toString())
//                    Log.d("临时测试 T:", javaClass.toString())
                    val cacheResult: CacheResult<T> = iCache.load(type, cacheKey ?: "", cacheTime)
                    if (!tagIsRemoteFinished) {
//                        ApiResult.Success(cacheResult.data)
                        successBlock?.let {
                            it(ApiResult.Success(cacheResult.data))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            HttpLog.e("读取缓存异常：${e.message}")
        }
    }

    /**
     * 执行缓存
     * 记录一个问题，现在的调用很呆，要把type传进去。 原因是，kotlin泛型擦除：kotlin里获取泛型参数一般使用 inline配合reified关键字
     * 但是这里是用的挂起函数suspend，又不能用inline的。头大~~   后期想辙优化吧~~~
     */
    suspend fun <T : Any> executeCache(type: Type, successBlock: (suspend CoroutineScope.(success: ApiResult.Success<T>) -> Unit)? = null): R {
        if (!tagIsRemoteFinished) {//如果网络请求已回调，则不取缓存，不过现在手机读取本地缓存一般在15ms上下，肯定比接口快，
            handleCache(cacheMode, type, successBlock)
        }
        return this as R
    }

    //之后在这里捕捉通用异常
    private suspend fun <T> safeApiCall(call: suspend (apiService: ApiService, params: MutableMap<String, String>) -> BaseResponse<T>): BaseResponse<T> {
        return try {
            call(getApiService(), getParams())
        } catch (e: Exception) {
            // An exception was thrown when calling the API so we're converting this to an IOException
            HttpLog.e(e.message)
            val ex = ApiException.handleException(e)
            BaseResponse(ex.code, ex.message ?: "", null)
        }
    }

    //之后在这里捕捉通用异常
    private suspend fun <T> safeApiCallTest(call: suspend (retrofit: Retrofit, params: MutableMap<String, String>) -> BaseResponse<T>): BaseResponse<T> {
        return try {
            call(build().retrofit, getParams())
        } catch (e: Exception) {
            // An exception was thrown when calling the API so we're converting this to an IOException
            HttpLog.e(e.message)
            val ex = ApiException.handleException(e)
            BaseResponse(ex.code, ex.message ?: "", null)
        }
    }

    init {
        val config = instance
        baseUrl = config.getBaseUrl()
        if (!TextUtils.isEmpty(baseUrl)) {
            httpUrl = HttpUrl.parse(baseUrl)
        }

        //        if (baseUrl == null) {
        //            baseUrl = httpUrl!!.url().protocol + "://" + httpUrl!!.url().host + "/"
        //        }
        cacheMode = config.getCacheMode() //添加缓存模式
        cacheTime = config.getCacheTime() //缓存时间
        //Okhttp  cache
        cache = config.getHttpCache()
        //默认添加 Accept-Language
        val acceptLanguage = HttpHeaders.getAcceptLanguage()
        if (!TextUtils.isEmpty(acceptLanguage)) headers(HttpHeaders.HEAD_KEY_ACCEPT_LANGUAGE, acceptLanguage)
        //默认添加 User-Agent
        val userAgent = HttpHeaders.getUserAgent()
        if (!TextUtils.isEmpty(userAgent)) headers(HttpHeaders.HEAD_KEY_USER_AGENT, userAgent)
        //添加公共请求参数
        config.getCommonParams()?.let { httpParams.putAll(it) }
        config.getCommonHeaders()?.let { headers.put(it) }
    }

    fun getParams(): MutableMap<String, String> {
        return httpParams
    }

    fun readTimeOut(readTimeOut: Long): R {
        this.readTimeOut = readTimeOut
        return this as R
    }

    fun writeTimeOut(writeTimeOut: Long): R {
        this.writeTimeOut = writeTimeOut
        return this as R
    }

    fun connectTimeout(connectTimeout: Long): R {
        this.connectTimeout = connectTimeout
        return this as R
    }

    fun okCache(cache: Cache?): R {
        this.cache = cache
        return this as R
    }

    fun cacheMode(cacheMode: CacheMode): R {
        this.cacheMode = cacheMode
        return this as R
    }

    fun cacheKey(cacheKey: String?): R {
        this.cacheKey = cacheKey
        return this as R
    }

    fun cacheTime(cacheTime: Long): R {
        var cacheTime = cacheTime
        if (cacheTime <= -1) cacheTime = IHttp.DEFAULT_CACHE_NEVER_EXPIRE.toLong()
        this.cacheTime = cacheTime
        return this as R
    }

    fun baseUrl(baseUrl: String?): R {
        this.baseUrl = baseUrl
        if (!TextUtils.isEmpty(this.baseUrl)) httpUrl = HttpUrl.parse(baseUrl)
        return this as R
    }

    //    public BaseRequest retryCount(int retryCount) {
    //        if (retryCount < 0) throw new IllegalArgumentException("retryCount must > 0");
    //        this.retryCount = retryCount;
    //        return  this;
    //    }
    //
    //    public BaseRequest retryDelay(int retryDelay) {
    //        if (retryDelay < 0) throw new IllegalArgumentException("retryDelay must > 0");
    //        this.retryDelay = retryDelay;
    //        return  this;
    //    }
    //
    //    public BaseRequest retryIncreaseDelay(int retryIncreaseDelay) {
    //        if (retryIncreaseDelay < 0)
    //            throw new IllegalArgumentException("retryIncreaseDelay must > 0");
    //        this.retryIncreaseDelay = retryIncreaseDelay;
    //        return  this;
    //    }
    fun addInterceptor(interceptor: Interceptor): R {
        interceptors.add(Utils.checkNotNull(interceptor, "interceptor == null"))
        return this as R
    }

    fun addNetworkInterceptor(interceptor: Interceptor): R {
        networkInterceptors.add(Utils.checkNotNull(interceptor, "interceptor == null"))
        return this as R
    }

    fun addCookie(name: String?, value: String?): R {
        val builder = Cookie.Builder()
        val cookie = builder.name(name).value(value).domain(httpUrl!!.host()).build()
        cookies.add(cookie)
        return this as R
    }

    fun addCookie(cookie: Cookie): R {
        cookies.add(cookie)
        return this as R
    }

    fun addCookies(cookies: List<Cookie>): R {
        this.cookies.addAll(cookies)
        return this as R
    }

    /**
     * 设置Converter.Factory,默认GsonConverterFactory.create()
     */
    fun addConverterFactory(factory: Converter.Factory): R {
        converterFactories.add(factory)
        return this as R
    }

//    /**
//     * 设置CallAdapter.Factory,默认RxJavaCallAdapterFactory.create()
//     */
//    fun addCallAdapterFactory(factory: CallAdapter.Factory): R {
//        adapterFactories.add(factory)
//        return this as R
//    }

    /**
     * 设置代理
     */
    fun okproxy(proxy: Proxy?): R {
        this.proxy = proxy
        return this as R
    }

    /**
     * 设置缓存的转换器
     */
    fun cacheDiskConverter(converter: IDiskConverter): R {
        diskConverter = Utils.checkNotNull(converter, "converter == null")
        return this as R
    }

    /**
     * https的全局访问规则
     */
    fun hostnameVerifier(hostnameVerifier: HostnameVerifier?): R {
        this.hostnameVerifier = hostnameVerifier
        return this as R
    }

    /**
     * https的全局自签名证书
     */
    fun certificates(vararg certificates: InputStream?): R {
        sslParams = HttpsUtils.getSslSocketFactory(null, null, certificates)
        return this as R
    }

    /**
     * https双向认证证书
     */
    fun certificates(bksFile: InputStream?, password: String?, vararg certificates: InputStream?): R {
        sslParams = HttpsUtils.getSslSocketFactory(bksFile, password, certificates)
        return this as R
    }

    /**
     * 添加头信息
     */
    fun headers(headers: HttpHeaders?): R {
        this.headers.put(headers)
        return this as R
    }

    /**
     * 添加头信息
     */
    fun headers(key: String?, value: String?): R {
        headers.put(key, value)
        return this as R
    }

    /**
     * 移除头信息
     */
    fun removeHeader(key: String?): R {
        headers.remove(key)
        return this as R
    }

    /**
     * 移除所有头信息
     */
    fun removeAllHeaders(): R {
        headers.clear()
        return this as R
    }

    /**
     * 设置参数
     */
    fun params(key: String, value: String): R {
        httpParams.put(key, value)
        return this as R
    }

    fun params(keyValues: Map<String, String>): R {
        httpParams.putAll(keyValues)
        return this as R
    }

    fun removeParam(key: String?): R {
        httpParams.remove(key)
        return this as R
    }

    fun removeAllParams(): R {
        httpParams.clear()
        return this as R
    }

    fun sign(sign: Boolean): R {
        this.sign = sign
        return this as R
    }

    fun timeStamp(timeStamp: Boolean): R {
        this.timeStamp = timeStamp
        return this as R
    }

    fun accessToken(accessToken: Boolean): R {
        this.accessToken = accessToken
        return this as R
    }

    fun isAddParams(isAdd: Boolean): R {
        this.isAddParams = isAdd
        return this as R
    }
    //    /**
    //     * 移除缓存（key）
    //     */
    //    public void removeCache(String key) {
    //        IHttp.Companion.getInstance().getRxCache().remove(key).compose(RxUtil.<Boolean>io_main())
    //                .subscribe(new Consumer<Boolean>() {
    //                    @Override
    //                    public void accept(@NonNull Boolean aBoolean) throws Exception {
    //                        HttpLog.i("removeCache success!!!");
    //                    }
    //                }, new Consumer<Throwable>() {
    //                    @Override
    //                    public void accept(@NonNull Throwable throwable) throws Exception {
    //                        HttpLog.i("removeCache err!!!" + throwable);
    //                    }
    //                });
    //    }
}
