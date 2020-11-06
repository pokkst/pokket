package xyz.pokkst.pokket.wallet

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.kits.SlpAppKit
import org.bitcoinj.kits.SlpBIP47AppKit
import org.bitcoinj.kits.WalletAppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.util.Constants
import java.io.File
import java.util.*
import java.util.concurrent.Executor

class WalletManager {
    companion object {
        lateinit var walletDir: File
        var walletKit: SlpBIP47AppKit? = null
        val parameters: NetworkParameters = MainNetParams.get()
        val walletFileName = "pokket"
        fun startWallet(activity: Activity, seed: String?, newUser: Boolean) {
            walletDir = File(activity.applicationInfo.dataDir)
            setBitcoinSDKThread()

            walletKit = object : SlpBIP47AppKit(parameters, Script.ScriptType.P2PKH, KeyChainGroupStructure.SLP, walletDir, walletFileName) {
                override fun onSetupCompleted() {
                    println("STARTED wallet...")
                    wallet().isAcceptRiskyTransactions = true
                    wallet().allowSpendingUnconfirmedTransactions()
                    refresh(activity, 0)
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                }
            }

            walletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    println("DONE DOWNLOAD")
                    refresh(activity, 100)
                }
                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    refresh(activity, pct.toInt())
                    println(pct)
                }
            })

            val creationDate = if(newUser) System.currentTimeMillis() / 1000L else 1554163098L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, "", creationDate)
                walletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            walletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            walletKit?.setCheckpoints(checkpointsInputStream)

            println("Starting wallet...")
            walletKit?.startAsync()
        }

        fun getBalance(wallet: Wallet): Coin {
            return wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        }


        fun setBitcoinSDKThread() {
            val handler = Handler()
            Threading.USER_THREAD = Executor { handler.post(it) }
        }

        fun refresh(activity: Activity, sync: Int?) {
            val intent = Intent(Constants.ACTION_UPDATE_REFRESH)
            intent.putExtra("sync", sync)
            LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
        }

        fun refresh(activity: Activity) {
            val intent = Intent(Constants.ACTION_UPDATE_REFRESH)
            LocalBroadcastManager.getInstance(activity).sendBroadcast(intent)
        }
    }
}