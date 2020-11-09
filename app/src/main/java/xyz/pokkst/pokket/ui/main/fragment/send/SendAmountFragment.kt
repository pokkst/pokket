package xyz.pokkst.pokket.ui.main.fragment.send

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.component_input_numpad.view.*
import kotlinx.android.synthetic.main.fragment_send_amount.view.*
import kotlinx.android.synthetic.main.token_spinner_cell.view.*
import net.glxn.qrgen.android.QRCode
import org.bitcoinj.core.*
import org.bitcoinj.core.bip47.BIP47Channel
import org.bitcoinj.core.slp.SlpTokenBalance
import org.bitcoinj.crypto.TransactionSignature
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.protocols.payments.PaymentProtocolException
import org.bitcoinj.protocols.payments.PaymentSession
import org.bitcoinj.script.Script
import org.bitcoinj.script.ScriptPattern
import org.bitcoinj.utils.MultisigPayload
import org.bitcoinj.wallet.RedeemData
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bouncycastle.util.encoders.Hex
import xyz.pokkst.pokket.MainActivity
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.*
import xyz.pokkst.pokket.wallet.WalletManager
import java.util.concurrent.ExecutionException


/**
 * A placeholder fragment containing a simple view.
 */
class SendAmountFragment : Fragment() {
    var tokenId: String? = null
    var root: View? = null
    var paymentContent: PaymentContent? = null
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_MAIN_ENABLE_PAGER == intent.action) {
                this@SendAmountFragment.findNavController()
                    .popBackStack(R.id.sendHomeFragment, false)
            } else if (Constants.ACTION_FRAGMENT_SEND_SEND == intent.action) {
                if (getCoinAmount() != Coin.ZERO) {
                    if (WalletManager.isMultisigKit) {
                        if (paymentContent?.paymentType == PaymentType.MULTISIG_PAYLOAD) {
                            paymentContent?.addressOrPayload?.let {
                                this@SendAmountFragment.importMultisigPayload(
                                    it
                                )
                            }
                        } else {
                            this@SendAmountFragment.sendMultisig()
                        }
                    } else {
                        this@SendAmountFragment.send()
                    }
                }
            }
        }
    }

    var bchIsSendType = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (activity as? MainActivity)?.toggleSendScreen(false)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        root = inflater.inflate(R.layout.fragment_send_amount, container, false)
        prepareViews()
        setListeners()
        return root
    }

    private fun prepareViews() {
        (activity as? MainActivity)?.toggleSendScreen(true)

        root?.input_type_toggle?.isChecked = true

        tokenId = arguments?.getString("tokenId", null)
        paymentContent = arguments?.getString("address", null)?.let { UriHelper.parse(it) }
        if (paymentContent?.paymentType == PaymentType.MULTISIG_PAYLOAD) {
            root?.send_amount_input?.isEnabled = false
            val payload =
                paymentContent?.addressOrPayload?.let { PayloadHelper.decodeMultisigPayload(it) }
            if (payload != null) {
                val tx = Transaction(WalletManager.parameters, Hex.decode(payload.hex))
                val payloadAddress = fetchMultisigPayloadDestination(tx)
                root?.to_field_text?.text = "to: ${payloadAddress?.replace("bitcoincash:", "")}"
                this.getPayloadData(tx)
            }
        } else {
            if (paymentContent != null) {
                when (paymentContent?.paymentType) {
                    PaymentType.BIP70 -> this.getBIP70Data(root, paymentContent?.addressOrPayload)
                    else -> root?.to_field_text?.text = "to: ${paymentContent?.addressOrPayload}"
                }

                if (paymentContent?.amount != null) {
                    root?.send_amount_input?.isEnabled = false
                    setCoinAmount(paymentContent?.amount)
                }
            } else {
                root?.to_field_text?.visibility = View.GONE
                root?.to_field_edit_text?.visibility = View.VISIBLE
            }
        }

        if (tokenId != null || paymentContent?.paymentType == PaymentType.SLP_ADDRESS) {
            val items = WalletManager.walletKit?.slpBalances?.toList() ?: listOf()
            val adapter = object : ArrayAdapter<SlpTokenBalance>(
                requireContext(),
                R.layout.token_spinner_cell,
                items
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = LayoutInflater.from(requireContext())
                        .inflate(R.layout.token_spinner_cell, null)
                    val slpToken = WalletManager.walletKit?.getSlpToken(
                        WalletManager.walletKit?.slpBalances?.get(position)?.tokenId
                    )
                    if (slpToken != null) {
                        view.token_ticker.text = slpToken.ticker
                    }
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    val view = LayoutInflater.from(requireContext())
                        .inflate(R.layout.token_spinner_cell, null)
                    val slpToken = WalletManager.walletKit?.getSlpToken(
                        WalletManager.walletKit?.slpBalances?.get(position)?.tokenId
                    )
                    if (slpToken != null) {
                        view.token_ticker.text = slpToken.ticker
                    }
                    return view
                }
            }
            root?.token_selector_todo?.adapter = adapter
            root?.token_selector_todo?.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        v: View?,
                        position: Int,
                        id: Long
                    ) {
                        tokenId = WalletManager.walletKit?.slpBalances?.get(position)?.tokenId
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            if (tokenId != null) {
                for (x in items.indices) {
                    if (items[x].tokenId == tokenId) {
                        root?.token_selector_todo?.setSelection(x)
                    }
                }
            }
            root?.token_selector_todo?.visibility = View.VISIBLE
            root?.alt_currency_symbol?.visibility = View.GONE
            root?.alt_currency_display?.visibility = View.GONE
            root?.input_type_toggle?.visibility = View.GONE
            root?.main_currency_symbol?.visibility = View.GONE
        }
    }

    private fun setListeners() {
        root?.input_type_toggle?.setOnClickListener {
            if (PriceHelper.price != 0.0) {
                bchIsSendType = !bchIsSendType
                swapSendTypes(root)
            }
        }

        val charInputListener = View.OnClickListener { v ->
            if (root?.send_amount_input?.isEnabled == true) {
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
            if (root?.send_amount_input?.isEnabled == true) {
                val newValue = root?.send_amount_input?.text.toString().dropLast(1)
                root?.send_amount_input?.setText(newValue)
                updateAltCurrencyDisplay(root)
            }
        }

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_SEND)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    private fun send() {
        if (WalletManager.wallet?.getBalance(Wallet.BalanceType.ESTIMATED)?.isZero == false) {
            if (paymentContent != null || root?.to_field_edit_text?.text?.isNotEmpty() == true) {
                if (paymentContent == null) paymentContent =
                    root?.to_field_edit_text?.text?.toString()?.let { UriHelper.parse(it) }
                val destination = paymentContent?.addressOrPayload
                if (tokenId == null) {
                    when (paymentContent?.paymentType) {
                        PaymentType.BIP70 -> destination?.let { this.processBIP70(it) }
                        PaymentType.CASH_ACCOUNT, PaymentType.ADDRESS -> this.processNormalTransaction()
                        PaymentType.PAYMENT_CODE -> {
                            val canSendToPaymentCode =
                                WalletManager.walletKit?.canSendToPaymentCode(destination)
                            if (canSendToPaymentCode == true) {
                                this.attemptBip47Payment()
                            } else {
                                val notification =
                                    WalletManager.walletKit?.makeNotificationTransaction(
                                        destination,
                                        true
                                    )
                                WalletManager.walletKit?.broadcastTransaction(notification?.tx)
                                WalletManager.walletKit?.putPaymenCodeStatusSent(
                                    destination,
                                    notification?.tx
                                )
                                this.attemptBip47Payment()
                            }
                        }
                        PaymentType.SLP_ADDRESS -> showToast("please choose slp token")
                        PaymentType.MULTISIG_PAYLOAD -> showToast("send is in incorrect state")
                        null -> showToast("please enter a valid destination")
                    }
                } else {
                    if (paymentContent?.paymentType == PaymentType.SLP_ADDRESS) {
                        val amount = root?.send_amount_input?.text?.toString()?.toDouble() ?: 0.0
                        val slpTokenId = tokenId
                        if (destination != null && slpTokenId != null) {
                            this.processSlpTransaction(destination, amount, slpTokenId)
                        }
                    }
                }
            } else {
                (activity as? MainActivity)?.let {
                    Toaster.showMessage(
                        it,
                        "please enter an address"
                    )
                }
            }
        } else {
            (activity as? MainActivity)?.let { Toaster.showMessage(it, "wallet balance is zero") }
        }
    }

    private fun sendMultisig() {
        if (WalletManager.wallet?.getBalance(Wallet.BalanceType.ESTIMATED)?.isZero == false) {
            if (paymentContent != null) {
                if (paymentContent?.paymentType == PaymentType.ADDRESS) {
                    val toAddress = AddressFactory.create()
                        .getAddress(WalletManager.parameters, paymentContent?.addressOrPayload)
                    val myTx = WalletManager.multisigWalletKit?.makeIndividualMultisigTransaction(
                        toAddress,
                        getCoinAmount()
                    )
                    var needsMoreSigs = false

                    myTx?.inputs?.forEach { input ->
                        val bitcoinRedeemData: RedeemData? =
                            input.getConnectedRedeemData(WalletManager.wallet)
                        if (bitcoinRedeemData != null) {
                            val utxo = input.connectedOutput
                            val script: Script? = utxo?.scriptPubKey
                            val redeemScriptProgram = bitcoinRedeemData.redeemScript.program
                            val sigHash: Sha256Hash = myTx.hashForSignatureWitness(
                                input.index,
                                bitcoinRedeemData.redeemScript,
                                input.connectedOutput!!.value,
                                Transaction.SigHash.ALL,
                                false
                            )
                            var inputScript = input.scriptSig

                            val mySignature: TransactionSignature =
                                myTx.calculateWitnessSignature(
                                    input.index,
                                    bitcoinRedeemData.fullKey,
                                    redeemScriptProgram,
                                    input.connectedOutput!!.value,
                                    Transaction.SigHash.ALL,
                                    false
                                )
                            val mySignatureIndex = input.scriptSig.getSigInsertionIndex(
                                sigHash,
                                bitcoinRedeemData.fullKey
                            )

                            inputScript = script?.getScriptSigWithSignature(
                                inputScript,
                                mySignature.encodeToBitcoin(),
                                mySignatureIndex
                            )
                            input.scriptSig = inputScript

                            needsMoreSigs = needsMoreSigs(input, utxo)
                        }
                    }

                    if (needsMoreSigs) {
                        val payload = MultisigPayload()
                        payload.hex = Hex.toHexString(myTx?.bitcoinSerialize())
                        val json: String = Gson().toJson(payload)
                        showPayload(json)
                    } else {
                        val peers = WalletManager.multisigWalletKit?.peerGroup()?.connectedPeers
                        if (peers != null) {
                            var broadcasted = false
                            for (peer in peers) {
                                peer.sendMessage(myTx)

                                if (!broadcasted) {
                                    showToast("coins sent!")
                                    (activity as? MainActivity)?.toggleSendScreen(false)
                                    broadcasted = true
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun importMultisigPayload(base64Payload: String) {
        val multisigPayload = PayloadHelper.decodeMultisigPayload(base64Payload) ?: return
        var needsMoreSigs = false

        val cosignerTx = WalletManager.multisigWalletKit?.importMultisigPayload(multisigPayload.hex)
        cosignerTx?.inputs?.forEach { input ->
            val bitcoinRedeemData =
                input.getConnectedRedeemData(WalletManager.wallet)
            if (bitcoinRedeemData != null) {
                val utxo = input.connectedOutput
                val script: Script? = utxo?.scriptPubKey
                val redeemScriptProgram = bitcoinRedeemData.redeemScript.program
                val sigHash: Sha256Hash = cosignerTx.hashForSignatureWitness(
                    input.index,
                    bitcoinRedeemData.redeemScript,
                    input.connectedOutput!!.value,
                    Transaction.SigHash.ALL,
                    false
                )
                var inputScript = input.scriptSig
                val cosignerSignature: TransactionSignature = cosignerTx.calculateWitnessSignature(
                    input.index,
                    bitcoinRedeemData.fullKey,
                    redeemScriptProgram,
                    input.connectedOutput!!.value,
                    Transaction.SigHash.ALL,
                    false
                )
                val cosignerSignatureIndex =
                    input.scriptSig.getSigInsertionIndex(sigHash, bitcoinRedeemData.fullKey)
                inputScript = script?.getScriptSigWithSignature(
                    inputScript,
                    cosignerSignature.encodeToBitcoin(),
                    cosignerSignatureIndex
                )
                input.scriptSig = inputScript
                needsMoreSigs = needsMoreSigs(input, utxo)

                if (needsMoreSigs) {
                    multisigPayload.hex = Hex.toHexString(cosignerTx.bitcoinSerialize())
                }
            }
        }

        if (needsMoreSigs) {
            val newPayloadJson: String = Gson().toJson(multisigPayload)
            showPayload(newPayloadJson)
        } else {
            val peers = WalletManager.multisigWalletKit?.peerGroup()?.connectedPeers
            if (peers != null) {
                var broadcasted = false
                for (peer in peers) {
                    peer.sendMessage(cosignerTx)

                    if (!broadcasted) {
                        showToast("coins sent!")
                        (activity as? MainActivity)?.toggleSendScreen(false)
                        broadcasted = true
                    }
                }
            }
        }
    }

    private fun showPayload(payloadJson: String) {
        val payloadBase64 = PayloadHelper.encodeMultisigPayload(payloadJson)
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.dialog_payload)
        val payloadQr = dialog?.findViewById<ImageView>(R.id.payload_qr)
        val sharePayloadButton = dialog?.findViewById<Button>(R.id.share_payload_button)
        val closeButton = dialog?.findViewById<TextView>(R.id.payload_close)
        payloadQr?.setImageBitmap(generateQR(payloadBase64))
        payloadQr?.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, payloadBase64)
        }
        sharePayloadButton?.setOnClickListener {
            sharePayload(payloadBase64)
        }
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }
        dialog?.show()
    }

    private fun attemptBip47Payment() {
        val destination = paymentContent?.addressOrPayload
        val paymentChannel: BIP47Channel? =
            WalletManager.walletKit?.getBip47MetaForPaymentCode(destination)
        var depositAddress: String? = null
        if (paymentChannel != null) {
            if (paymentChannel.isNotificationTransactionSent) {
                depositAddress = WalletManager.walletKit?.getCurrentOutgoingAddress(paymentChannel)
                if (depositAddress != null) {
                    println("Received user's deposit address $depositAddress")
                    paymentChannel.incrementOutgoingIndex()
                    WalletManager.walletKit?.saveBip47MetaData()
                    this.processNormalTransaction(depositAddress)
                }
            } else {
                val notification =
                    WalletManager.walletKit?.makeNotificationTransaction(destination, true)
                WalletManager.walletKit?.broadcastTransaction(notification?.tx)
                WalletManager.walletKit?.putPaymenCodeStatusSent(destination, notification?.tx)
                this.attemptBip47Payment()
            }
        }
    }

    private fun swapSendTypes(root: View?) {
        if (root != null) {
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
        if (root != null) {
            if (char == "." && !root.send_amount_input.text.toString().contains(".")) {
                root.send_amount_input.append(char)
            } else if (char != ".") {
                root.send_amount_input.append(char)
            }
        }
    }

    private fun updateAltCurrencyDisplay(root: View?) {
        if (root != null) {
            object : Thread() {
                override fun run() {
                    if (root.send_amount_input.text.toString().isNotEmpty()) {
                        val value = if (root.send_amount_input.text.toString() == ".")
                            0.0
                        else
                            java.lang.Double.parseDouble(root.send_amount_input.text.toString())

                        val price = PriceHelper.price

                        activity?.runOnUiThread {
                            root.alt_currency_display.text = if (bchIsSendType) {
                                val fiatValue = value * price
                                BalanceFormatter.formatBalance(fiatValue, "0.00")
                            } else {
                                val bchValue = value / price
                                BalanceFormatter.formatBalance(bchValue, "#.########")
                            }
                        }
                    } else {
                        activity?.runOnUiThread {
                            root.alt_currency_display.text = null
                        }
                    }
                }
            }.start()
        }
    }

    private fun processBIP70(url: String) {
        object : Thread() {
            override fun run() {
                try {
                    val future: ListenableFuture<PaymentSession> = PaymentSession.createFromUrl(url)

                    val session = future.get()
                    if (session.isExpired) {
                        showToast("invoice is expired")
                        return
                    }

                    val req = session.sendRequest
                    req.allowUnconfirmed()
                    WalletManager.wallet?.completeTx(req)

                    val ack = session.sendPayment(
                        ImmutableList.of(req.tx),
                        WalletManager.wallet?.freshReceiveAddress(),
                        null
                    )
                    if (ack != null) {
                        Futures.addCallback<PaymentProtocol.Ack>(
                            ack,
                            object : FutureCallback<PaymentProtocol.Ack> {
                                override fun onSuccess(ack: PaymentProtocol.Ack?) {
                                    WalletManager.wallet?.commitTx(req.tx)
                                    showToast("coins sent!")
                                    (activity as? MainActivity)?.toggleSendScreen(false)
                                }

                                override fun onFailure(throwable: Throwable) {
                                    showToast("an error occurred")
                                }
                            },
                            MoreExecutors.directExecutor()
                        )
                    }
                } catch (e: InsufficientMoneyException) {
                    e.printStackTrace()
                    showToast("not enough coins in wallet")
                } catch (e: Wallet.CouldNotAdjustDownwards) {
                    e.printStackTrace()
                    showToast("error adjusting downwards")
                } catch (e: Wallet.ExceededMaxTransactionSize) {
                    e.printStackTrace()
                    showToast("transaction is too big")
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    e.message?.let {
                        showToast(it)
                    }
                }
            }
        }.start()
    }

    private fun processSlpTransaction(address: String, tokenAmount: Double, tokenId: String) {
        val tx = WalletManager.walletKit?.createSlpTransaction(address, tokenId, tokenAmount, null)
        val req = SendRequest.forTx(tx)
        val sendResult = WalletManager.wallet?.sendCoins(req)
        Futures.addCallback(
            sendResult?.broadcastComplete,
            object : FutureCallback<Transaction?> {
                override fun onSuccess(@Nullable result: Transaction?) {
                    showToast("coins sent!")
                    (activity as? MainActivity)?.toggleSendScreen(false)
                }

                override fun onFailure(t: Throwable) { // We died trying to empty the wallet.

                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun processNormalTransaction() {
        paymentContent?.addressOrPayload?.let { this.processNormalTransaction(it) }
    }

    private fun processNormalTransaction(address: String?) {
        val bchToSend = getCoinAmount()
        address?.let { this.processNormalTransaction(it, bchToSend) }
    }

    private fun processNormalTransaction(address: String, bchAmount: Coin) {
        object : Thread() {
            override fun run() {
                try {
                    val req: SendRequest =
                        if (bchAmount == WalletManager.wallet?.let { WalletManager.getBalance(it) }) {
                            SendRequest.emptyWallet(WalletManager.parameters, address)
                        } else {
                            SendRequest.to(WalletManager.parameters, address, bchAmount)
                        }

                    req.allowUnconfirmed()
                    req.ensureMinRequiredFee = false
                    req.feePerKb = Coin.valueOf(2L * 1000L)
                    val sendResult = WalletManager.wallet?.sendCoins(req)
                    Futures.addCallback(
                        sendResult?.broadcastComplete,
                        object : FutureCallback<Transaction?> {
                            override fun onSuccess(@Nullable result: Transaction?) {
                                showToast("coins sent!")
                                (activity as? MainActivity)?.toggleSendScreen(false)
                            }

                            override fun onFailure(t: Throwable) { // We died trying to empty the wallet.

                            }
                        },
                        MoreExecutors.directExecutor()
                    )
                } catch (e: InsufficientMoneyException) {
                    e.printStackTrace()
                    showToast("not enough coins in wallet")
                } catch (e: Wallet.CouldNotAdjustDownwards) {
                    e.printStackTrace()
                    showToast("error adjusting downwards")
                } catch (e: Wallet.ExceededMaxTransactionSize) {
                    e.printStackTrace()
                    showToast("transaction is too big")
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    e.message?.let {
                        showToast(it)
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
                    setCoinAmount(amountWanted)
                    activity?.runOnUiThread {
                        root?.send_amount_input?.isEnabled = false
                        root?.to_field_text?.text = session.memo
                    }
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

    fun getPayloadData(tx: Transaction) {
        object : Thread() {
            override fun run() {
                try {
                    var amount = 0L
                    for (output in tx.outputs) {
                        if (!output.isMineOrWatched(WalletManager.wallet)) {
                            amount += output.value.value
                        }
                    }
                    val amountWanted = Coin.valueOf(amount)
                    setCoinAmount(amountWanted)
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

    private fun getCoinAmount(): Coin {
        val bchToSend = if (bchIsSendType) {
            root?.send_amount_input?.text.toString()
        } else {
            root?.alt_currency_display?.text.toString()
        }

        if (bchToSend.isEmpty())
            return Coin.ZERO

        return Coin.parseCoin(bchToSend)
    }

    private fun setCoinAmount(coin: Coin?) {
        val amountFormatted = coin?.toPlainString()?.toDouble()
            ?.let { BalanceFormatter.formatBalance(it, "#.########") }
        activity?.runOnUiThread {
            val bchValue = amountFormatted?.toDouble() ?: 0.0
            val price = PriceHelper.price
            val fiatValue = bchValue * price
            if (bchIsSendType) {
                root?.send_amount_input?.setText(
                    BalanceFormatter.formatBalance(
                        bchValue,
                        "#.########"
                    )
                )
                root?.alt_currency_display?.text = BalanceFormatter.formatBalance(fiatValue, "0.00")
            } else {
                root?.send_amount_input?.setText(BalanceFormatter.formatBalance(fiatValue, "0.00"))
                root?.alt_currency_display?.text =
                    BalanceFormatter.formatBalance(bchValue, "#.########")
            }
        }
    }

    fun needsMoreSigs(input: TransactionInput, utxo: TransactionOutput?): Boolean {
        return try {
            input.verify(utxo)
            false
        } catch (e: Exception) {
            true
        }
    }

    private fun showToast(message: String) {
        (activity as? MainActivity)?.let { Toaster.showMessage(it, message) }
    }

    private fun sharePayload(payloadJson: String?) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, payloadJson)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            shareIntent?.let { startActivity(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateQR(payload: String?): Bitmap? {
        return try {
            val encoder = QRCode.from(payload).withSize(1024, 1024).withErrorCorrection(
                ErrorCorrectionLevel.L
            )
            encoder.bitmap()
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchMultisigPayloadDestination(tx: Transaction): String? {
        for (utxo in tx.outputs) {
            if (!ScriptPattern.isOpReturn(utxo.scriptPubKey) && !utxo.isMineOrWatched(WalletManager.wallet))
                return utxo.scriptPubKey.getToAddress(WalletManager.parameters).toCash().toString()
        }

        return null
    }
}