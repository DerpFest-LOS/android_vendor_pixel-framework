package com.google.android.systemui.controls.controller

import android.content.ComponentName

import com.android.systemui.controls.controller.ControlsController
import com.android.systemui.controls.controller.ControlsTileResourceConfiguration
import com.android.systemui.dagger.SysUISingleton
import com.google.android.systemui.res.R

import javax.inject.Inject

@SysUISingleton
class GoogleControlsTileResourceConfigurationImpl @Inject constructor(
    private val controlsController: ControlsController,
): ControlsTileResourceConfiguration {

   val componentName: ComponentName
        get() = controlsController.getPreferredSelection().componentName

    override fun getPackageName(): String? {
        if (componentName.getPackageName().equals(GOOGLE_HOME_PACKAGE)) {
            return componentName.getPackageName()
        }
        return null
    }

    override fun getTileImageId(): Int {
        if (componentName.getPackageName().equals(GOOGLE_HOME_PACKAGE)) {
            return R.drawable.home_controls_icon
        }
        return com.android.systemui.res.R.drawable.controls_icon
    }

    override fun getTileTitleId(): Int {
        if (componentName.getPackageName().equals(GOOGLE_HOME_PACKAGE)) {
            return R.string.home_controls_tile_title
        }
        return com.android.systemui.res.R.string.quick_controls_title
    }

    companion object {
        const val GOOGLE_HOME_PACKAGE: String = "com.google.android.apps.chromecast.app"
    }

}
