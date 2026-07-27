package com.callguard.app

object RingingSimTracker {
    @Volatile
    var lastRingingSlot: Int = -1

    @Volatile
    var lastRingingTime: Long = 0L

    fun markRinging(slot: Int) {
        lastRingingSlot = slot
        lastRingingTime = System.currentTimeMillis()
    }

    // Only trust this value if it was set very recently (within 5 seconds)
    fun getRecentSlotOrUnknown(): Int {
        return if (System.currentTimeMillis() - lastRingingTime < 5000) lastRingingSlot else -1
    }
}
