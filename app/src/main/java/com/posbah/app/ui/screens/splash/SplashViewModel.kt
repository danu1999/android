package com.posbah.app.ui.screens.splash

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posbah.app.data.repository.AuthRepository
import com.posbah.app.security.DeviceIntegrityGuard
import com.posbah.app.security.IntegrityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SplashRoute {
    object NotReady : SplashRoute()
    object GoToLogin : SplashRoute()
    object GoToLock : SplashRoute()
    object GoToBmpDashboard : SplashRoute()
    data class Blocked(val reason: String) : SplashRoute()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val authRepository: AuthRepository,
    private val integrityChecker: IntegrityChecker
) : ViewModel() {

    private val _route = MutableStateFlow<SplashRoute>(SplashRoute.NotReady)
    val route = _route.asStateFlow()

    init { evaluate() }

    private fun evaluate() {
        viewModelScope.launch {
            // 1) Device integrity guard
            val report = DeviceIntegrityGuard.inspect(appContext)
            if (report.rooted || report.hookFrameworkDetected) {
                _route.value = SplashRoute.Blocked(
                    "Perangkat Anda terdeteksi rooted / memuat hook framework. " +
                        "Demi keamanan data, POSBah tidak dapat dijalankan."
                )
                return@launch
            }

            // 2) Best-effort Play Integrity check (non-blocking)
            //    Run async, do not gate UI on it. Just log signature SHA-256.
            launch { integrityChecker.fetchIntegrityToken() }

            // 3) Session-based routing
            val sub = authRepository.activeUserSub()
            val tenant = authRepository.activeTenantId()
            _route.value = when {
                sub == null -> SplashRoute.GoToLogin
                tenant == null -> SplashRoute.GoToLogin
                else -> SplashRoute.GoToLock // require biometric/PIN to resume
            }
        }
    }
}
