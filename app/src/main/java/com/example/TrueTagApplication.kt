package com.example
import android.app.Application
import com.example.data.LocationHelper
import com.example.data.PriceInsightRepository
import com.example.data.RevenueCatHelper
import com.example.data.TaxRepository
import com.example.data.TrueTagRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrueTagApplication : Application() {

    lateinit var repository: TrueTagRepository
        private set

    override fun onCreate() {
        super.onCreate()
        TaxRepository.initialize(this)
        LocationHelper.initialize(this)
        PriceInsightRepository.initialize(this)
        RevenueCatHelper.initialize(this)
        com.example.data.AuthHelper.initialize(this)

        repository = TrueTagRepository(this)
        CoroutineScope(Dispatchers.IO).launch {
            repository.seedInitialDataIfEmpty()
        }
    }
}
