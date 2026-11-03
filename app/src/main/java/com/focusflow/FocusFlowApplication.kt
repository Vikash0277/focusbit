package com.focusflow

import android.app.Application
import com.focusflow.data.db.FocusFlowDatabase

class FocusFlowApplication : Application() {
    val database: FocusFlowDatabase by lazy {
        FocusFlowDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        com.focusflow.notifications.NotificationHelper.createNotificationChannels(this)
        com.focusflow.coins.CoinLedgerService.init(database)
        com.focusflow.subscription.SubscriptionManager.init(this)
        com.focusflow.scroll.ScrollTrackerManager.init(this)
    }
}
