# pokket
A rewrite of Crescent Cash, rebranded to Pokket

NOTICE (2022):
This wallet is no longer actively supported by pokkst. The CashFusion branch that you are viewing was work-in-progress/*beta* and was never fully completed.
After the May/June implosion of the cryptocurrency space, notably the CoinFlex/SmartBCH collapse, and the growing toxic community in BCH, I am exhausted.
https://github.com/JettScythe/pokket has an updated CashFusion branch with the latest CashFusion server.

Since this was never completed, there are a few things that still need to be completed if anyone is looking to continue work:
- Proof messages when round fails (this client just disconnects and assumes fault since it is an independent implemention of the CashFusion protocol).
- The Pokket send screen user interace still needs to have a "Spend only fused coins" checkbox. This can easily be added by setting SendRequest.onlyFusion (or whatever exactly it is called) to true when checked.
- General stability improvements as getting this to work reliably and over Tor is a pain on Android.
