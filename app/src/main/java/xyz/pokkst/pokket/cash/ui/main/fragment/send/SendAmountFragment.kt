package xyz.pokkst.pokket.cash.ui.main.fragment.send

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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.luminiasoft.ethereum.blockiesandroid.BlockiesIdenticon
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.component_input_numpad.view.*
import kotlinx.android.synthetic.main.fragment_send_amount.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.glxn.qrgen.android.QRCode
import org.bitcoinj.core.*
import org.bitcoinj.core.bip47.BIP47Channel
import org.bitcoinj.core.flipstarter.FlipstarterInvoicePayload
import org.bitcoinj.protocols.payments.PaymentProtocol
import org.bitcoinj.protocols.payments.PaymentProtocolException
import org.bitcoinj.protocols.payments.slp.SlpPaymentProtocol
import org.bitcoinj.script.ScriptPattern
import org.bitcoinj.utils.MultisigPayload
import org.bitcoinj.wallet.SendRequest
import org.bitcoinj.wallet.Wallet
import org.bouncycastle.util.encoders.Hex
import xyz.pokkst.pokket.cash.MainActivity
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.*
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.ExecutionException


/**
 * A placeholder fragment containing a simple view.
 */
class SendAmountFragment : Fragment() {
    var tokenId: String? = null
    var root: View? = null
    var paymentContent: PaymentContent? = null
    var bip70Type: BIP70Type? = null

    val sendingNft: MutableLiveData<Boolean> = MutableLiveData(false)

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
                } else {
                    showToast("enter an amount")
                }
            } else if (Constants.ACTION_FRAGMENT_SEND_MAX == intent.action) {
                val balance = WalletManager.kit?.wallet()?.getBalance(Wallet.BalanceType.ESTIMATED)?.toPlainString()
                val coinBalance = Coin.parseCoin(balance)
                setCoinAmount(coinBalance)
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
                root?.to_field_text?.text = "to: ${
                    payloadAddress?.replace(
                            "${WalletManager.parameters.cashAddrPrefix}:",
                            ""
                    )
                }"
                this.getPayloadData(tx)
            }
        } else if (paymentContent?.paymentType == PaymentType.FLIPSTARTER_PAYLOAD) {
            root?.send_amount_input?.isEnabled = false
            val payload =
                    paymentContent?.addressOrPayload?.let { PayloadHelper.decodeFlipstarterPayload(it) }
            if (payload != null) {
                root?.to_field_text?.text = "to: flipstarter"
                this.getFlipstarterInvoiceData(payload)
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

        setSlpView()
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

        val decimalListener = View.OnClickListener { v ->
            if (root?.send_amount_input?.isEnabled == true) {
                val view = v as Button

                if (tokenId != null) {
                    val slpToken = WalletManager.walletKit?.getSlpToken(tokenId)
                            ?: WalletManager.walletKit?.getNft(tokenId)
                    if (slpToken?.decimals != 0) {
                        appendCharacterToInput(root, view.text.toString())
                        updateAltCurrencyDisplay(root)
                    }
                } else {
                    if (paymentContent?.paymentType != PaymentType.SLP_ADDRESS) {
                        appendCharacterToInput(root, view.text.toString())
                        updateAltCurrencyDisplay(root)
                    }
                }
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
        root?.decimal_button?.setOnClickListener(decimalListener)
        root?.delete_button?.setOnClickListener {
            if (root?.send_amount_input?.isEnabled == true) {
                val newValue = root?.send_amount_input?.text.toString().dropLast(1)
                root?.send_amount_input?.setText(newValue)
                updateAltCurrencyDisplay(root)
            }
        }

        sendingNft.observe(viewLifecycleOwner, Observer {
            if (tokenId != null) {
                setInputViewForSlp(it)
            }
        })

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_MAIN_ENABLE_PAGER)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_SEND)
        filter.addAction(Constants.ACTION_FRAGMENT_SEND_MAX)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)
    }

    private fun setSlpView() {
        if (tokenId != null || paymentContent?.paymentType == PaymentType.SLP_ADDRESS) {
            val nft = WalletManager.walletKit?.getNft(tokenId)
            sendingNft.value = nft != null
        }
    }

    private fun setSlpBip70View() {
        if (tokenId != null) {
            root?.alt_currency_symbol?.visibility = View.GONE
            root?.alt_currency_display?.visibility = View.GONE
            root?.input_type_toggle?.visibility = View.GONE
            root?.main_currency_symbol?.text = WalletManager.walletKit?.getSlpToken(tokenId)?.ticker
                    ?: WalletManager.walletKit?.getNft(tokenId)?.ticker
        }
    }

    private fun setInputViewForSlp(isSendingNft: Boolean) {
        val tokenLayout = root?.findViewById<LinearLayout>(R.id.selected_token_layout)
        val slpImage = tokenLayout?.findViewById<BlockiesIdenticon>(R.id.slpImage)
        val slpIcon = tokenLayout?.findViewById<ImageView>(R.id.slpWithIcon)
        val text1 = tokenLayout?.findViewById<TextView>(R.id.text1)
        val text2 = tokenLayout?.findViewById<TextView>(R.id.text2)
        val text3 = tokenLayout?.findViewById<TextView>(R.id.text3)
        val blockiesAddress = blockieAddressFromTokenId(
                tokenId
                        ?: error("")
        )
        root?.selected_token_layout?.visibility = View.VISIBLE
        root?.alt_currency_symbol?.visibility = View.GONE
        root?.alt_currency_display?.visibility = View.GONE
        root?.input_type_toggle?.visibility = View.GONE
        root?.main_currency_symbol?.visibility = View.GONE
        if (isSendingNft) {
            root?.send_amount_input?.isEnabled = false
            root?.send_amount_input?.setText("1")
            root?.send_amount_input?.visibility = View.GONE

            val nft = WalletManager.walletKit?.getNft(tokenId)

            try {
                if (nft != null) {
                    val nftParentId = nft.nftParentId
                    if (nftParentId == NFTConstants.NFT_PARENT_ID_WAIFU) {
                        Picasso.get().load("https://icons.waifufaucet.com/64/${nft.tokenId}.png").into(slpIcon)
                        slpIcon?.visibility = View.VISIBLE
                        slpImage?.visibility = View.GONE
                    } else {
                        slpImage?.setAddress(blockiesAddress)
                        slpImage?.setCornerRadius(128f)
                    }
                }
            } catch (e: Exception) {
                slpImage?.setAddress(blockiesAddress)
                slpImage?.setCornerRadius(128f)
            }

            text1?.text = nft?.name
            text3?.text = nft?.ticker + " (NFT)"
            text2?.text = nft?.tokenId
        } else {
            root?.send_amount_input?.isEnabled = true
            root?.send_amount_input?.text = null
            root?.send_amount_input?.visibility = View.VISIBLE

            val slpToken = WalletManager.walletKit?.getSlpToken(tokenId)

            try {
                if (slpToken != null) {
                    val picasso = context?.let {
                        Picasso.Builder(it)
                                .listener { picasso, uri, exception ->
                                    slpImage?.setAddress(blockiesAddress)
                                    slpImage?.setCornerRadius(128f)
                                    slpImage?.visibility = View.VISIBLE
                                    slpIcon?.visibility = View.GONE
                                }
                                .build()
                    }
                    picasso?.load("https://tokens.bch.sx/64/${slpToken.tokenId}.png")?.into(slpIcon)
                    slpImage?.visibility = View.GONE
                    slpIcon?.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                slpImage?.setAddress(blockiesAddress)
                slpImage?.setCornerRadius(128f)
            }

            val balance =
                    WalletManager.walletKit?.getTokenBalance(tokenId)?.balance
            text1?.text = String.format(
                    Locale.ENGLISH, "%.${
                slpToken?.decimals
                        ?: 0
            }f", balance
            )
            text3?.text = slpToken?.ticker
            text2?.text = slpToken?.tokenId
        }
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
                        PaymentType.FLIPSTARTER_PAYLOAD -> {
                            val sendReq = SendRequest.createFlipstarterPledge(WalletManager.wallet, paymentContent?.addressOrPayload)
                            val peers = WalletManager.kit?.peerGroup()?.connectedPeers
                            if (peers != null) {
                                for (peer in peers) {
                                    val tx = sendReq.left
                                    peer.sendMessage(tx)
                                }
                            }

                            val pledgePayload = sendReq.right
                            showFlipstarterPledge(pledgePayload)
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
                    } else if (paymentContent?.paymentType == PaymentType.BIP70) {
                        destination?.let { this.processBIP70(it) }
                    } else {
                        showToast("invalid slp address")
                    }
                }
            } else {
                showToast("please enter an address")
            }
        } else {
            showToast("wallet balance is zero")
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
                    val needsMoreSigs = WalletManager.multisigWalletKit?.signMultisigInputs(myTx)

                    if (needsMoreSigs == true) {
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
        val cosignerTx = WalletManager.multisigWalletKit?.importMultisigPayload(multisigPayload.hex)
        val needsMoreSigs = WalletManager.multisigWalletKit?.signMultisigInputs(cosignerTx)

        if (needsMoreSigs == true) {
            multisigPayload.hex = Hex.toHexString(cosignerTx?.bitcoinSerialize())
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

    private fun showFlipstarterPledge(base64Payload: String) {
        val dialog = activity?.let { Dialog(it) }
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.setContentView(R.layout.dialog_share_pledge)
        val payloadQr = dialog?.findViewById<ImageView>(R.id.payload_qr)
        val sharePayloadButton = dialog?.findViewById<Button>(R.id.share_payload_button)
        val closeButton = dialog?.findViewById<TextView>(R.id.payload_close)
        payloadQr?.setImageBitmap(generateQR(base64Payload))
        payloadQr?.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, base64Payload)
        }
        sharePayloadButton?.setOnClickListener {
            ClipboardHelper.copyToClipboard(activity, base64Payload)
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
            lifecycleScope.launch(Dispatchers.IO) {
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
        }
    }

    private fun processBIP70(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val type = bip70Type
                if (type != null) {
                    if (type == BIP70Type.BCH) {
                        processBchBIP70(url)
                    } else if (type == BIP70Type.SLP) {
                        processSlpBIP70(url)
                    }
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
            } catch (e: Exception) {
                e.printStackTrace()
                e.message?.let {
                    showToast(it)
                }
            }
        }
    }

    private fun processBchBIP70(url: String) {
        val session = BIP70Helper.getBchPaymentSession(url)
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
                            activity?.runOnUiThread {
                                (activity as? MainActivity)?.toggleSendScreen(false)
                            }
                        }

                        override fun onFailure(throwable: Throwable) {
                            showToast("an error occurred")
                        }
                    },
                    MoreExecutors.directExecutor()
            )
        }
    }

    private fun processSlpBIP70(url: String) {
        val session = BIP70Helper.getSlpPaymentSession(url)
        if (session.isExpired) {
            showToast("invoice is expired")
            return
        }

        val tokenId = session.tokenId
        val slpToken = WalletManager.walletKit?.getSlpToken(tokenId)
        if (slpToken != null) {
            val rawTokens = session.rawTokenAmounts
            val addresses = session.getSlpAddresses(WalletManager.parameters)
            val tx = WalletManager.walletKit?.createSlpTransactionBip70(
                    tokenId,
                    null,
                    rawTokens,
                    addresses,
                    session
            )
            val ack = session.sendPayment(
                    ImmutableList.of(tx!!),
                    WalletManager.wallet?.freshReceiveAddress(),
                    null
            )
            if (ack != null) {
                Futures.addCallback<SlpPaymentProtocol.Ack>(
                        ack,
                        object : FutureCallback<SlpPaymentProtocol.Ack> {
                            override fun onSuccess(ack: SlpPaymentProtocol.Ack?) {
                                showToast("coins sent!")
                                activity?.runOnUiThread {
                                    (activity as? MainActivity)?.toggleSendScreen(false)
                                }
                            }

                            override fun onFailure(throwable: Throwable) {
                                showToast("an error occurred")
                            }
                        },
                        MoreExecutors.directExecutor()
                )
            }
        } else {
            showToast("unknown token")
        }
    }

    private fun processSlpTransaction(address: String, tokenAmount: Double, tokenId: String) {
        val tx = if (sendingNft.value == true) {
            WalletManager.walletKit?.createNftChildSendTx(address, tokenId, tokenAmount, null)
        } else {
            WalletManager.walletKit?.createSlpTransaction(address, tokenId, tokenAmount, null)
        }
        val req = SendRequest.forTx(tx)
        val sendResult = WalletManager.walletKit?.peerGroup()?.broadcastTransaction(req.tx)

        Futures.addCallback(
                sendResult?.future(),
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
        lifecycleScope.launch(Dispatchers.IO) {
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
    }

    fun getBIP70Data(root: View?, url: String?) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val type = BIP70Helper.getPaymentSessionType(url)
                bip70Type = type
                if (type == BIP70Type.BCH) {
                    val session = BIP70Helper.getBchPaymentSession(url)
                    val amountWanted = session.value
                    setCoinAmount(amountWanted)
                    activity?.runOnUiThread {
                        root?.send_amount_input?.isEnabled = false
                        root?.to_field_text?.text = session.memo
                    }
                } else if (type == BIP70Type.SLP) {
                    val session = BIP70Helper.getSlpPaymentSession(url)
                    val amountWanted = session.totalTokenAmount
                    val slpToken = WalletManager.walletKit?.getSlpToken(session.tokenId)
                    if (slpToken != null) {
                        setTokenAmount(
                                BigDecimal.valueOf(amountWanted)
                                        .scaleByPowerOfTen(-slpToken.decimals)
                        )

                        tokenId = session.tokenId
                        activity?.runOnUiThread {
                            setSlpBip70View()
                            root?.send_amount_input?.isEnabled = false
                            root?.to_field_text?.text = session.memo
                        }
                    } else {
                        showToast("unknown token")
                        activity?.runOnUiThread {
                            (activity as? MainActivity)?.toggleSendScreen(false)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getFlipstarterInvoiceData(payload: FlipstarterInvoicePayload) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val amount = payload.donation.amount
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
    }

    fun getPayloadData(tx: Transaction) {
        lifecycleScope.launch(Dispatchers.IO) {
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

    private fun setTokenAmount(amount: BigDecimal) {
        activity?.runOnUiThread {
            root?.send_amount_input?.setText(amount.toDouble().toString())
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            (activity as? MainActivity)?.enablePayButton()
        }
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
        } catch (e: Exception) {
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

    private fun blockieAddressFromTokenId(tokenId: String): String {
        return tokenId.slice(IntRange(12, tokenId.count() - 1))
    }
}