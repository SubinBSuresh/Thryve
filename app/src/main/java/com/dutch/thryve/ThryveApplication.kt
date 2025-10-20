package com.dutch.thryve

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


@HiltAndroidApp
class ThryveApplication: Application() {
    companion object {
        val LOG_TAG = "THRYVE"

    }
}