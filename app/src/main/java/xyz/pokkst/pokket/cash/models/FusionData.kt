package xyz.pokkst.pokket.cash.models

import xyz.pokkst.pokket.cash.livedata.Event

data class FusionData(val status: String, val enabled: Boolean, val utxoCount: Int, val event: Event<String>)