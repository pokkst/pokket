package xyz.pokkst.pokket.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import xyz.pokkst.pokket.R

class StatusBarHelper {
    companion object {
        fun setStatusBarColor(activity: Activity?, color: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val window = activity?.window
                window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                if (activity != null) {
                    window?.statusBarColor = activity.resources.getColor(color)
                }
            }
        }

        fun prepareLightStatusBar(activity: Activity) {
            val decorView = activity.window.decorView
            var flags = decorView.systemUiVisibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            decorView.systemUiVisibility = flags
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.window.statusBarColor = activity.resources.getColor(R.color.statusBarLight)
                activity.window.navigationBarColor =
                    activity.resources.getColor(R.color.navBarLight)
            }
        }
    }
}