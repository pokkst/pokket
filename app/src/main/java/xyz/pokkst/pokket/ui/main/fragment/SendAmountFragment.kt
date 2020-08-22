package xyz.pokkst.pokket.ui.main.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.android.synthetic.main.component_input_numpad.view.*
import kotlinx.android.synthetic.main.fragment_send_amount.view.*
import org.bitcoinj.core.Address
import org.bitcoinj.core.Coin
import org.bitcoinj.core.InsufficientMoneyException
import org.bitcoinj.core.Transaction
import org.bitcoinj.core.bip47.BIP47Channel
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.protocols.payments.PaymentProtocolException
import org.bitcoinj.protocols.payments.PaymentSession
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.PriceHelper
import xyz.pokkst.pokket.util.Toaster
import xyz.pokkst.pokket.wallet.WalletManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.ExecutionException


/**
 * A placeholder fragment containing a simple view.
 */
class SendAmountFragment : Fragment() {
    //TODO add SLP sending
    var address: String? = null
    var root: View? = null
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@SendAmountFragment.findNavController().popBackStack(R.id.sendHomeFragment, false)
            } else if (Constants.ACTION_FRAGMENT_SEND_SEND == intent.action) {
                this@SendAmountFragment.send()
            }
        }
    }

    var bchIsSendType = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        root = inflater.inflate(R.layout.fragment_send_amount, container, false)
        (requireActivity() as MainActivity).enableSendScreen()
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_SEND)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
        root?.input_type_toggle?.isChecked = true
        root?.input_type_toggle?.setOnClickListener {
            bchIsSendType = !bchIsSendType
            swapSendTypes(root)
        }

        val charInputListener = View.OnClickListener { v ->
            if(root?.send_amount_input?.isEnabled == true) {
                val view = v as Button
                appendCharacterToInput(root, view.text.toString())
                updateAltCurrencyDisplay(root)
            }
        }

        root?.input_0?.setOnClickListener(charInputListener)
        root?.input_1?.setOnClickListener(charInputListener)
        root?.input_2?.setOnClickListener(charInputListener)
        root?.input_3?.setOnClickListener(charInputListener)
        root?.input_4?.setOnClickListener(charInputListener)
        root?.input_5?.setOnClickListener(charInputListener)
        root?.input_6?.setOnClickListener(charInputListener)
        root?.input_7?.setOnClickListener(charInputListener)
        root?.input_8?.setOnClickListener(charInputListener)
        root?.input_9?.setOnClickListener(charInputListener)
        root?.decimal_button?.setOnClickListener(charInputListener)
        root?.delete_button?.setOnClickListener {
            if(root?.send_amount_input?.isEnabled == true) {
                val newValue = root?.send_amount_input?.text.toString().dropLast(1)
                root?.send_amount_input?.setText(newValue)
                updateAltCurrencyDisplay(root)
            }
        }

        address = arguments?.getString("address", "")
        if(address?.contains("http") == true) {
            this.getBIP70Data(root, address?.replace("bitcoincash:?r=", ""))
        } else {
            root?.to_field_text?.text = "to: ${address?.replace("bitcoincash:", "")}"
        }
        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun send() {
        if(WalletManager.walletKit?.wallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.isZero == false) {
            if(address?.startsWith("http") == true) {
                this.processBIP70(address!!)
            } else {
                if(address?.contains("#") == true || Address.isValidCashAddr(WalletManager.parameters, address) || Address.isValidLegacyAddress(WalletManager.parameters, address)) {
                    this.processNormalTransaction()
                } else if(Address.isValidPaymentCode(address)) {
                    val canSendToPaymentCode = WalletManager.walletKit?.canSendToPaymentCode(address)
                    if(canSendToPaymentCode == true) {
                        this.attemptBip47Payment()
                    } else {
                        val notification = WalletManager.walletKit?.makeNotificationTransaction(address, true)
                        WalletManager.walletKit?.broadcastTransaction(notification?.tx)
                        WalletManager.walletKit?.putPaymenCodeStatusSent(address, notification?.tx)
                        this.attemptBip47Payment()
                    }
                }
            }
        } else {
            Toaster.showMessage(requireActivity() as MainActivity, "wallet balance is zero")
        }
    }

    private fun attemptBip47Payment() {
        val paymentChannel: BIP47Channel? = WalletManager.walletKit?.getBip47MetaForPaymentCode(address)
        var depositAddress: String? = null
        if (paymentChannel != null) {
            if(paymentChannel.isNotificationTransactionSent) {
                depositAddress = WalletManager.walletKit?.getCurrentOutgoingAddress(paymentChannel)
                if(depositAddress != null) {
                    println("Received user's deposit address $depositAddress")
                    paymentChannel.incrementOutgoingIndex()
                    WalletManager.walletKit?.saveBip47MetaData()
                    this.processNormalTransaction(depositAddress)
                }
            } else {
                val notification = WalletManager.walletKit?.makeNotificationTransaction(address, true)
                WalletManager.walletKit?.broadcastTransaction(notification?.tx)
                WalletManager.walletKit?.putPaymenCodeStatusSent(address, notification?.tx)
                this.attemptBip47Payment()
            }
        }
    }

    private fun swapSendTypes(root: View?) {
        if(root != null) {
            if (bchIsSendType) {
                //We are changing from BCH as the alt currency.
                val bchValue = root.alt_currency_display.text.toString()
                val fiatValue = root.send_amount_input.text.toString()
                root.main_currency_symbol.text = resources.getString(R.string.b_symbol)
                root.alt_currency_symbol.text = resources.getString(R.string.fiat_symbol)
                root.send_amount_input.setText(bchValue)
                root.alt_currency_display.text = fiatValue
            } else {
                //We are changing from fiat as the alt currency.
                val bchValue = root.send_amount_input.text.toString()
                val fiatValue = root.alt_currency_display.text.toString()
                root.main_currency_symbol.text = resources.getString(R.string.fiat_symbol)
                root.alt_currency_symbol.text = resources.getString(R.string.b_symbol)
                root.alt_currency_display.text = bchValue
                root.send_amount_input.setText(fiatValue)
            }
        }
    }

    private fun appendCharacterToInput(root: View?, char: String) {
        if(root != null) {
            if (char == "." && !root.send_amount_input.text.toString().contains(".")) {
                root.send_amount_input.append(char)
            } else if (char != ".") {
                root.send_amount_input.append(char)
            }
        }
    }

    private fun updateAltCurrencyDisplay(root: View?) {
        if(root != null) {
            object : Thread() {
                override fun run() {
                    if (root.send_amount_input.text.toString().isNotEmpty()) {
                        val value = if(root.send_amount_input.text.toString() == ".")
                            0.0
                        else
                            java.lang.Double.parseDouble(root.send_amount_input.text.toString())

                        val price = PriceHelper.price

                        if (bchIsSendType) {
                            val fiatValue = value * price
                            requireActivity().runOnUiThread {
                                root.alt_currency_display.text = formatBalance(fiatValue, "0.00")
                            }
                        } else {
                            val bchValue = value / price
                            requireActivity().runOnUiThread {
                                root.alt_currency_display.text =
                                    formatBalance(bchValue, "#.########")
                            }
                        }
                    } else {
                        requireActivity().runOnUiThread {
                            root.alt_currency_display.text = null
                        }
                    }
                }
            }.start()
        }
    }

    fun formatBalance(amount: Double, pattern: String): String {
        val formatter = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return formatter.format(amount)
    }

    private fun processBIP70(url: String) {
        object : Thread() {
            override fun run() {
                try {
                    val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)

                    val session = future.get()
                    if (session.isExpired) {
                        Toaster.showMessage(requireActivity() as MainActivity, "invoice is expired")
                    }

                    val req = session.sendRequest
                    req.allowUnconfirmed()
                    WalletManager.walletKit?.wallet()?.completeTx(req)

                    val ack = session.sendPayment(ImmutableList.of(req.tx), WalletManager.walletKit?.wallet()?.freshReceiveAddress(), null)
                    if (ack != null) {
                        Futures.addCallback<PaymentProtocol.Ack>(ack, object : FutureCallback<PaymentProtocol.Ack> {
                            override fun onSuccess(ack: PaymentProtocol.Ack?) {
                                WalletManager.walletKit?.wallet()?.commitTx(req.tx)
                                Toaster.showMessage(
                                    requireActivity() as MainActivity,
                                    "coins sent!"
                                )
                                (requireActivity() as MainActivity).disableSendScreen()
                            }

                            override fun onFailure(throwable: Throwable) {
                                Toaster.showMessage(requireActivity() as MainActivity, "an error occurred")
                            }
                        }, MoreExecutors.directExecutor())
                    }
                } catch (e: InsufficientMoneyException) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "not enough coins in wallet"
                    )
                } catch (e: Wallet.CouldNotAdjustDownwards) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "error adjusting downwards"
                    )
                } catch (e: Wallet.ExceededMaxTransactionSize) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "transaction is too big"
                    )
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    e.message?.let {
                        Toaster.showMessage(
                            requireActivity() as MainActivity,
                            it
                        )
                    }
                }
            }
        }.start()
    }

    private fun processNormalTransaction() {
        address?.let { this.processNormalTransaction(it) }
    }

    private fun processNormalTransaction(address: String?) {
        val bchToSend = if (bchIsSendType) {
            root?.send_amount_input?.text.toString()
        } else {
            root?.alt_currency_display?.text.toString()
        }
        address?.let { this.processNormalTransaction(it, bchToSend) }
    }

    private fun processNormalTransaction(address: String, bchAmount: String) {
        val bchToSend = formatBalance(bchAmount.toDouble(), "#.########")
        val coinToSend = Coin.parseCoin(bchAmount)

        object : Thread() {
            override fun run() {
                try {
                    val req: SendRequest =
                        if (coinToSend == WalletManager.getBalance(WalletManager.walletKit!!.wallet())) {
                            SendRequest.emptyWallet(WalletManager.parameters, address)
                        } else {
                            SendRequest.to(WalletManager.parameters, address, coinToSend)
                        }

                    req.allowUnconfirmed()
                    req.ensureMinRequiredFee = false
                    req.feePerKb = Coin.valueOf(2L * 1000L)
                    val sendResult = WalletManager.walletKit?.wallet()?.sendCoins(req)
                    Futures.addCallback(
                        sendResult?.broadcastComplete,
                        object : FutureCallback<Transaction?> {
                            override fun onSuccess(@Nullable result: Transaction?) {
                                Toaster.showMessage(
                                    requireActivity() as MainActivity,
                                    "coins sent!"
                                )
                                (requireActivity() as MainActivity).disableSendScreen()
                            }

                            override fun onFailure(t: Throwable) { // We died trying to empty the wallet.

                            }
                        },
                        MoreExecutors.directExecutor()
                    )
                } catch (e: InsufficientMoneyException) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "not enough coins in wallet"
                    )
                } catch (e: Wallet.CouldNotAdjustDownwards) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "error adjusting downwards"
                    )
                } catch (e: Wallet.ExceededMaxTransactionSize) {
                    e.printStackTrace()
                    Toaster.showMessage(
                        requireActivity() as MainActivity,
                        "transaction is too big"
                    )
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    e.message?.let {
                        Toaster.showMessage(
                            requireActivity() as MainActivity,
                            it
                        )
                    }
                }
            }
        }.start()
    }

    fun getBIP70Data(root: View?, url: String?) {
        object : Thread() {
            override fun run() {
                try {
                    val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)
                    val session = future.get()
                    val amountWanted = session.value
                    val amountFormatted = formatBalance(amountWanted.toPlainString().toDouble(), "#.########")
                    requireActivity().runOnUiThread {
                        val bchValue = amountFormatted.toDouble()
                        val price = PriceHelper.price
                        val fiatValue = bchValue * price
                        if (bchIsSendType) {
                            root?.send_amount_input?.setText(formatBalance(bchValue, "#.########"))
                            root?.alt_currency_display?.text = formatBalance(fiatValue, "0.00")
                        } else {
                            root?.send_amount_input?.setText(formatBalance(fiatValue, "0.00"))
                            root?.alt_currency_display?.text = formatBalance(bchValue, "#.########")
                        }

                        root?.send_amount_input?.isEnabled = false
                        root?.to_field_text?.text = session.memo
                    }
                    address = url
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                } catch (e: PaymentProtocolException) {
                    e.printStackTrace()
                }

            }
        }.start()
    }
}