plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("waipack")
    dynamicDelivery {
        deliveryType.set("install-time")
    }
}