package xyz.pokkst.pokket.cash.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.glxn.qrgen.android.QRCode
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.util.ClipboardHelper
import xyz.pokkst.pokket.cash.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var page: Int = 0
    var receiveQr: ImageView? = null
    var receiveQrCoinIcon: ImageView? = null
    var receiveText: TextView? = null
    var swapAddressButton: ImageView? = null

    enum class AddressViewType {
        CASH,
        SLP,
        BIP47
    }

    var currentAddressViewType: AddressViewType = AddressViewType.CASH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
            page = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val sendScreen: LinearLayout = root.findViewById(R.id.send_screen)
        val receiveScreen: LinearLayout = root.findViewById(R.id.receive_screen)
        if (page == 1) {
            sendScreen.visibility = View.VISIBLE
            receiveScreen.visibility = View.GONE
        } else if (page == 2) {
            receiveQr = root.findViewById(R.id.receive_qr)
            receiveQrCoinIcon = root.findViewById(R.id.receive_qr_coin_icon)
            receiveText = root.findViewById(R.id.main_address_text)
            swapAddressButton = root.findViewById(R.id.swap_address_button)

            receiveText?.setOnClickListener {
                ClipboardHelper.copyToClipboard(
                    activity,
                    receiveText?.text.toString()
                )
            }
            receiveQr?.setOnClickListener {
                ClipboardHelper.copyToClipboard(
                    activity,
                    receiveText?.text.toString()
                )
            }
            receiveQrCoinIcon?.setOnClickListener {
                ClipboardHelper.copyToClipboard(
                    activity,
                    receiveText?.text.toString()
                )
            }
            swapAddressButton?.setOnClickListener {
                currentAddressViewType = when (currentAddressViewType) {
                    AddressViewType.CASH -> {
                        refresh(
                            WalletManager.walletKit?.currentSlpReceiveAddress().toString(),
                            R.drawable.logo_slp
                        )
                        AddressViewType.SLP
                    }
                    AddressViewType.SLP -> {
                        refresh(WalletManager.walletKit?.paymentCode, R.drawable.logo_bch_bip47)
                        AddressViewType.BIP47
                    }
                    AddressViewType.BIP47 -> {
                        refresh(
                            WalletManager.wallet?.currentReceiveAddress()?.toCash().toString(),
                            R.drawable.logo_bch
                        )
                        AddressViewType.CASH
                    }
                }
            }

            if (WalletManager.isMultisigKit) {
                swapAddressButton?.visibility = View.GONE
            }

            sendScreen.visibility = View.GONE
            receiveScreen.visibility = View.VISIBLE
        }

        WalletManager.refreshEvents.observe(viewLifecycleOwner, Observer { event ->
            if (event != null) {
                println("Refresh Event...")
                refresh()
            }
        })

        return root
    }

    private fun refresh() {
        when (currentAddressViewType) {
            AddressViewType.SLP -> {
                refresh(
                    WalletManager.walletKit?.currentSlpReceiveAddress().toString(),
                    R.drawable.logo_slp
                )
            }
            AddressViewType.BIP47 -> {
                refresh(WalletManager.walletKit?.paymentCode, R.drawable.logo_bch_bip47)
            }
            AddressViewType.CASH -> {
                refresh(
                    WalletManager.wallet?.currentReceiveAddress()?.toCash().toString(),
                    R.drawable.logo_bch
                )
            }
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
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): MainFragment {
            return MainFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}