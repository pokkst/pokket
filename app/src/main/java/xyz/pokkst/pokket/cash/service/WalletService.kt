package xyz.pokkst.pokket.cash.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.PeerAddress
import org.bitcoinj.core.TransactionConfidence
import org.bitcoinj.core.TransactionOutput
import org.bitcoinj.core.listeners.DownloadProgressTracker
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.kits.BIP47AppKit
import org.bitcoinj.kits.MultisigAppKit
import org.bitcoinj.kits.WalletKitCore
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.protocols.fusion.FusionClient
import org.bitcoinj.protocols.fusion.models.FusionStatus
import org.bitcoinj.script.Script
import org.bitcoinj.utils.Threading
import org.bitcoinj.wallet.DeterministicSeed
import org.bitcoinj.wallet.KeyChainGroupStructure
import org.bitcoinj.wallet.Wallet
import org.torproject.jni.TorService
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Credentials
import org.web3j.crypto.MnemonicUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.livedata.Event
import xyz.pokkst.pokket.cash.util.PrefsHelper
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong
import org.bitcoinj.protocols.fusion.FusionListener
import org.bitcoinj.protocols.fusion.models.PoolStatus
import java.net.ServerSocket


data class WalletStartupConfig(val activity: Activity, val seed: String?, val newUser: Boolean, val passphrase: String?, val derivationPath: KeyChainGroupStructure?)
data class MultisigWalletStartupConfig(val activity: Activity, val seed: String?, val newUser: Boolean, val followingKeys: List<DeterministicKey>, val m: Int)

class WalletService : LifecycleService(), FusionListener {
    companion object {
        private lateinit var instance: WalletService

        fun getInstance(): WalletService {
            return instance
        }

        // CASH FUSION SERVICE
        var fusionClient: FusionClient? = null
        var fusionStatus = FusionStatus.NOT_FUSING
        var poolStatuses = listOf<PoolStatus>()
        private val _status: MutableLiveData<String> = MutableLiveData()
        val status: LiveData<String> = _status
        private val _cashFusionEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
        val cashFusionEnabled: LiveData<Boolean> = _cashFusionEnabled

        fun setEnabled(enabled: Boolean) {
            _cashFusionEnabled.value = enabled
        }

        fun setStatus(status: String) {
            _status.postValue(status)
        }

        // MAIN WALLET SERVICE
        lateinit var walletDir: File
        var web3: Web3j? = null
        var credentials: Credentials? = null
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
        private val _updateUtxosForFusion: MutableLiveData<Event<Int>> = MutableLiveData();
        val updateUtxosForFusion: LiveData<Event<Int>> = _updateUtxosForFusion;
        private val _syncPercentage: MutableLiveData<Int> = MutableLiveData(0)
        val syncPercentage: LiveData<Int> = _syncPercentage
        private val _refreshEvents: MutableLiveData<Event<String>> = MutableLiveData()
        val refreshEvents: LiveData<Event<String>> = _refreshEvents
        private val _peerCount: MutableLiveData<Int> = MutableLiveData(0)
        val peerCount: LiveData<Int> = _peerCount
        const val walletFileName = "pokket"
        const val multisigWalletFileName = "pokket_multisig"
        val parameters: NetworkParameters = MainNetParams.get()

        fun getRandomInputAmount(): Int {
            return Random().nextInt(14)+1
        }
    }

    private fun startPeriodicRefresher() {
        val refreshRunnable = Runnable {
            _refreshEvents.postValue(Event(""))
        }
        val exec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        exec.scheduleAtFixedRate(refreshRunnable, 30L, 20L, TimeUnit.SECONDS)
    }

    fun startWallet(config: WalletStartupConfig) {
        setBitcoinSDKThread()
        startPeriodicRefresher()

        walletKit = object : BIP47AppKit(
            parameters,
            Script.ScriptType.P2PKH,
            config.derivationPath ?: KeyChainGroupStructure.SLP,
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
                    _updateUtxosForFusion.postValue(Event(getRandomInputAmount()))
                }
                peerGroup()?.addConnectedEventListener { peer, peerCount ->
                    _peerCount.postValue(peerCount)
                }
                peerGroup()?.addDisconnectedEventListener { peer, peerCount ->
                    _peerCount.postValue(peerCount)
                }
                val privateMode =
                    PrefsHelper.instance(null)?.getBoolean("private_mode", false) ?: false
                peerGroup()?.isBloomFilteringEnabled = !privateMode
                wallet().saveToFile(vWalletFile)

                _updateUtxosForFusion.postValue(Event(getRandomInputAmount()))

                val web3Seed = wallet().keyChainSeed.mnemonicString
                web3Seed?.let { seed -> initWeb3(seed) }
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

        val creationDate = if (config.newUser) System.currentTimeMillis() / 1000L else 1560281760L
        if (config.seed != null) {
            val deterministicSeed = DeterministicSeed(
                config.seed, null, config.passphrase
                    ?: "", creationDate
            )
            walletKit?.restoreWalletFromSeed(deterministicSeed)
        }

        walletKit?.setBlockingStartup(false)
        val checkpointsInputStream = config.activity.assets.open("checkpoints.txt")
        walletKit?.setCheckpoints(checkpointsInputStream)
        setupNodeOnStart()
        walletKit?.startAsync()
    }

    fun startMultisigWallet(config: MultisigWalletStartupConfig) {
        setBitcoinSDKThread()

        multisigWalletKit = object :
            MultisigAppKit(parameters, walletDir, multisigWalletFileName, config.followingKeys, config.m) {
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

        val creationDate = if (config.newUser) System.currentTimeMillis() / 1000L else 1611541003L
        if (config.seed != null) {
            val deterministicSeed = DeterministicSeed(config.seed, null, "", creationDate)
            multisigWalletKit?.restoreWalletFromSeed(deterministicSeed)
        }

        multisigWalletKit?.setBlockingStartup(false)
        val checkpointsInputStream = config.activity.assets.open("checkpoints.txt")
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

            kit?.setPeerNodes(null)
            kit?.setPeerNodes(PeerAddress(parameters, node1))
        }
    }

    private fun setBitcoinSDKThread() {
        val handler = Handler()
        Threading.USER_THREAD = Executor { handler.post(it) }
    }

    fun stopWallets() {
        web3?.shutdown()
        kit?.stopAsync()
        kit?.awaitTerminated()
        web3 = null
        credentials = null
        walletKit = null
        multisigWalletKit = null
    }

    private fun initWeb3(seed: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val httpService = HttpService("https://smartbch.fountainhead.cash/mainnet")
            web3 = Web3j.build(httpService)

            // generates the same private key as MetaMask would given the seed
            val seedBytes = MnemonicUtils.generateSeed(seed, "")
            val masterKeypair = Bip32ECKeyPair.generateKeyPair(seedBytes)
            val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
            val childKeypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path)
            credentials = Credentials.create(childKeypair)

            _refreshEvents.postValue(Event(""))
        }
    }

    fun getSmartBchAddress(): String? {
        return credentials?.address
    }

    fun setUpdateUtxosForFusion() {
        _updateUtxosForFusion.postValue(Event(getRandomInputAmount()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        instance = this
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var notificationBuilder: NotificationCompat.Builder? = null
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                "xyz.pokkst.pokket.cash.WALLET_SERVICE",
                "Pokket"
            )
        } else {
            ""
        }

        notificationBuilder = NotificationCompat.Builder(this, channelId )
            .setOngoing(true)
            .setTicker("Pokket")
            .setNotificationSilent()
            .setChannelId(channelId)
            .setSmallIcon(R.drawable.app_icon_round)
            .setContentTitle("Pokket")
            .setContentText(FusionStatus.NOT_FUSING.toString())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder = notificationBuilder.setCategory(Notification.CATEGORY_SERVICE)
        }

        startForeground(444, notificationBuilder.build())

        val enabled = PrefsHelper.instance(this)?.getBoolean("use_fusion", true)
        enabled?.let { setEnabled(it) }
        var statusString = ""
        val statusRunnable = Runnable {
            statusString = ""
            val torOnline = isServerSocketInUse(9050)
            statusString += "Tor online: $torOnline"
            if(!torOnline) {
                startService(Intent(this, TorService::class.java))
            }
            val fusionClient = fusionClient
            val utxos = getConfirmedCoins()
            if (fusionClient != null) {
                if (fusionClient.socket.isClosed) {
                    statusString += "Not connected to Fusion socket..."
                    setUpdateUtxosForFusion()
                } else if (fusionStatus == FusionStatus.FAILED) {
                    statusString += "Fusion failed. Restarting..."
                    setUpdateUtxosForFusion()
                } else {
                    if (poolStatuses.isNotEmpty()) {
                        for (status in poolStatuses) {
                            val pct =
                                (((status.players.toDouble() / status.minPlayers.toDouble()) * 100.0)).roundToLong()
                            statusString += if (pct >= 100) {
                                (status.tier.toString() + ": starting in " + status.timeUntilStart + "s") + "\n"
                            } else {
                                (status.tier.toString() + ": " + status.players + "/" + status.minPlayers) + "\n"
                            }
                        }
                        statusString += "\n"
                        statusString += fusionStatus
                    } else if(utxos.isNullOrEmpty()) {
                        statusString = "waiting for confirmed coins"
                    } else if(utxos.isNotEmpty()) {
                        statusString = "could not join any pools"
                    }
                }
            } else {
                val useFusion =
                    PrefsHelper.instance(this@WalletService)?.getBoolean("use_fusion", true)
                if(useFusion == true) {
                    if(utxos.isNullOrEmpty()) {
                        statusString = "waiting for confirmed coins"
                    }
                } else {
                    statusString = "CashFusion disabled"
                }
            }

            _status.postValue(statusString)

            notificationBuilder = notificationBuilder?.setContentText(fusionStatus.toString())
            nm.notify(444, notificationBuilder?.build())
        }
        val feeExec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        feeExec.scheduleAtFixedRate(statusRunnable, 0L, 2, TimeUnit.SECONDS)

        updateUtxosForFusion.observe(this, { event ->
            lifecycleScope.launch(Dispatchers.IO) {
                val useFusion =
                    PrefsHelper.instance(this@WalletService)?.getBoolean("use_fusion", true)
                if (useFusion == true) {
                    var inputCount = event.getContentIfNotHandled()
                    if (inputCount != null && inputCount != 0) {
                        try {
                            val wallet = WalletService.wallet ?: return@launch
                            val utxos = getConfirmedCoins()
                            if (utxos.isNotEmpty()) {
                                val filteredUtxos: ArrayList<TransactionOutput> = ArrayList()
                                if (utxos.size < inputCount) inputCount = utxos.size
                                for (x in 0 until inputCount) {
                                    val utxo: TransactionOutput = utxos[x]
                                    filteredUtxos.add(utxo)
                                }
                                fusionClient = if (fusionClient == null) {
                                    FusionClient(
                                        "cashfusion.electroncash.dk",
                                        8788,
                                        filteredUtxos,
                                        wallet,
                                        this@WalletService
                                    )
                                } else {
                                    fusionClient?.updateUtxos(filteredUtxos)
                                }
                            } else {
                                setStatus("waiting for confirmed coins")
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    } else {
                        setStatus("CashFusion offline")
                    }
                }
            }
        })
    }

    private fun getConfirmedCoins(): List<TransactionOutput> {
        return wallet?.utxos?.shuffled()
            ?.filter { it.parentTransaction?.confidence?.confidenceType == TransactionConfidence.ConfidenceType.BUILDING } ?: emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onPoolStatus(poolStatusList: MutableList<PoolStatus>?) {
        if (poolStatusList?.isNotEmpty() == true) {
            poolStatuses = poolStatusList
        }
    }

    override fun onFusionStatus(status: FusionStatus?) {
        if (status != null) {
            fusionStatus = status
        }
    }

    private fun isServerSocketInUse(port: Int): Boolean {
        return try {
            ServerSocket(port).close()
            false
        } catch (e: IOException) {
            // Could not connect.
            true
        }
    }
}