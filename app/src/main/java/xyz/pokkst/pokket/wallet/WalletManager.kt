package xyz.pokkst.pokket.wallet

import android.app.Activity
import android.content.Intent
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.kits.SlpBIP47AppKit
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.params.TestNet4Params
import org.bitcoinj.script.Script
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.livedata.Event
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.PrefsHelper
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
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
        private val _syncPercentage: MutableLiveData<Int> = MutableLiveData(0)
        val syncPercentage: LiveData<Int> = _syncPercentage
        private val _refreshEvents: MutableLiveData<Event<String>> = MutableLiveData()
        val refreshEvents: LiveData<Event<String>> = _refreshEvents
        private val _peerCount: MutableLiveData<Int> = MutableLiveData(0)
        val peerCount: LiveData<Int> = _peerCount
        const val walletFileName = "pokket"
        const val multisigWalletFileName = "pokket_multisig"
        fun startWallet(activity: Activity, seed: String?, newUser: Boolean) {
            setBitcoinSDKThread()

            walletKit = object : SlpBIP47AppKit(
                parameters,
                Script.ScriptType.P2PKH,
                KeyChainGroupStructure.SLP,
                walletDir,
                walletFileName
            ) {
                override fun onSetupCompleted() {
                    wallet().isAcceptRiskyTransactions = true
                    wallet().allowSpendingUnconfirmedTransactions()
                    _syncPercentage.postValue(0)
                    _refreshEvents.postValue(Event(""))
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    peerGroup()?.addConnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            walletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    _syncPercentage.postValue(100)
                }

                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    _syncPercentage.postValue(pct.toInt())
                }
            })

            val creationDate = if (newUser) System.currentTimeMillis() / 1000L else 1604647474L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, "", creationDate)
                walletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            walletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            walletKit?.setCheckpoints(checkpointsInputStream)
            setupNodeOnStart()
            println("Starting wallet...")
            walletKit?.startAsync()
        }

        fun startMultisigWallet(
            activity: Activity,
            seed: String?,
            newUser: Boolean,
            followingKeys: List<DeterministicKey>,
            m: Int
        ) {
            setBitcoinSDKThread()

            multisigWalletKit = object :
                MultisigAppKit(parameters, walletDir, multisigWalletFileName, followingKeys, m) {
                override fun onSetupCompleted() {
                    wallet().isAcceptRiskyTransactions = true
                    wallet().allowSpendingUnconfirmedTransactions()
                    _syncPercentage.postValue(0)
                    _refreshEvents.postValue(Event(""))
                    wallet().addCoinsReceivedEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    wallet().addCoinsSentEventListener { wallet, tx, prevBalance, newBalance ->
                        _refreshEvents.postValue(Event(tx.txId.toString()))
                    }
                    peerGroup()?.addConnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            multisigWalletKit?.setDownloadListener(object : DownloadProgressTracker() {
                override fun doneDownload() {
                    super.doneDownload()
                    _syncPercentage.postValue(100)
                }

                override fun progress(pct: Double, blocksSoFar: Int, date: Date?) {
                    super.progress(pct, blocksSoFar, date)
                    _syncPercentage.postValue(pct.toInt())
                }
            })

            val creationDate = if (newUser) System.currentTimeMillis() / 1000L else 1604647474L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, "", creationDate)
                multisigWalletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            multisigWalletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            multisigWalletKit?.setCheckpoints(checkpointsInputStream)
            setupNodeOnStart()
            println("Starting multisig wallet...")
            multisigWalletKit?.startAsync()
        }

        private fun setupNodeOnStart() {
            val nodeIP = PrefsHelper.instance(null)?.getString("node_ip", null)
            if (nodeIP?.isNotEmpty() == true) {
                var node1: InetAddress? = null
                try {
                    node1 = InetAddress.getByName(nodeIP)
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                }

                if(isMultisigKit) {
                    this.multisigWalletKit?.setPeerNodes(null)
                    this.multisigWalletKit?.setPeerNodes(PeerAddress(parameters, node1))
                } else {
                    this.walletKit?.setPeerNodes(null)
                    this.walletKit?.setPeerNodes(PeerAddress(parameters, node1))
                }
            }
        }

        fun getBalance(wallet: Wallet): Coin {
            return wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        }


        fun setBitcoinSDKThread() {
            val handler = Handler()
            Threading.USER_THREAD = Executor { handler.post(it) }
        }

        fun stopWallets() {
            walletKit?.stopAsync()
            multisigWalletKit?.stopAsync()
            walletKit?.awaitTerminated()
            multisigWalletKit?.awaitTerminated()
            walletKit = null
            multisigWalletKit = null
        }
    }
}