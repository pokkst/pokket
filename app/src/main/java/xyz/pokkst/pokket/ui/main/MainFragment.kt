package xyz.pokkst.pokket.ui.main

import android.app.Dialog
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.zxing.WriterException
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import net.glxn.qrgen.android.QRCode
import xyz.pokkst.pokket.R
import xyz.pokkst.pokket.util.Constants
import xyz.pokkst.pokket.util.Toaster
import xyz.pokkst.pokket.wallet.WalletManager


/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment() {

    private lateinit var pageViewModel: PageViewModel
    private var page: Int = 0
    var receiveQr: ImageView? = null
    var receiveText: TextView? = null
    var swapAddressButton: ImageView? = null
    enum class AddressViewType {
        CASH,
        SLP,
        BIP47
    }
    var currentAddressViewType: AddressViewType = AddressViewType.CASH

    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Constants.ACTION_UPDATE_REFRESH == intent.action) {
                when(currentAddressViewType) {
                    AddressViewType.SLP -> { refresh(WalletManager.walletKit?.currentSlpReceiveAddress().toString(), true) }
                    AddressViewType.BIP47 -> { refresh(WalletManager.walletKit?.paymentCode, false) }
                    AddressViewType.CASH -> { refresh(WalletManager.wallet?.currentReceiveAddress()?.toCash().toString(), false) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
            page = arguments?.getInt(ARG_SECTION_NUMBER) ?: 1
        }

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_UPDATE_REFRESH)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter)

    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_main, container, false)
        val sendScreen: LinearLayout = root.findViewById(R.id.send_screen)
        val receiveScreen: LinearLayout = root.findViewById(R.id.receive_screen)
        if(page == 1) {
            sendScreen.visibility = View.VISIBLE
            receiveScreen.visibility = View.GONE
        } else if(page == 2) {
            receiveQr = root.findViewById(R.id.receive_qr)
            receiveText = root.findViewById(R.id.main_address_text)
            swapAddressButton = root.findViewById(R.id.swap_address_button)

            receiveText?.setOnClickListener {
                copyToClipboard(receiveText?.text.toString())
            }
            receiveQr?.setOnClickListener {
                copyToClipboard(receiveText?.text.toString())
            }
            swapAddressButton?.setOnClickListener {
                currentAddressViewType = when(currentAddressViewType) {
                    AddressViewType.CASH -> {
                        refresh(WalletManager.walletKit?.currentSlpReceiveAddress().toString(), true)
                        AddressViewType.SLP
                    }
                    AddressViewType.SLP -> {
                        refresh(WalletManager.walletKit?.paymentCode, false)
                        AddressViewType.BIP47
                    }
                    AddressViewType.BIP47 -> {
                        refresh(WalletManager.wallet?.currentReceiveAddress()?.toCash().toString(), false)
                        AddressViewType.CASH
                    }
                }
            }

            if(WalletManager.isMultisigKit) {
                swapAddressButton?.visibility = View.GONE
            }

            sendScreen.visibility = View.GONE
            receiveScreen.visibility = View.VISIBLE
        }

        return root
    }

    private fun copyToClipboard(text: String) {
        val clipboard: ClipboardManager? = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        val clip = ClipData.newPlainText("Address", text)
        clipboard?.setPrimaryClip(clip)
        Toaster.showToastMessage(requireContext(), "copied")
    }

    private fun refresh(address: String?, slp: Boolean) {
        this.generateQR(address, slp)
    }

    private fun generateQR(address: String?, slp: Boolean) {

        try {
            val encoder = QRCode.from(address).withSize(1024, 1024).withErrorCorrection(ErrorCorrectionLevel.H)

            val qrCode = encoder.bitmap()
            val coinLogo: Bitmap? = if (!slp)
                drawableToBitmap(this.resources.getDrawable(R.drawable.logo_bch))
            else
                drawableToBitmap(this.resources.getDrawable(R.drawable.logo_slp))

            val merge = overlayBitmapToCenter(qrCode, coinLogo!!)
            receiveQr?.setImageBitmap(merge)
            receiveText?.text = address?.replace("${WalletManager.parameters.cashAddrPrefix}:", "")?.replace("${WalletManager.parameters.simpleledgerPrefix}:", "")
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    /*
    I'm absolutely terrible with Bitmap and image generation shit. Always have been.
    Shout-out to StackOverflow for some of this.
    */
    private fun overlayBitmapToCenter(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val bitmap1Width = bitmap1.width
        val bitmap1Height = bitmap1.height
        val bitmap2Width = bitmap2.width
        val bitmap2Height = bitmap2.height

        val marginLeft = (bitmap1Width * 0.5 - bitmap2Width * 0.5).toFloat()
        val marginTop = (bitmap1Height * 0.5 - bitmap2Height * 0.5).toFloat()

        val overlayBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.config)
        val canvas = Canvas(overlayBitmap)
        canvas.drawBitmap(bitmap1, Matrix(), null)
        canvas.drawBitmap(bitmap2, marginLeft, marginTop, null)
        return overlayBitmap
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val bitmap: Bitmap? = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) // Single color bitmap will be created of 1x1 pixel
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }

        if (drawable is BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        val canvas = Canvas(bitmap!!)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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