package com.smartbudget

import android.app.Application
import com.smartbudget.data.SmartBudgetDatabase

class SmartBudgetApp : Application() {
    val database: SmartBudgetDatabase by lazy {
        SmartBudgetDatabase.getDatabase(this)
    }
}
