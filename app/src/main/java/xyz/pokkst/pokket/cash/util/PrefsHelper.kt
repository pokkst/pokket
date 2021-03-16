package xyz.pokkst.pokket.cash.util

import android.content.Context
import android.content.SharedPreferences

class PrefsHelper {
    companion object {
        private var prefs: SharedPreferences? = null

        fun instance(context: Context?): SharedPreferences? {
            if (prefs == null) {
                prefs = context?.getSharedPreferences(
                        context.applicationInfo?.packageName,
                        Context.MODE_PRIVATE
                )
            }

            return prefs
        }
    }
}