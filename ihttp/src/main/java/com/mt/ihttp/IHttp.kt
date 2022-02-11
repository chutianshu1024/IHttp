package com.mt.ihttp

import android.app.Application
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.mt.ihttp.cache.ICache
import com.mt.ihttp.cache.converter.GsonDiskConverter
import com.mt.ihttp.cache.converter.IDiskConverter
import com.mt.ihttp.cache.model.CacheMode
import com.mt.ihttp.cookie.CookieManger
import com.mt.ihttp.https.HttpsUtils
import com.mt.ihttp.interceptor.HttpLoggingInterceptor
import com.mt.ihttp.model.HttpHeaders
import com.mt.ihttp.request.CommonRequest
import com.mt.ihttp.utils.HttpLog
import com.mt.ihttp.utils.Utils
import okhttp3.*
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.File
import java.io.InputStream
import java.net.Proxy
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession

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
 *
 * @Description: 核心配置类
 * @Author: CTS
 * @Date: 2020/2/26 21:05
 * @Note: 核心配置类，用于配置所有网络请求相关参数及 okHttpClient、retrofit、iCache等的初始化
 */
class IHttp {
    private var mBaseUrl: String? = null                                //全局BaseUrl

    /**
     * Retrofit相关
     *
     */
    private var retrofitBuilder: Retrofit.Builder                       //Retrofit请求Builder

    /**
     * OhHttp相关
     */
    private var okHttpClientBuilder: OkHttpClient.Builder               //OkHttp请求的客户端

    //OhHttp相关配置变量
    private var mCommonHeaders: HttpHeaders? = null                     //全局公共请求头
    private var mCommonParams: MutableMap<String, String>? = null       //全局公共请求参数
    private var cookieJar: CookieManger? = null                         //Cookie管理

    /**
     * 缓存相关配置
     */
    //缓存相关参数
    private var mCacheMode: CacheMode = CacheMode.NO_CACHE              //缓存类型
    private var mCacheTime: Long = -1                                   //缓存时间
    private var mCacheDirectory: File? = null                           //缓存目录
    private var mCacheMaxSize: Long = 300                               //缓存大小

    //缓存相关实例（这些都是可复用实例，仅初始化一次即可）
    private var mCache: Cache? = null                                   //OkHttp缓存对象，OkHttp的缓存一般不用
    private var iCacheBuilder: ICache.Builder                           //ICache请求的Builder
    open var iCache: ICache                                             //ICache

    companion object {
        const val DEFAULT_CACHE_NEVER_EXPIRE = -1 //缓存过期时间，默认永久缓存
        const val DEFAULT_MILLISECONDS = 10000 //超时时间，默认10s
        private var sContext: Context? = null
        val instance: IHttp by lazy { IHttp() }

        //必须在全局Application先调用，获取context上下文，否则缓存无法使用
        fun initHttp(app: Application?) {
            sContext = app
        }
    }

    constructor() {
        Log.v("TAG", "")
        okHttpClientBuilder = OkHttpClient.Builder()
        okHttpClientBuilder.hostnameVerifier(DefaultHostnameVerifier())
        okHttpClientBuilder.connectTimeout(DEFAULT_MILLISECONDS.toLong(), TimeUnit.MILLISECONDS)
        retrofitBuilder = Retrofit.Builder()
        iCacheBuilder = ICache.Builder().init(sContext)
            .diskConverter(GsonDiskConverter()) //目前只支持Serializable和Gson缓存其它可以扩展
        iCache = iCacheBuilder.build()
    }

    /**
     * 获取全局上下文
     */
    fun getContext(): Context? {
        okHttpClientBuilder.build().sslSocketFactory()

        if (sContext == null) throw ExceptionInInitializerError("请先在全局Application中调用 IHttp.initHttp() 初始化！")
        return sContext
    }

    private fun testInitialize() {
        if (sContext == null) throw ExceptionInInitializerError("请先在全局Application中调用 IHttp.initHttp() 初始化！")
    }

    /**
     * 对外暴露 OkHttpClient,方便自定义
     */
    fun getOkHttpClientBuilder(): OkHttpClient.Builder? {
        return okHttpClientBuilder
    }

    fun getOkHttpClient(): OkHttpClient? {
        return okHttpClientBuilder?.build()
    }

    /**
     * 获取全局baseurl
     */
    fun getBaseUrl(): String? {
        return instance.mBaseUrl
    }

    /**
     * 此类是用于主机名验证的基接口。 在握手期间，如果 URL 的主机名和服务器的标识主机名不匹配，
     * 则验证机制可以回调此接口的实现程序来确定是否应该允许此连接。策略可以是基于证书的或依赖于其他验证方案。
     * 当验证 URL 主机名使用的默认规则失败时使用这些回调。如果主机名是可接受的，则返回 true
     */
    class DefaultHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String, session: SSLSession): Boolean {
            return true
        }
    }

    /**
     * 对外暴露 Retrofit,方便自定义
     */
    fun getRetrofitBuilder(): Retrofit.Builder {
        return retrofitBuilder
    }

    /**
     * 对外暴露 RxCache,方便自定义
     */
    fun getRxCacheBuilder(): ICache.Builder {
        return iCacheBuilder
    }

    fun getRxCache(): ICache {
        return iCacheBuilder.build()
    }

    /**
     * 调试模式,默认打开所有的异常调试
     */
    fun debug(tag: String?): IHttp? {
        debug(tag, true)
        return this
    }

    /**
     * 调试模式,第二个参数表示所有catch住的log是否需要打印<br></br>
     * 一般来说,这些异常是由于不标准的数据格式,或者特殊需要主动产生的,
     * 并不是框架错误,如果不想每次打印,这里可以关闭异常显示
     */
    fun debug(tag: String?, isPrintException: Boolean): IHttp? {
        val tempTag = if (TextUtils.isEmpty(tag)) "IHttp_" else tag!!
        if (isPrintException) {
            val loggingInterceptor = HttpLoggingInterceptor(tempTag, isPrintException)
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            okHttpClientBuilder!!.addInterceptor(loggingInterceptor)
        }
        HttpLog.customTagPrefix = tempTag
        HttpLog.allowE = isPrintException
        HttpLog.allowD = isPrintException
        HttpLog.allowI = isPrintException
        HttpLog.allowV = isPrintException
        return this
    }

    /**
     * 添加了自定义log拦截器（因为有些时候需要用到数据加密，log要添加解密逻辑，又没必要单独兼容这个业务，干脆直接自定义拦截器吧）
     * @param logInterceptor 自定义log拦截器
     */
    fun debug(tag: String?, isPrintException: Boolean, logInterceptor: Interceptor): IHttp? {
        val tempTag = if (TextUtils.isEmpty(tag)) "IHttp_" else tag!!
        if (isPrintException) {
            okHttpClientBuilder!!.addInterceptor(logInterceptor)
        }
        HttpLog.customTagPrefix = tempTag
        HttpLog.allowE = isPrintException
        HttpLog.allowD = isPrintException
        HttpLog.allowI = isPrintException
        HttpLog.allowV = isPrintException
        return this
    }

    /**
     * https的全局自签名证书
     */
    fun setCertificates(vararg certificates: InputStream?): IHttp {
        val sslParams: HttpsUtils.SSLParams = HttpsUtils.getSslSocketFactory(
            null,
            null,
            certificates
        )
        okHttpClientBuilder!!.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
        return this
    }

    /**
     * https双向认证证书
     */
    fun setCertificates(
        bksFile: InputStream?,
        password: String?,
        vararg certificates: InputStream?
    ): IHttp {
        val sslParams: HttpsUtils.SSLParams = HttpsUtils.getSslSocketFactory(
            bksFile,
            password,
            certificates
        )
        okHttpClientBuilder!!.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
        return this
    }

    /**
     * 全局cookie存取规则
     */
    fun setCookieStore(cookieManager: CookieManger): IHttp {
        cookieJar = cookieManager
        okHttpClientBuilder!!.cookieJar(cookieJar)
        return this
    }

    /**
     * 获取全局的cookie实例
     */
    fun getCookieJar(): CookieManger? {
        return cookieJar
    }

    /**
     * 全局读取超时时间
     */
    fun setReadTimeOut(readTimeOut: Long): IHttp {
        okHttpClientBuilder!!.readTimeout(readTimeOut, TimeUnit.MILLISECONDS)
        return this
    }

    /**
     * 全局写入超时时间
     */
    fun setWriteTimeOut(writeTimeout: Long): IHttp {
        okHttpClientBuilder!!.writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
        return this
    }

    /**
     * 全局连接超时时间
     */
    fun setConnectTimeout(connectTimeout: Long): IHttp {
        okHttpClientBuilder!!.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
        return this
    }

    /**
     * 全局的缓存模式
     */
    fun setCacheMode(cacheMode: CacheMode): IHttp {
        mCacheMode = cacheMode
        return this
    }

    /**
     * 获取全局的缓存模式
     */
    fun getCacheMode(): CacheMode {
        return mCacheMode
    }

    /**
     * 全局的缓存过期时间
     */
    fun setCacheTime(cacheTime: Long): IHttp {
        var cacheTime = cacheTime
        if (cacheTime <= -1) cacheTime = DEFAULT_CACHE_NEVER_EXPIRE.toLong()
        mCacheTime = cacheTime
        return this
    }

    /**
     * 获取全局的缓存过期时间
     */
    fun getCacheTime(): Long {
        return mCacheTime
    }

    /**
     * 全局的缓存大小,默认50M
     */
    fun setCacheMaxSize(maxSize: Long): IHttp {
        mCacheMaxSize = maxSize
        return this
    }

    /**
     * 获取全局的缓存大小
     */
    fun getCacheMaxSize(): Long {
        return mCacheMaxSize
    }

    /**
     * 全局设置缓存的版本，默认为1，缓存的版本号
     */
    fun setCacheVersion(cacheersion: Int): IHttp {
        require(cacheersion >= 0) { "cacheersion must > 0" }
        iCacheBuilder!!.appVersion(cacheersion)
        return this
    }

    /**
     * 全局设置缓存的路径，默认是应用包下面的缓存
     */
    fun setCacheDirectory(directory: File?): IHttp {
        mCacheDirectory = Utils.checkNotNull(directory, "directory == null")
        iCacheBuilder!!.diskDir(directory)
        return this
    }

    /**
     * 获取缓存的路劲
     */
    fun getCacheDirectory(): File? {
        return mCacheDirectory
    }

    /**
     * 全局设置缓存的转换器
     */
    fun setCacheDiskConverter(converter: IDiskConverter?): IHttp {
        iCacheBuilder!!.diskConverter(Utils.checkNotNull(converter, "converter == null"))
        return this
    }

    /**
     * 全局设置OkHttp的缓存,默认是3天
     */
    fun setHttpCache(cache: Cache?): IHttp {
        mCache = cache
        return this
    }

    /**
     * 获取OkHttp的缓存<br></br>
     */
    fun getHttpCache(): Cache? {
        return mCache
    }

    /**
     * 添加全局公共请求参数
     */
    fun addCommonParams(commonParams: Map<String, String>): IHttp {
        if (mCommonParams == null) mCommonParams = mutableMapOf()
        mCommonParams?.putAll(commonParams)
        return this
    }

    /**
     * 添加全局公共请求参数 Json格式
     */
    fun addCommonParamsJson(commonParams: Map<String, String>): IHttp {
        if (mCommonParams == null) mCommonParams = mutableMapOf()
        mCommonParams?.putAll(commonParams)
        return this
    }

    /**
     * 获取全局公共请求参数
     */
    fun getCommonParams(): Map<String, String>? {
        return mCommonParams
    }

    /**
     * 获取全局公共请求头
     */
    fun getCommonHeaders(): HttpHeaders? {
        return mCommonHeaders
    }

    /**
     * 添加全局公共请求参数
     */
    fun addCommonHeaders(commonHeaders: HttpHeaders?): IHttp {
        if (mCommonHeaders == null) mCommonHeaders = HttpHeaders()
        mCommonHeaders?.put(commonHeaders)
        return this
    }

    /**
     * 添加全局拦截器
     */
    fun addInterceptor(interceptor: Interceptor?): IHttp {
        okHttpClientBuilder!!.addInterceptor(Utils.checkNotNull(interceptor, "interceptor == null"))
        return this
    }

    /**
     * 添加全局网络拦截器
     */
    fun addNetworkInterceptor(interceptor: Interceptor?): IHttp {
        okHttpClientBuilder!!.addNetworkInterceptor(
            Utils.checkNotNull(
                interceptor,
                "interceptor == null"
            )
        )
        return this
    }

    /**
     * 全局设置代理
     */
    fun setOkproxy(proxy: Proxy?): IHttp {
        okHttpClientBuilder!!.proxy(Utils.checkNotNull(proxy, "proxy == null"))
        return this
    }

    /**
     * 全局设置请求的连接池
     */
    fun setOkconnectionPool(connectionPool: ConnectionPool?): IHttp {
        okHttpClientBuilder!!.connectionPool(
            Utils.checkNotNull(
                connectionPool,
                "connectionPool == null"
            )
        )
        return this
    }

    /**
     * 全局为Retrofit设置自定义的OkHttpClient
     */
    fun setOkclient(client: OkHttpClient?): IHttp {
        retrofitBuilder!!.client(Utils.checkNotNull(client, "client == null"))
        return this
    }

    /**
     * 全局设置Converter.Factory,默认GsonConverterFactory.create()
     */
    fun addConverterFactory(factory: Converter.Factory?): IHttp {
        retrofitBuilder!!.addConverterFactory(Utils.checkNotNull(factory, "factory == null"))
        return this
    }

    /**
     * 全局设置CallAdapter.Factory,默认RxJavaCallAdapterFactory.create()
     */
    fun addCallAdapterFactory(factory: CallAdapter.Factory?): IHttp {
        retrofitBuilder!!.addCallAdapterFactory(Utils.checkNotNull(factory, "factory == null"))
        return this
    }

    /**
     * 全局设置Retrofit callbackExecutor
     */
    fun setCallbackExecutor(executor: Executor?): IHttp {
        retrofitBuilder!!.callbackExecutor(Utils.checkNotNull(executor, "executor == null"))
        return this
    }

    /**
     * 全局设置Retrofit对象Factory
     */
    fun setCallFactory(factory: Call.Factory?): IHttp {
        retrofitBuilder!!.callFactory(Utils.checkNotNull(factory, "factory == null"))
        return this
    }

    /**
     * 全局设置baseurl
     */
    fun setBaseUrl(baseUrl: String?): IHttp {
        mBaseUrl = Utils.checkNotNull(baseUrl, "baseUrl == null")
        return this
    }

    /**
     * 创建通用请求request
     */
    fun createRequest(): CommonRequest {
        return CommonRequest()
    }

//    /**
//     * 清空缓存
//     */
//    fun clearCache() {
//        getRxCache().clear().compose(RxUtil.< Boolean > io_main < kotlin . Boolean ? > ())
//                .subscribe(Consumer<Boolean?> { HttpLog.i("clearCache success!!!") }, Consumer<Throwable?> { HttpLog.i("clearCache err!!!") })
//    }
//
//    /**
//     * 移除缓存（key）
//     */
//    fun removeCache(key: String?) {
//       getRxCache().remove(key).compose(RxUtil.< Boolean > io_main < kotlin . Boolean ? > ()).subscribe(Consumer<Boolean?> { HttpLog.i("removeCache success!!!") }, Consumer<Throwable?> { HttpLog.i("removeCache err!!!") })
//    }

}