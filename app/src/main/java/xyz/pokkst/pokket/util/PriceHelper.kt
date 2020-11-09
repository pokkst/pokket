package xyz.pokkst.pokket.util

class PriceHelper {
    companion object {
        private var lastChecked = 0L
        private var cachedPrice = 0.0
        val price: Double
            get() {
                val currentTime = System.currentTimeMillis() / 1000L
                if (cachedPrice == 0.0 || currentTime - lastChecked >= 300L) {
                    lastChecked = currentTime
                    cachedPrice =
                        readPriceFromUrl("https://api.cryptowat.ch/markets/coinbase-pro/bchusd/price")
                }
                return cachedPrice
            }

        private fun readPriceFromUrl(url: String): Double {
            return try {
                val json = JSONHelper().getJsonObject(url)
                json?.getJSONObject("result")?.getDouble("price") ?: 0.0
            } catch (e: Exception) {
                0.0
            }
        }

    }
}