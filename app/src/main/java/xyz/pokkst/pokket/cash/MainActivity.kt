package xyz.pokkst.pokket.cash

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.wallet.DeterministicKeyChain
import xyz.pokkst.pokket.cash.interactors.BalanceInteractor
import xyz.pokkst.pokket.cash.service.MultisigWalletStartupConfig
import xyz.pokkst.pokket.cash.service.WalletService
import xyz.pokkst.pokket.cash.service.WalletStartupConfig
import xyz.pokkst.pokket.cash.util.*
import java.io.File
import java.math.BigDecimal


class MainActivity : AppCompatActivity() {
    var inFragment = false
    val balanceInteractor = BalanceInteractor.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PrefsHelper.instance(this)
        WalletService.walletDir = File(applicationInfo.dataDir)
        val extras = intent.extras
        var derivationPath: String? = null
        var seed: String? = null
        var newUser: Boolean = false
        var passphrase: String? = null
        var isMultisig: Boolean = false
        val followingKeys = ArrayList<DeterministicKey>()
        var m = 0
        if (extras != null) {
            passphrase = extras.getString(Constants.EXTRA_PASSPHRASE)
            derivationPath = extras.getString(Constants.EXTRA_DERIVATION)
            seed = extras.getString(Constants.EXTRA_SEED)
            newUser = extras.getBoolean(Constants.EXTRA_NEW)
            isMultisig = extras.getBoolean(Constants.EXTRA_MULTISIG)
            val keys = extras.getStringArrayList(Constants.EXTRA_FOLLOWING_KEYS) ?: ArrayList()
            val keysLength = keys.size - 1
            for (x in 0..keysLength) {
                val deterministicKey =
                    DeterministicKey.deserializeB58(keys[x], WalletService.parameters)
                        .setPath(DeterministicKeyChain.BIP44_ACCOUNT_ZERO_PATH)
                followingKeys.add(deterministicKey)
            }
            m = extras.getInt(Constants.EXTRA_M)
        } else {
            if(!newUser && seed == null) {
                val exists = PrefsHelper.instance(this)?.contains(Constants.PREF_DERIVATION_PATH) == true
                if(exists) {
                    val pathPref = PrefsHelper.instance(this)?.getString(Constants.PREF_DERIVATION_PATH, Constants.DERIVATION_PATH_DEFAULT)
                    derivationPath = pathPref
                }
            }
        }

        if (!newUser && seed == null) {
            val multisigWalletFile =
                File(WalletService.walletDir, "${WalletService.multisigWalletFileName}.wallet")
            isMultisig = multisigWalletFile.exists()
        }

        prepareViews()
        setListeners()

        val path = derivationPath?.let { DerivationParser.parse(it) }

        if (isMultisig) {
            val config = MultisigWalletStartupConfig(this, seed, newUser, followingKeys, m)
            WalletService.getInstance().startMultisigWallet(config)
        } else {
            val config = WalletStartupConfig(this, seed, newUser, passphrase, path)
            WalletService.getInstance().startWallet(config)
        }
    }

    private fun prepareViews() {
        StatusBarHelper.prepareLightStatusBar(this)
    }

    private fun setListeners() {
        pay_button.setOnClickListener {
            pay_button.isEnabled = false
            val intent = Intent(Constants.ACTION_FRAGMENT_SEND_SEND)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        appbar_title.setOnClickListener {
            if (isSendScreenEnabled()) {
                val intent = Intent(Constants.ACTION_FRAGMENT_SEND_MAX)
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bchBalance = balanceInteractor.getBitcoinBalance()
                    val sbchBalance = balanceInteractor.getSmartBalance()

                    this@MainActivity.runOnUiThread {
                        val inflater = layoutInflater
                        val dialoglayout: View = inflater.inflate(R.layout.dialog_balances, null)
                        dialoglayout.findViewById<TextView>(R.id.bch_balance_textview)?.text =
                            resources.getString(R.string.bch_balance, bchBalance)
                        dialoglayout.findViewById<TextView>(R.id.sbch_balance_textview)?.text =
                            resources.getString(R.string.bch_balance, sbchBalance)

                        val builder = AlertDialog.Builder(this@MainActivity)
                        builder.setView(dialoglayout)
                        val dialog = builder.show()
                        val swapMinimum = BigDecimal.valueOf(0.01)
                        val swapToSbchButton = dialog?.findViewById<Button>(R.id.swap_to_sbch_button)
                        val swapToBchButton = dialog?.findViewById<Button>(R.id.swap_to_bch_button)

                        if(WalletService.isMultisigKit) {
                            dialog?.findViewById<ImageView>(R.id.sbch_balance_imageview)?.visibility = View.GONE
                            dialog?.findViewById<TextView>(R.id.sbch_balance_textview)?.visibility = View.GONE
                            swapToSbchButton?.visibility = View.GONE
                            swapToBchButton?.visibility = View.GONE
                        }

                        val bchBalanceSatisfied = bchBalance > swapMinimum
                        swapToSbchButton?.setTextColor(ContextCompat.getColor(this@MainActivity, if(bchBalanceSatisfied) R.color.dark_blue else R.color.gray))
                        swapToSbchButton?.isEnabled = bchBalanceSatisfied

                        val sbchBalanceSatisfied = sbchBalance > swapMinimum
                        swapToBchButton?.setTextColor(ContextCompat.getColor(this@MainActivity, if(sbchBalanceSatisfied) R.color.dark_blue else R.color.gray))
                        swapToBchButton?.isEnabled = sbchBalanceSatisfied

                        swapToSbchButton?.setOnClickListener {
                            val intent = Intent(Constants.ACTION_HOP_TO_SBCH)
                            LocalBroadcastManager.getInstance(this@MainActivity)
                                .sendBroadcast(intent)
                            dialog.dismiss()
                        }
                        swapToBchButton?.setOnClickListener {
                            val intent = Intent(Constants.ACTION_HOP_TO_BCH)
                            LocalBroadcastManager.getInstance(this@MainActivity)
                                .sendBroadcast(intent)
                            dialog.dismiss()
                        }

                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    }
                }
            }
        }

        settings_button.setOnClickListener {
            if (!inFragment) {
                val intentSettings = Intent(this, SettingsActivity::class.java)
                startActivity(intentSettings)
            } else {
                toggleSendScreen(false)
            }
        }

        WalletService.syncPercentage.observe(this, Observer { pct ->
            refresh(pct)
        })

        WalletService.refreshEvents.observe(this, Observer { event ->
            if (event != null) {
                refresh()
            }
        })

        WalletService.peerCount.observe(this, Observer { peers ->
            if (peers == 0) {
                appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_disconnected,
                    0
                )
            } else if (peers > 0 && peers < WalletService.parameters.defaultPeerCount) {
                appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_connected_partial,
                    0
                )
            } else if (peers >= WalletService.parameters.defaultPeerCount) {
                appbar_title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_connected,
                    0
                )
            }
        })

        WalletService.cashFusionEnabled.observe(this) { enabled ->
            if (enabled) {
                WalletService.getInstance().setUpdateUtxosForFusion()
            } else {
                try {
                    WalletService.fusionClient?.stopConnection()
                    WalletService.fusionClient = null
                } catch (e: Exception) {

                }
            }
        }
    }

    override fun onBackPressed() {
        if (!inFragment) {
            val a = Intent(Intent.ACTION_MAIN)
            a.addCategory(Intent.CATEGORY_HOME)
            a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(a)
        } else {
            super.onBackPressed()
        }
    }

    private fun refresh(sync: Int?) {
        refresh()
        if (sync != null) {
            sync_progress_bar.visibility = if (sync == 100) View.INVISIBLE else View.VISIBLE
            sync_progress_bar.progress = sync
        }
    }

    private fun refresh() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sbch = balanceInteractor.getSmartBalance()
                val bch = balanceInteractor.getBitcoinBalance()
                val totalBalance = sbch.add(bch)
                val bchStr = BalanceFormatter.formatBalance(totalBalance.toDouble(), "#.########")
                val fiat = totalBalance.toDouble() * PriceHelper.price
                val fiatStr = BalanceFormatter.formatBalance(fiat, "0.00")
                this@MainActivity.runOnUiThread {
                    appbar_title.text =
                        "${resources.getString(R.string.appbar_title, bchStr)} ($${fiatStr})"
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleSendScreen(status: Boolean) {
        val imgResId = if (status) R.drawable.navigationback else R.drawable.burger
        settings_button.setImageResource(imgResId)
        pay_button.visibility = if (status) View.VISIBLE else View.INVISIBLE
        pay_button.isEnabled = status
        inFragment = status

        if (!status) {
            val intent = Intent(Constants.ACTION_MAIN_ENABLE_PAGER)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    fun isSendScreenEnabled(): Boolean {
        return pay_button.visibility == View.VISIBLE
    }

    fun enablePayButton() {
        pay_button.isEnabled = true
    }
}