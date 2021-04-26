package org.slavicin.jidelna.activities.main

import com.google.android.gms.ads.formats.UnifiedNativeAd
import org.slavicin.jidelna.data.CantryMenu

data class MenuRecyclerviewItem(
    val cantryMenu: CantryMenu,
    var ad: UnifiedNativeAd?
)