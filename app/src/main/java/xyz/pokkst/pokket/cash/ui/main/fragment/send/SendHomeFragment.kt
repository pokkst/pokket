package xyz.pokkst.pokket.cash.ui.main.fragment.send

import android.app.Activity
import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_send_home.view.*
import org.bitcoinj.protocols.fusion.models.FusionStatus
import org.bitcoinj.protocols.fusion.models.PoolStatus
import xyz.pokkst.pokket.cash.R
import xyz.pokkst.pokket.cash.qr.QRHelper
import xyz.pokkst.pokket.cash.service.YourService
import xyz.pokkst.pokket.cash.ui.main.MainFragmentDirections
import xyz.pokkst.pokket.cash.util.*
import xyz.pokkst.pokket.cash.wallet.WalletManager
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * A placeholder fragment containing a simple view.
 */
class SendHomeFragment : Fragment() {
    private var receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (Constants.ACTION_HOP_TO_BCH == intent.action) {
                    findNavController().navigate(MainFragmentDirections.navToSend(Constants.HOPCASH_SBCH_INCOMING))
                } else if (Constants.ACTION_HOP_TO_SBCH == intent.action) {
                    findNavController().navigate(MainFragmentDirections.navToSend(Constants.HOPCASH_BCH_INCOMING))
                }
            } catch (e: Exception) {

            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_send_home, container, false)

        root.scan_qr_code_button.setOnClickListener {
            QRHelper().startQRScan(this, Constants.REQUEST_CODE_SCAN_QR)
        }

        root.paste_address_button.setOnClickListener {
            val clipBoard =
                requireActivity().getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val pasteData = clipBoard.primaryClip?.getItemAt(0)?.text.toString()
            if (isValidPaymentType(pasteData) || PayloadHelper.isMultisigPayload(pasteData)) {
                findNavController().navigate(
                    MainFragmentDirections.navToSend(
                        pasteData
                    )
                )
            }
        }

        root.donate_button.setOnClickListener {
            findNavController().navigate(
                MainFragmentDirections.navToSend(
                    Constants.DONATION_ADDRESS
                )
            )
        }

        root.fusion_status_imageview.setOnClickListener {
            activity?.runOnUiThread {
                val dialogInflater = layoutInflater
                val dialoglayout: View = dialogInflater.inflate(R.layout.dialog_fusions, null)
                val builder = context?.let { it1 -> AlertDialog.Builder(it1) }
                builder?.setView(dialoglayout)
                val dialog = builder?.show()

                YourService.status.observe(viewLifecycleOwner, {
                    dialog?.findViewById<TextView>(R.id.fusion_status_textview)?.text = it
                })
                dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
        }

        YourService.cashFusionEnabled.observe(viewLifecycleOwner, { enabled ->
            root.fusion_status_imageview.visibility = if(enabled == true) View.VISIBLE else View.INVISIBLE
        })

        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_HOP_TO_BCH)
        filter.addAction(Constants.ACTION_HOP_TO_SBCH)
        activity?.let { LocalBroadcastManager.getInstance(it).registerReceiver(receiver, filter) }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.REQUEST_CODE_SCAN_QR) {
                if (data != null) {
                    val scanData = data.getStringExtra(Constants.QR_SCAN_RESULT)
                    if (scanData != null) {
                        if (isValidPaymentType(scanData) || PayloadHelper.isMultisigPayload(scanData)) {
                            findNavController().navigate(
                                MainFragmentDirections.navToSend(
                                    scanData
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isValidPaymentType(address: String): Boolean {
        return if (WalletManager.isMultisigKit) {
            UriHelper.parse(address)?.paymentType == PaymentType.ADDRESS
        } else {
            UriHelper.parse(address) != null
        }
    }
}