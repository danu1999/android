package com.posbah.app.security

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Best-effort runtime defense against rooted devices and emulators.
 * These checks are heuristics; a determined attacker on a rooted device can
 * still bypass them. But they raise the cost significantly and catch
 * automated farm-style cloning attempts.
 */
object DeviceIntegrityGuard {

    data class Report(
        val rooted: Boolean,
        val emulator: Boolean,
        val debuggable: Boolean,
        val hookFrameworkDetected: Boolean,
        val reasons: List<String>
    ) {
        val passed: Boolean get() = !rooted && !emulator && !hookFrameworkDetected
    }

    fun inspect(context: Context): Report {
        val reasons = mutableListOf<String>()

        val rooted = detectRoot().also { if (it) reasons += "ROOT_DETECTED" }
        val emulator = detectEmulator().also { if (it) reasons += "EMULATOR_DETECTED" }
        val debuggable = (context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) reasons += "DEBUGGABLE_BUILD"
        val hook = detectHookFramework(context).also { if (it) reasons += "HOOK_FRAMEWORK_DETECTED" }

        return Report(
            rooted = rooted,
            emulator = emulator,
            debuggable = debuggable,
            hookFrameworkDetected = hook,
            reasons = reasons
        )
    }

    private fun detectRoot(): Boolean {
        // Indicator 1: common su / busybox binaries
        val suspiciousPaths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/su",
            "/system/usr/we-need-root/su",
            "/system/xbin/busybox",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su"
        )
        if (suspiciousPaths.any { File(it).exists() }) return true

        // Indicator 2: build tags
        if (Build.TAGS?.contains("test-keys") == true) return true

        // Indicator 3: Magisk hidden mount
        runCatching {
            val mounts = File("/proc/mounts").readText()
            if (mounts.contains("magisk") || mounts.contains("MagiskSU")) return true
        }

        // Indicator 4: writable system partitions
        val systemDirs = arrayOf("/system", "/system/bin", "/system/sbin", "/system/xbin", "/vendor/bin", "/sbin")
        if (systemDirs.any { File(it).canWrite() }) return true

        return false
    }

    private fun detectEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()

        val signals = listOf(
            fp.startsWith("generic"),
            fp.startsWith("unknown"),
            fp.contains("sdk_gphone"),
            fp.contains("emulator"),
            fp.contains("sdk_x86"),
            model.contains("google_sdk"),
            model.contains("emulator"),
            model.contains("android sdk"),
            model.contains("sdk_gphone"),
            product.contains("sdk"),
            product.contains("emulator"),
            product.contains("simulator"),
            brand.startsWith("generic") && device.startsWith("generic"),
            hardware.contains("goldfish"),
            hardware.contains("ranchu"),
            manufacturer.contains("genymotion"),
            "qc_reference_phone" == product && manufacturer.lowercase() != "xiaomi"
        )
        return signals.any { it }
    }

    private fun detectHookFramework(context: Context): Boolean {
        val pm = context.packageManager
        val hookers = arrayOf(
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "io.va.exposed",
            "com.topjohnwu.magisk",
            "com.koushikdutta.rommanager",
            "com.noshufou.android.su",
            "eu.chainfire.supersu",
            "com.thirdparty.superuser",
            "com.zachspong.temprootremovejb",
            "com.formyhm.hideroot",
            "com.amphoras.hidemyroot",
            "com.devadvance.rootcloak"
        )
        for (p in hookers) {
            runCatching { pm.getPackageInfo(p, 0); return true }
        }

        // Frida default port / library
        runCatching {
            val maps = File("/proc/self/maps")
            if (maps.exists()) {
                val content = maps.readText()
                if (content.contains("frida") || content.contains("gum-js-loop")) return true
            }
        }
        return false
    }
}
