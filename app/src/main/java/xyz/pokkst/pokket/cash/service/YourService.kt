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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bitcoinj.protocols.fusion.models.PoolStatus
import xyz.pokkst.pokket.cash.wallet.WalletManager
import kotlin.math.roundToLong


class YourService : Service() {
    private val NOTIF_ID = 1294912
    private val NOTIF_CHANNEL_ID = "FUSION_CHANNELID"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForeground() {

    }
}