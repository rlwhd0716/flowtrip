package com.github.flowtrip.core.platform

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.core.content.ContextCompat

object PleosPermissions {
    const val NAVI_ROUTE = "pleos.car.permission.NAVI_ROUTE"
    const val NAVI_ROUTE_SEARCH = "pleos.car.permission.NAVI_ROUTE_SEARCH"
    const val FUSED_LOCATION = "pleos.car.permission.FUSED_LOCATION"
    val required = listOf(NAVI_ROUTE, NAVI_ROUTE_SEARCH, FUSED_LOCATION)

    fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // Device permission definitions are authoritative; do not guess protection levels.
    @Suppress("DEPRECATION")
    fun missingDangerous(context: Context) = required.filter { permission ->
        if (granted(context, permission)) false else try {
            val info = context.packageManager.getPermissionInfo(permission, 0)
            info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
        } catch (_: PackageManager.NameNotFoundException) { false }
    }
}
