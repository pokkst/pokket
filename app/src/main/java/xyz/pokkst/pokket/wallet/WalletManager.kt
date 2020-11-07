package xyz.pokkst.pokket.wallet

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.kits.*
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
        var multisigWalletKit: MultisigAppKit? = null
        val wallet: Wallet?
            get() {
                return walletKit?.wallet() ?: multisigWalletKit?.wallet()
            }
        val isMultisigKit: Boolean
            get() {
                return multisigWalletKit != null && walletKit == null
            }
        val parameters: NetworkParameters = MainNetParams.get()
        val walletFileName = "pokket"
        fun startWallet(activity: Activity, seed: String?, newUser: Boolean) {
            setBitcoinSDKThread()

            walletKit = object : SlpBIP47AppKit(parameters, Script.ScriptType.P2PKH, KeyChainGroupStructure.SLP, walletDir, walletFileName) {
                override fun onSetupCompleted() {
                    wallet().isAcceptRiskyTransactions = true
                    wallet().allowSpendingUnconfirmedTransactions()
                    refresh(activity, 0)
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            walletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    refresh(activity, 100)
                }
                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    refresh(activity, pct.toInt())
                    println(pct)
                }
            })

            val creationDate = if(newUser) System.currentTimeMillis() / 1000L else 1604647474L
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

        fun startMultisigWallet(activity: Activity, seed: String?, newUser: Boolean, followingKeys: List<DeterministicKey>, m: Int) {
            setBitcoinSDKThread()

            multisigWalletKit = object : MultisigAppKit(parameters, walletDir, "${walletFileName}_multisig", followingKeys, m) {
                override fun onSetupCompleted() {
                    wallet().isAcceptRiskyTransactions = true
                    wallet().allowSpendingUnconfirmedTransactions()
                    refresh(activity, 0)
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        refresh(activity)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            multisigWalletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    refresh(activity, 100)
                }
                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    refresh(activity, pct.toInt())
                    println(pct)
                }
            })

            val creationDate = if(newUser) System.currentTimeMillis() / 1000L else 1604647474L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, "", creationDate)
                multisigWalletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            multisigWalletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            multisigWalletKit?.setCheckpoints(checkpointsInputStream)

            println("Starting multisig wallet...")
            multisigWalletKit?.startAsync()
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