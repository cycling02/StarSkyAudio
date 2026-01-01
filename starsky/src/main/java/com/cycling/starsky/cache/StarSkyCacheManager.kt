package com.cycling.starsky.cache

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
class StarSkyCacheManager private constructor(private val context: Context) {

    private var cache: Cache? = null
    private val cacheDir: File = File(context.cacheDir, "starrysky_cache")

    companion object {
        private const val MAX_CACHE_SIZE = 512 * 1024 * 1024L

        @Volatile
        private var instance: StarSkyCacheManager? = null

        fun getInstance(context: Context): StarSkyCacheManager {
            return instance ?: synchronized(this) {
                instance ?: StarSkyCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getCache(): Cache {
        if (cache == null) {
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }

            val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE)
            cache = SimpleCache(cacheDir, evictor)
        }
        return cache!!
    }

    fun createCacheDataSource(
        upstreamFactory: androidx.media3.datasource.DataSource.Factory
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(getCache())
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun clearCache() {
        cache?.release()
        cacheDir.deleteRecursively()
        cache = null
    }

    fun getCacheSize(): Long {
        return cache?.cacheSpace ?: 0L
    }

    fun release() {
        cache?.release()
        cache = null
    }
}