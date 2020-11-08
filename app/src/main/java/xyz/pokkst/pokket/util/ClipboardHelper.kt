package xyz.pokkst.pokket.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class ClipboardHelper{
    companion object {
        fun copyToClipboard(activity: Activity?, text: String?) {
            val clipboard: ClipboardManager? = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("Pokket", text)
            clipboard?.setPrimaryClip(clip)
            activity?.let { Toaster.showToastMessage(it, "copied") }
        }
    }
}