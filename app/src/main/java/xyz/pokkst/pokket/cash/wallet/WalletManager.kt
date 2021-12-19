package xyz.pokkst.pokket.cash.wallet

import android.app.Activity
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import org.bitcoinj.core.Coin
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.kits.BIP47AppKit
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.kits.WalletKitCore
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.Script
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import org.web3j.protocol.Web3j
import xyz.pokkst.pokket.cash.livedata.Event
import xyz.pokkst.pokket.cash.util.PrefsHelper
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Executor
import org.web3j.protocol.http.HttpService
import org.web3j.crypto.Credentials

import org.web3j.crypto.WalletUtils
import xyz.pokkst.pokket.cash.interactors.BalanceInteractor
import java.lang.Runnable
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class WalletManager {
    companion object {
        lateinit var walletDir: File
        var web3: Web3j? = null
        var credentials: Credentials? = null
        private val password = "rhfk4zr2uPXbxbnhkCMDTQ3yEH3skkuMNVXDojTcCCqWUT6v9YvwFLLSkMZzF" // not actually supposed to be private
        var walletKit: BIP47AppKit? = null
        var multisigWalletKit: MultisigAppKit? = null
        val wallet: Wallet?
            get() {
                return walletKit?.wallet() ?: multisigWalletKit?.wallet()
            }
        val kit: WalletKitCore?
            get() {
                return walletKit ?: multisigWalletKit
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
        const val walletFileName = "pokket2"
        const val multisigWalletFileName = "pokket2_multisig"

        private fun startPeriodicRefresher() {
            val refreshRunnable = Runnable {
                _refreshEvents.postValue(Event(""))
            }
            val exec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
            exec.scheduleAtFixedRate(refreshRunnable, 30L, 20L, TimeUnit.SECONDS)
        }

        fun startWallet(activity: Activity, seed: String?, newUser: Boolean, passphrase: String?) {
            setBitcoinSDKThread()
            startPeriodicRefresher()

            val clientSbchWallet = getClientWalletFile(walletDir)
            val clientExists = clientSbchWallet != null
            if(newUser) {
                initWeb3(seed, false, null)
            } else if(clientExists) {
                initWeb3(null, true, clientSbchWallet)
            } else if(seed != null && !newUser) {
                initWeb3(seed, false, null)
            }

            walletKit = object : BIP47AppKit(
                    parameters,
                    Script.ScriptType.P2PKH,
                    KeyChainGroupStructure.DEFAULT,
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
                    peerGroup()?.addDisconnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    val privateMode = PrefsHelper.instance(null)?.getBoolean("private_mode", false) ?: false
                    peerGroup()?.isBloomFilteringEnabled = !privateMode
                    wallet().saveToFile(vWalletFile)

                    if(seed == null && !clientExists && !newUser) {
                        val web3Seed = wallet().keyChainSeed.mnemonicCode?.joinToString { " " }
                        initWeb3(web3Seed, false, null)
                    }
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

            val creationDate = if (newUser) System.currentTimeMillis() / 1000L else 1560281760L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, passphrase
                        ?: "", creationDate)
                walletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            walletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            walletKit?.setCheckpoints(checkpointsInputStream)
            setupNodeOnStart()
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
                    peerGroup()?.addDisconnectedEventListener { peer, peerCount ->
                        _peerCount.postValue(peerCount)
                    }
                    wallet().saveToFile(vWalletFile)
                }
            }

            multisigWalletKit?.setUseSchnorr(true)

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

            val creationDate = if (newUser) System.currentTimeMillis() / 1000L else 1611541003L
            if (seed != null) {
                val deterministicSeed = DeterministicSeed(seed, null, "", creationDate)
                multisigWalletKit?.restoreWalletFromSeed(deterministicSeed)
            }

            multisigWalletKit?.setBlockingStartup(false)
            val checkpointsInputStream = activity.assets.open("checkpoints.txt")
            multisigWalletKit?.setCheckpoints(checkpointsInputStream)
            setupNodeOnStart()
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

                this.kit?.setPeerNodes(null)
                this.kit?.setPeerNodes(PeerAddress(parameters, node1))
            }
        }

        fun getBalance(wallet: Wallet): Coin {
            return wallet.getBalance(Wallet.BalanceType.ESTIMATED)
        }

        fun getTotalBalance(wallet: Wallet): String {
            return if(web3 != null) {
                val balanceInteractor = BalanceInteractor.getInstance()
                val sbchBalance = balanceInteractor.getSmartBalance()
                val bchBalance = balanceInteractor.getBitcoinBalance()
                val totalBalance = sbchBalance.add(bchBalance)
                totalBalance.toString()
            } else {
                wallet.getBalance(Wallet.BalanceType.ESTIMATED).toPlainString()
            }
        }

        private fun setBitcoinSDKThread() {
            val handler = Handler()
            Threading.USER_THREAD = Executor { handler.post(it) }
        }

        fun stopWallets() {
            kit?.stopAsync()
            kit?.awaitTerminated()
            walletKit = null
            multisigWalletKit = null
        }

        private fun getClientWalletFile(clientDirectory: File): File? {
            return clientDirectory.listFiles().firstOrNull { file -> file.name.contains(".json") }
        }

        private fun initWeb3(seed: String?, clientExists: Boolean, clientSbchWallet: File?) {
            GlobalScope.launch(Dispatchers.IO) {
                val httpService = HttpService("https://smartbch.fountainhead.cash/mainnet")
                web3 = Web3j.build(httpService)
                var walletFile = ""
                if (clientExists) {
                    if (clientSbchWallet != null) walletFile = clientSbchWallet.name
                } else {
                    walletFile = WalletUtils.generateBip39WalletFromMnemonic(password, seed, walletDir).filename
                }
                credentials = WalletUtils.loadCredentials(
                    password,
                    File(walletDir, walletFile)
                )
                _refreshEvents.postValue(Event(""))
            }
        }

        fun getSmartBchAddress(): String? {
            return credentials?.address
        }
    }
}