package xyz.pokkst.pokket.util

class PriceHelper {
    companion object {
        private var lastChecked = 0L
        private var cachedPrice = 0.0
        val price: Double
            get() {
                val currentTime = System.currentTimeMillis() / 1000L
                return if(cachedPrice == 0.0 || currentTime - lastChecked >= 300L) {
                    println("Fetching new price data...")
                    lastChecked = System.currentTimeMillis() / 1000L
                    cachedPrice = readPriceFromUrl("https://api.cryptowat.ch/markets/coinbase-pro/bchusd/price")
                    cachedPrice
                } else {
                    cachedPrice
                }
            }

        private fun readPriceFromUrl(url: String): Double {
            var price = 0.0

            try {
                val json = JSONHelper().getJsonObject(url)
                val priceStr = when {
                    url.contains("min-api.cryptocompare.com") -> {
                        json!!.getDouble("AUD")
                    }
                    else -> {
                        json!!.getJSONObject("result").getDouble("price")
                    }
                }

                println(priceStr)
                price = priceStr
            } catch (e: Exception) {
                e.printStackTrace()
                price = 0.0
            }

            return price
        }

    }
}