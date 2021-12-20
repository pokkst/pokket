package xyz.pokkst.pokket.cash.util

class Constants {
    companion object {
        const val ACTION_MAIN_ENABLE_PAGER = "ACTION_MAIN_DISABLE_PAGER"
        const val ACTION_FRAGMENT_SEND_SEND = "ACTION_FRAGMENT_SEND_SEND"
        const val ACTION_FRAGMENT_SEND_MAX = "ACTION_FRAGMENT_SEND_MAX"
        const val ACTION_HOP_TO_BCH = "ACTION_HOP_TO_BCH"
        const val ACTION_HOP_TO_SBCH = "ACTION_HOP_TO_SBCH"

        const val PREF_DERIVATION_PATH = "derivation_path"
        const val DERIVATION_PATH_DEFAULT = "m/44'/245'/0'"

        const val EXTRA_DERIVATION = "derivation"
        const val EXTRA_SEED = "seed"
        const val EXTRA_NEW = "new"
        const val EXTRA_MULTISIG = "multisig"
        const val EXTRA_FOLLOWING_KEYS = "followingKeys"
        const val EXTRA_PASSPHRASE = "passphrase"
        const val EXTRA_M = "m"

        const val REQUEST_CODE_SCAN_QR = 100

        const val QR_SCAN_RESULT = "SCAN_RESULT"

        const val DONATION_ADDRESS = "qpywztspkg2zk2lqygt2n3ftj9ftwvtsu52zz6v02e"

        const val HOPCASH_BCH_INCOMING = "qqa0dj5rwaw2s4tz88m3xmcpjyzry356gglq7zvu80"
        const val HOPCASH_SBCH_INCOMING = "0x3207d65b4D45CF617253467625AF6C1b687F720b"
    }
}