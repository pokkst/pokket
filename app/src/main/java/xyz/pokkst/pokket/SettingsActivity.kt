package xyz.pokkst.pokket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_settings.appbar_title
import kotlinx.android.synthetic.main.activity_settings.settings_button
import xyz.pokkst.pokket.util.BalanceFormatter
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.PriceHelper
import xyz.pokkst.pokket.wallet.WalletManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*


class SettingsActivity : AppCompatActivity() {
    var deepMenuCount: MutableLiveData<Int> = MutableLiveData(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val settingsButton: ImageView = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            deepMenuCount.value?.let {
                if(it > 0) {
                    onBackPressed()
                } else {
                    finish()
                }
            }
        }

        deepMenuCount.observe(this, androidx.lifecycle.Observer { count ->
            if(count <= 0) {
                if(count < 0)
                    deepMenuCount.value = 0

                settings_button.setImageResource(R.drawable.x)
            } else {
                settings_button.setImageResource(R.drawable.navigationback)
            }
        })

        appbar_title.text = resources.getString(R.string.app_name)
    }

    override fun onBackPressed() {
        super.onBackPressed()
         adjustDeepMenu(-1)
    }

    fun adjustDeepMenu(byAmount: Int) {
        val previousValue = deepMenuCount.value ?: 0
        val newValue = previousValue + byAmount
        deepMenuCount.value = newValue
    }
}