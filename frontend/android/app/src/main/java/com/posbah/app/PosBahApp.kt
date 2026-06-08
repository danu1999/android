package com.posbah.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entrypoint. Hilt initializes the singleton component here.
 * Heavy work (key derivation, integrity check) is deferred to the first
 * splash screen viewmodel to keep app cold-start fast.
 */
@HiltAndroidApp
class PosBahApp : Application()
