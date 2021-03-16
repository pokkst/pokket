package xyz.pokkst.pokket.cash

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bouncycastle.jce.provider.BouncyCastleProvider
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.io.File
import java.security.Provider
import java.security.Security


class SplashActivity : AppCompatActivity() {
    private val CODE_AUTHENTICATION_VERIFICATION = 241
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBouncyCastle()
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
            if (newUser) {
                val intent = Intent(baseContext, NewUserActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            } else {
                val km =
                        getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (km.isKeyguardSecure) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val securityIntent = km.createConfirmDeviceCredentialIntent(
                                "Unlock Wallet",
                                null
                        )
                        startActivityForResult(securityIntent, CODE_AUTHENTICATION_VERIFICATION)
                    } else {
                        val intent = Intent(baseContext, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    }

                } else {
                    val intent = Intent(baseContext, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }

        }
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == CODE_AUTHENTICATION_VERIFICATION) {
            val intent = Intent(baseContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Unlock failed. Try again.", Toast.LENGTH_SHORT)
                    .show()
        }
    }

    /*
    Read the comment within the method. This is needed so the proper Bouncycastle is loaded, so we can use the Elliptic Curve Diffie-Hellman algorithm for BIP47 common secret calculation.
     */
    private fun setupBouncyCastle() {
        val provider: Provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
                ?: return
        if (provider.javaClass == BouncyCastleProvider::class.java) { // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }
}