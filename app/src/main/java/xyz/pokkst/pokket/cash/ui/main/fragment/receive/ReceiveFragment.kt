package xyz.pokkst.pokket.cash.ui.main.fragment.receive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.glxn.qrgen.android.QRCode
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.interactors.WalletInteractor
import xyz.pokkst.pokket.cash.util.ClipboardHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager

/**
 * A placeholder fragment containing a simple view.
 */
class ReceiveFragment : Fragment() {
    private val walletInteractor = WalletInteractor.getInstance()

    enum class AddressViewType {
        CASH,
        SMARTBCH,
        BIP47
    }

    var currentAddressViewType: AddressViewType = AddressViewType.CASH

    var receiveQr: ImageView? = null
    var receiveQrCoinIcon: ImageView? = null
    var receiveText: TextView? = null
    var swapAddressButton: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_receive, container, false)
        receiveQr = root.findViewById(R.id.receive_qr)
        receiveQrCoinIcon = root.findViewById(R.id.receive_qr_coin_icon)
        receiveText = root.findViewById(R.id.main_address_text)
        swapAddressButton = root.findViewById(R.id.swap_address_button)

        receiveText?.setOnClickListener {
            copyToClipboard()
        }
        receiveQr?.setOnClickListener {
            copyToClipboard()
        }
        receiveQrCoinIcon?.setOnClickListener {
            copyToClipboard()
        }
        swapAddressButton?.setOnClickListener {
            currentAddressViewType = when (currentAddressViewType) {
                AddressViewType.CASH -> {
                    refresh(walletInteractor.getSmartAddress(), R.drawable.logo_sbch)
                    AddressViewType.SMARTBCH
                }
                AddressViewType.SMARTBCH -> {
                    refresh(WalletManager.walletKit?.paymentCode, R.drawable.logo_bch_bip47)
                    AddressViewType.BIP47
                }
                AddressViewType.BIP47 -> {
                    refresh(
                        walletInteractor.getBitcoinAddress()?.toString(),
                        R.drawable.logo_bch
                    )
                    AddressViewType.CASH
                }
            }
        }

        if (WalletManager.isMultisigKit) {
            swapAddressButton?.visibility = View.GONE
        }

        WalletManager.refreshEvents.observe(viewLifecycleOwner, { event ->
            if (event != null) {
                refresh()
            }
        })

        return root
    }

    private fun copyToClipboard() {
        val address = receiveText?.text.toString()
        when (currentAddressViewType) {
            AddressViewType.CASH -> ClipboardHelper.copyToClipboard(
                activity,
                "${WalletManager.parameters.cashAddrPrefix}:${address}"
            )
            AddressViewType.SMARTBCH -> ClipboardHelper.copyToClipboard(activity, address)
            AddressViewType.BIP47 -> ClipboardHelper.copyToClipboard(activity, address)
        }
    }

    private fun refresh() {
        when (currentAddressViewType) {
            AddressViewType.SMARTBCH -> refresh(
                walletInteractor.getSmartAddress(),
                R.drawable.logo_sbch
            )
            AddressViewType.BIP47 -> refresh(
                WalletManager.walletKit?.paymentCode,
                R.drawable.logo_bch_bip47
            )
            AddressViewType.CASH -> refresh(
                walletInteractor.getBitcoinAddress().toString(),
                R.drawable.logo_bch
            )
        }
    }

    private fun refresh(address: String?, resId: Int) {
        this.generateQR(address, resId)
    }

    private fun generateQR(address: String?, resId: Int) {

        try {
            val encoder = QRCode.from(address).withSize(1024, 1024)
                .withErrorCorrection(ErrorCorrectionLevel.H)
            val qrCode = encoder.bitmap()
            receiveQrCoinIcon?.setImageResource(resId)
            receiveQr?.setImageBitmap(qrCode)
            receiveText?.text = address?.replace("${WalletManager.parameters.cashAddrPrefix}:", "")
                ?.replace("${WalletManager.parameters.simpleledgerPrefix}:", "")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}