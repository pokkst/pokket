package xyz.pokkst.pokket.cash

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.io.File

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                Thread.sleep(1500)
            } catch (ie: InterruptedException) {
                // fail silently
            }

            val newUser = !File(
                applicationInfo.dataDir,
                "${WalletManager.walletFileName}.wallet"
            ).exists() && !File(
                applicationInfo.dataDir,
                "${WalletManager.multisigWalletFileName}.wallet"
            ).exists()
            val intent = if (newUser) {
                Intent(baseContext, NewUserActivity::class.java)
            } else {
                Intent(baseContext, MainActivity::class.java)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}