package com.cycling.starsky.config

import com.cycling.starsky.listener.ConnServiceListener

data class StarSkyConfig(
    val openCache: Boolean = false,
    val notificationEnabled: Boolean = true,
    val autoPlay: Boolean = false,
    val restoreState: Boolean = true,
    val connService: Boolean = true,
    val isStartService: Boolean = false,
    val onlyStartService: Boolean = true,
    val connServiceListener: ConnServiceListener? = null,
    val startForegroundByWorkManager: Boolean = false
) {
    class Builder {
        private var openCache: Boolean = false
        private var notificationEnabled: Boolean = true
        private var autoPlay: Boolean = false
        private var restoreState: Boolean = true
        private var connService: Boolean = true
        private var isStartService: Boolean = false
        private var onlyStartService: Boolean = true
        private var connServiceListener: ConnServiceListener? = null
        private var startForegroundByWorkManager: Boolean = false

        fun setOpenCache(open: Boolean) = apply { this.openCache = open }

        fun setNotificationEnabled(enabled: Boolean) = apply { this.notificationEnabled = enabled }

        fun setAutoPlay(autoPlay: Boolean) = apply { this.autoPlay = autoPlay }

        fun setRestoreState(restore: Boolean) = apply { this.restoreState = restore }

        fun setConnService(connService: Boolean) = apply { this.connService = connService }

        fun setStartService(isStartService: Boolean) = apply { this.isStartService = isStartService }

        fun setOnlyStartService(onlyStartService: Boolean) = apply { this.onlyStartService = onlyStartService }

        fun setConnServiceListener(listener: ConnServiceListener?) = apply { this.connServiceListener = listener }

        fun setStartForegroundByWorkManager(enabled: Boolean) = apply { this.startForegroundByWorkManager = enabled }

        fun build() = StarSkyConfig(
            openCache = openCache,
            notificationEnabled = notificationEnabled,
            autoPlay = autoPlay,
            restoreState = restoreState,
            connService = connService,
            isStartService = isStartService,
            onlyStartService = onlyStartService,
            connServiceListener = connServiceListener,
            startForegroundByWorkManager = startForegroundByWorkManager
        )
    }
}
