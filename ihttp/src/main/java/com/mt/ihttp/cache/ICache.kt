package com.mt.ihttp.cache

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.mt.ihttp.cache.converter.GsonDiskConverter
import com.mt.ihttp.cache.converter.IDiskConverter
import com.mt.ihttp.cache.core.CacheCore
import com.mt.ihttp.cache.core.LruDiskCache
import com.mt.ihttp.cache.model.CacheResult
import com.mt.ihttp.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.Type

/**
 * @Description: 缓存
 * @Author: CTS
 * @Date: 2020/9/24 11:23
 */
class ICache private constructor(builder: Builder) {
    private val context: Context?
    private val cacheCore: CacheCore                        //缓存的核心管理类
    private val cacheKey: String?                           //缓存的key
    private val cacheTime: Long                             //缓存的时间 单位:秒
    private val diskConverter: IDiskConverter?              //缓存的转换器
    private val diskDir: File?                              //缓存的磁盘目录，默认是缓存目录
    private val appVersion: Int                             //缓存的版本
    private val diskMaxSize: Long                           //缓存的磁盘大小

    fun newBuilder(): Builder {
        return Builder(this)
    }

    /**
     * 获取缓存
     * @param type 保存的类型
     * @param key 缓存key
     */
    suspend fun <T> load(type: Type, key: String): CacheResult<T> {
        return withContext(Dispatchers.IO) {
            val sss: T = cacheCore.load(type, key, -1)
            CacheResult(true, sss)
        }
    }

    /**
     * 根据时间读取缓存
     *
     * @param type 保存的类型
     * @param key  缓存key
     * @param time 保存时间
     */
    suspend fun <T> load(type: Type, key: String, time: Long): CacheResult<T> {
        return withContext(Dispatchers.IO) {
            CacheResult(true, cacheCore.load(type, key, time) as T)
        }
    }

    /**
     * 保存
     * @param key   缓存key
     * @param value 缓存Value
     */
    suspend fun <T> save(key: String, value: T): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                cacheCore.save(key, value)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 是否包含
     */
    fun containsKey(key: String?): Boolean {
        return cacheCore.containsKey(key)
//        return withContext(Dispatchers.IO) {
//            cacheCore.containsKey(key)
//        }
    }

    /**
     * 删除缓存
     */
    suspend fun remove(key: String?): Boolean {
        return withContext(Dispatchers.IO) {
            cacheCore.remove(key)
        }
    }

    /**
     * 清空缓存
     */
    suspend fun clear(): Boolean {
        return withContext(Dispatchers.IO) {
            cacheCore.clear()
        }
    }

    fun getCacheTime(): Long {
        return cacheTime
    }

    fun getCacheKey(): String? {
        return cacheKey
    }

    fun getContext(): Context? {
        return context
    }

    fun getCacheCore(): CacheCore {
        return cacheCore
    }

    fun getDiskConverter(): IDiskConverter? {
        return diskConverter
    }

    fun getDiskDir(): File? {
        return diskDir
    }

    fun getAppVersion(): Int {
        return appVersion
    }

    fun getDiskMaxSize(): Long {
        return diskMaxSize
    }

    class Builder {
        var appVersion: Int
        var diskMaxSize: Long = 0
        var diskDir: File? = null
        var diskConverter: IDiskConverter?
        var context: Context? = null
        var cachekey: String? = null
        var cacheTime: Long

        constructor() {
            diskConverter = GsonDiskConverter()
            cacheTime = CACHE_NEVER_EXPIRE
            appVersion = 1
        }

        constructor(iCache: ICache) {
            context = iCache.context
            appVersion = iCache.appVersion
            diskMaxSize = iCache.diskMaxSize
            diskDir = iCache.diskDir
            diskConverter = iCache.diskConverter
            context = iCache.context
            cachekey = iCache.cacheKey
            cacheTime = iCache.cacheTime
        }

        fun init(context: Context?): Builder {
            this.context = context
            return this
        }

        /**
         * 不设置，默认为1
         */
        fun appVersion(appVersion: Int): Builder {
            this.appVersion = appVersion
            return this
        }

        /**
         * 默认为缓存路径
         *
         * @param directory
         * @return
         */
        fun diskDir(directory: File?): Builder {
            diskDir = directory
            return this
        }

        fun diskConverter(converter: IDiskConverter?): Builder {
            diskConverter = converter
            return this
        }

        /**
         * 不设置， 默为认50MB
         */
        fun diskMax(maxSize: Long): Builder {
            diskMaxSize = maxSize
            return this
        }

        fun cachekey(cachekey: String?): Builder {
            this.cachekey = cachekey
            return this
        }

        fun cacheTime(cacheTime: Long): Builder {
            this.cacheTime = cacheTime
            return this
        }

        fun build(): ICache {
//            Log.d("测试启动速度", "初始化RxCache--build--开始")
            if (diskDir == null && context != null) {
                diskDir = getDiskCacheDir(context, "data-cache")
            }
            Utils.checkNotNull(diskDir, "diskDir==null")
            if (!diskDir!!.exists()) {
                diskDir!!.mkdirs()
            }
            if (diskConverter == null) {//默认转换器为Gson，每次实现Serializable贼鸡儿麻烦
                //diskConverter = SerializableDiskConverter()
                diskConverter = GsonDiskConverter()
            }
            if (diskMaxSize <= 0) {
                diskMaxSize = calculateDiskCacheSize(diskDir)
            }
            cacheTime = Math.max(CACHE_NEVER_EXPIRE, cacheTime)
            appVersion = Math.max(1, appVersion)
//            Log.d("测试启动速度", "初始化RxCache--build--结束")
            return ICache(this)
        }

        /**
         * 应用程序缓存原理：
         * 1.当SD卡存在或者SD卡不可被移除的时候，就调用getExternalCacheDir()方法来获取缓存路径，否则就调用getCacheDir()方法来获取缓存路径<br></br>
         * 2.前者是/sdcard/Android/data/<application package>/cache 这个路径<br></br>
         * 3.后者获取到的是 /data/data/<application package>/cache 这个路径<br></br>
         *
         * @param uniqueName 缓存目录
        </application></application> */
        fun getDiskCacheDir(context: Context?, uniqueName: String): File {
            var cacheDir: File?
            cacheDir = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context!!.externalCacheDir
            } else {
                context!!.cacheDir
            }
            if (cacheDir == null) { // if cacheDir is null throws NullPointerException
                cacheDir = context.cacheDir
            }
            return File(cacheDir!!.path + File.separator + uniqueName)
        }

        companion object {
            private const val MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024 // 5MB
            private const val MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
            const val CACHE_NEVER_EXPIRE: Long = -1 //永久不过期
            private fun calculateDiskCacheSize(dir: File?): Long {
                var size: Long = 0
                try {
                    val statFs = StatFs(dir!!.absolutePath)
                    val available = statFs.blockCount.toLong() * statFs.blockSize
                    size = available / 50
                } catch (ignored: IllegalArgumentException) {
                }
                return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE.toLong()), MIN_DISK_CACHE_SIZE.toLong())
            }
        }
    }

    init {
        context = builder.context
        cacheKey = builder.cachekey
        cacheTime = builder.cacheTime
        diskDir = builder.diskDir
        appVersion = builder.appVersion
        diskMaxSize = builder.diskMaxSize
        diskConverter = builder.diskConverter
        cacheCore = CacheCore(LruDiskCache(diskConverter, diskDir, appVersion, diskMaxSize))
    }
}