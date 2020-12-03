package xyz.pokkst.pokket

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.TransactionOutPoint
import org.bitcoinj.core.bip47.BIP47PaymentCode
import org.bitcoinj.params.MainNetParams
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import xyz.pokkst.pokket.wallet.WalletManager
import java.io.File
import java.security.Provider
import java.security.Security

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