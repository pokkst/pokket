package xyz.pokkst.pokket.cash.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent

import android.os.IBinder

import androidx.core.app.NotificationCompat

import android.app.PendingIntent
import android.content.Context
import android.os.Build

import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.protocols.fusion.models.FusionStatus
import org.bitcoinj.protocols.fusion.models.PoolStatus
import xyz.pokkst.pokket.cash.wallet.WalletManager
import kotlin.math.roundToLong
import org.json.JSONObject
import xyz.pokkst.pokket.cash.livedata.Event
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class YourService : Service() {
    companion object {
        private val _status: MutableLiveData<String> = MutableLiveData()
        val status: LiveData<String> = _status
        private val _cashFusionEnabled: MutableLiveData<Boolean> = MutableLiveData(false)
        val cashFusionEnabled: LiveData<Boolean> = _cashFusionEnabled

        fun setEnabled(enabled: Boolean) {
            _cashFusionEnabled.value = enabled
        }

        fun setStatus(status: String) {
            _status.value = status
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {
        val statusRunnable = Runnable {
            var statusString = ""

            val fusionClient = WalletManager.fusionClient
            if(fusionClient != null) {
                val fusionStatus = fusionClient.fusionStatus
                if(!fusionClient.socket.isConnected) {
                    statusString += "Not connected to Fusion socket..."
                    WalletManager.setUpdateUtxosForFusion(Random().nextInt(8)+1)
                } else if(fusionStatus == FusionStatus.FAILED) {
                    statusString += "Fusion failed. Restarting..."
                    WalletManager.setUpdateUtxosForFusion(Random().nextInt(8)+1)
                } else {
                    val poolStatuses = fusionClient.poolStatuses
                    if (poolStatuses.isNotEmpty()) {
                        for (status in poolStatuses) {
                            val pct = (((status.players.toDouble() / status.minPlayers.toDouble()) * 100.0)).roundToLong()
                            statusString += if(pct >= 100) {
                                (status.tier.toString() + ": starting in " + status.timeUntilStart + "s")+"\n"
                            } else {
                                (status.tier.toString() + ": " + pct + "%")+"\n"
                            }
                        }
                        statusString += "\n"
                        statusString += fusionStatus
                    }
                }
            }

            _status.postValue(statusString)
        }
        val feeExec: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
        feeExec.scheduleAtFixedRate(statusRunnable, 0L, 2, TimeUnit.SECONDS)
    }
}