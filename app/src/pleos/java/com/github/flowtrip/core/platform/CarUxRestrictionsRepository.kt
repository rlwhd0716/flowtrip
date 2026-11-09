package com.github.flowtrip.core.platform

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.github.flowtrip.domain.model.*
import com.github.flowtrip.domain.repository.UxRestrictionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CarUxRestrictionsRepository(private val context: Context) : UxRestrictionsRepository {
    private val mutableState = MutableStateFlow(UxState())
    override val state = mutableState.asStateFlow()
    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null
    private var generation = 0

    override fun start() {
        if (car != null || Build.VERSION.SDK_INT < 29) return
        val session = ++generation
        mutableState.value = UxState()
        try {
            car = Car.createCar(context, Handler(Looper.getMainLooper()), Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { client, ready ->
                if (generation != session) return@createCar
                if (!ready) {
                    mutableState.value = UxState(message = "차량 UX 서비스 연결이 끊어졌습니다.")
                } else try {
                    val restrictions = client.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
                    manager = restrictions
                    restrictions.registerListener { value -> if (generation == session) update(value) }
                    update(restrictions.currentCarUxRestrictions)
                } catch (_: Exception) { mutableState.value = UxState(message = "차량 UX 제한을 확인할 수 없습니다.") }
            }
        } catch (_: Exception) { mutableState.value = UxState(message = "차량 UX 서비스에 연결할 수 없습니다.") }
    }
    private fun update(value: CarUxRestrictions) {
        val restricted = value.isRequiresDistractionOptimization || value.activeRestrictions != CarUxRestrictions.UX_RESTRICTIONS_BASELINE
        mutableState.value = if (restricted) UxState(UxAccess.RESTRICTED, "차량 UX 제한이 적용 중입니다. 정차 후 사용해 주세요.")
            else UxState(UxAccess.ALLOWED, "차량 플랫폼이 화면 조작을 허용했습니다.")
    }
    override fun stop() {
        generation++
        runCatching { manager?.unregisterListener() }
        manager = null
        runCatching { car?.disconnect() }
        car = null
        mutableState.value = UxState()
    }
}
