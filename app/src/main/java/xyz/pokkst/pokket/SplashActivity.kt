package xyz.pokkst.pokket

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import xyz.pokkst.pokket.wallet.WalletManager
import java.io.File

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)
        object : Thread() {
            override fun run() {
                Looper.prepare()
                try {
                    sleep(1500)
                } catch (ie: InterruptedException) {
                    // fail silently
                }

                val newUser = !File(applicationInfo.dataDir, "${WalletManager.walletFileName}.wallet").exists() && !File(applicationInfo.dataDir, "${WalletManager.multisigWalletFileName}.wallet").exists()
                val intent = if(newUser) {
                    Intent(baseContext, NewUserActivity::class.java)
                } else {
                    Intent(baseContext, MainActivity::class.java)
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
                Looper.loop()
            }
        }.start()

    }
}