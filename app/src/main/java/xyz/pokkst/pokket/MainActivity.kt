package xyz.pokkst.pokket

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_main.*
import xyz.pokkst.pokket.ui.ToggleViewPager
import xyz.pokkst.pokket.ui.main.SectionsPagerAdapter
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.PriceHelper
import xyz.pokkst.pokket.wallet.WalletManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class MainActivity : AppCompatActivity() {
    var inFragment = false
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_UPDATE_RECEIVE_QR == intent.action) {
                if(intent.extras?.containsKey("sync") == true) {
                    val pct = intent.extras?.getInt("sync")
                    this@MainActivity.refresh(pct)
                } else {
                    this@MainActivity.refresh()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val decorView = window.decorView
        var flags = decorView.systemUiVisibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags = flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        decorView.systemUiVisibility = flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = resources.getColor(R.color.statusBarLight)
            window.navigationBarColor = resources.getColor(R.color.navBarLight)
        }
        val extras = intent.extras
        var seed: String? = null
        var newUser: Boolean = false
        if (extras != null) {
            seed = extras.getString("seed")
            newUser = extras.getBoolean("new")
        }
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        if(newUser) { viewPager.currentItem = 2 }
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        val settingsButton: ImageView = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            if(viewPager.isPagingEnabled()) {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                startActivity(intentSettings)
            } else {
                disableSendScreen()
            }
        }

        pay_button.setOnClickListener {
            pay_button.isEnabled = false
            val intent = Intent(Constants.ACTION_FRAGMENT_SEND_SEND)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_UPDATE_RECEIVE_QR)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        WalletManager.startWallet(this, seed, newUser)
    }

    override fun onBackPressed() {
        if(!inFragment) {
            val a = Intent(Intent.ACTION_MAIN)
            a.addCategory(Intent.CATEGORY_HOME)
            a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(a)
        } else {
            super.onBackPressed()
        }
    }

    private fun refresh(sync: Int?) {
        if(WalletManager.walletKit?.wallet() != null) {
            object : Thread() {
                override fun run() {
                    super.run()
                    val bch = WalletManager.getBalance(WalletManager.walletKit?.wallet()!!).toPlainString().toDouble()
                    val fiat = bch * PriceHelper.price
                    val fiatStr = formatBalance(fiat, "0.00")
                    this@MainActivity.runOnUiThread {
                        appbar_title.text = "${resources.getString(R.string.appbar_title, bch)} ($${fiatStr})"
                    }
                }
            }.start()
        }
        if (sync != null) {
            sync_progress_bar.visibility = if(sync == 100) View.INVISIBLE else View.VISIBLE
            sync_progress_bar.progress = sync
        }
    }

    private fun refresh() {
        if(WalletManager.walletKit?.wallet() != null) {
            object : Thread() {
                override fun run() {
                    super.run()
                    val bch = WalletManager.getBalance(WalletManager.walletKit?.wallet()!!).toPlainString().toDouble()
                    val fiat = bch * PriceHelper.price
                    val fiatStr = formatBalance(fiat, "0.00")
                    this@MainActivity.runOnUiThread {
                        appbar_title.text = "${resources.getString(R.string.appbar_title, bch)} ($${fiatStr})"
                    }
                }
            }.start()
        }
    }

    fun enableSendScreen() {
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        val tabs: TabLayout = findViewById(R.id.tabs)
        viewPager.setPagingEnabled(false)
        settings_button.setImageResource(R.drawable.navigationback)
        pay_button.visibility = View.VISIBLE
        pay_button.isEnabled = true
        tabs.visibility = View.GONE
        inFragment = true
    }

    fun enableTokensScreen() {
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        val tabs: TabLayout = findViewById(R.id.tabs)
        viewPager.setPagingEnabled(false)
        settings_button.setImageResource(R.drawable.navigationback)
        tabs.visibility = View.GONE
        inFragment = true
    }

    fun disableSendScreen() {
        val viewPager: ToggleViewPager = findViewById(R.id.view_pager)
        val tabs: TabLayout = findViewById(R.id.tabs)
        viewPager.setPagingEnabled(true)
        settings_button.setImageResource(R.drawable.burger)
        tabs.visibility = View.VISIBLE
        pay_button.visibility = View.GONE
        val intent = Intent(Constants.ACTION_MAIN_ENABLE_PAGER)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        inFragment = false
    }

    fun formatBalance(amount: Double, pattern: String): String {
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return formatter.format(amount)
    }
}