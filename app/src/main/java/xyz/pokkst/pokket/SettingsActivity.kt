package xyz.pokkst.pokket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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
    var deepMenuCount = 0
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_SETTINGS_HIDE_BAR == intent.action) {
                this@SettingsActivity.showDeepMenuBar()
            } else if (Constants.ACTION_SETTINGS_SHOW_BAR == intent.action) {
                this@SettingsActivity.showRootMenuBar()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val settingsButton: ImageView = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            if(deepMenuCount > 0) {
                onBackPressed()
            } else {
                finish()
            }
        }

        showRootMenuBar()

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_SETTINGS_HIDE_BAR)
        filter.addAction(Constants.ACTION_SETTINGS_SHOW_BAR)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    override fun onBackPressed() {
        super.onBackPressed()

        if(deepMenuCount > 0)
            handleDeepMenu()
    }

    private fun handleDeepMenu() {
        deepMenuCount--

        if(deepMenuCount <= 0) {
            deepMenuCount = 0
            showRootMenuBar()
        }
    }
    private fun showDeepMenuBar() {
        deepMenuCount++
        appbar_title.text = resources.getString(R.string.app_name)
        settings_button.setImageResource(R.drawable.navigationback)
    }

    private fun showRootMenuBar() {
        deepMenuCount = 0
        object : Thread() {
            override fun run() {
                if(WalletManager.walletKit?.wallet() != null) {
                    val bch = WalletManager.getBalance(WalletManager.walletKit?.wallet()!!)
                        .toPlainString().toDouble()
                    val fiat = bch * PriceHelper.price
                    val fiatStr = BalanceFormatter.formatBalance(fiat, "0.00")
                    this@SettingsActivity.runOnUiThread {
                        appbar_title.text =
                            "${resources.getString(R.string.appbar_title, bch)} ($${fiatStr})"
                    }
                }
            }
        }.start()
        settings_button.setImageResource(R.drawable.x)
    }
}