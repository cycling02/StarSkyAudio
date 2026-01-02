package com.cycling.starsky.config

data class StarSkyConfig(
    val openCache: Boolean = false,
    val notificationEnabled: Boolean = true,
    val autoPlay: Boolean = false,
    val restoreState: Boolean = true
) {
    class Builder {
        private var openCache: Boolean = false
        private var notificationEnabled: Boolean = true
        private var autoPlay: Boolean = false
        private var restoreState: Boolean = true

        fun setOpenCache(open: Boolean) = apply { this.openCache = open }

        fun setNotificationEnabled(enabled: Boolean) = apply { this.notificationEnabled = enabled }

        fun setAutoPlay(autoPlay: Boolean) = apply { this.autoPlay = autoPlay }

        fun setRestoreState(restore: Boolean) = apply { this.restoreState = restore }

        fun build() = StarSkyConfig(
            openCache = openCache,
            notificationEnabled = notificationEnabled,
            autoPlay = autoPlay,
            restoreState = restoreState
        )
    }
}
