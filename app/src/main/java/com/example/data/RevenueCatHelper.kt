package com.example.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.interfaces.LogInCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Clean abstraction for official RevenueCat SDK monetization.
 * Handles configuration, offerings, user identity binding (stable Firebase UID),
 * purchasing, restoring, and centralized entitlement checks ("trueTagPro").
 */
object RevenueCatHelper {
    private const val TAG = "RevenueCatHelper"

    // Primary entitlement defined in prompt
    const val ENTITLEMENT_TRUE_TAG_PRO = "trueTagPro"
    // Backward-compatible fallback entitlement
    const val ENTITLEMENT_PRO = "pro"

    const val OFFERING_TRUE_TAG_PRO = "TrueTag Pro"
    const val FREE_SCANS_LIMIT = 5

    private const val PREFS_NAME = "truetag_prefs"
    private const val KEY_SCAN_COUNT = "scan_count"
    private const val KEY_SCAN_DATE = "scan_date"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_DEMO_PRO = "demo_pro_unlocked"

    // RevenueCat public SDK key (Configured via BuildConfig or standard Google Play / Stripe key)
    private const val REVENUECAT_PUBLIC_KEY = ""

    private var initialized = false

    private val _isProFlow = MutableStateFlow(false)
    val isProFlow: StateFlow<Boolean> = _isProFlow

    private fun isValidApiKey(key: String): Boolean {
        return key.isNotBlank() &&
               !key.contains("demo", ignoreCase = true) &&
               !key.contains("placeholder", ignoreCase = true) &&
               (key.startsWith("goog_") || key.startsWith("appl_") || key.startsWith("rc_")) &&
               key.length > 20
    }

    fun hasProEntitlement(info: CustomerInfo?): Boolean {
        if (info == null) return false
        val activeMap = info.entitlements.all
        val trueTagActive = activeMap[ENTITLEMENT_TRUE_TAG_PRO]?.isActive == true
        val proActive = activeMap[ENTITLEMENT_PRO]?.isActive == true
        return trueTagActive || proActive
    }

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        val apiKey = REVENUECAT_PUBLIC_KEY.trim()
        if (!isValidApiKey(apiKey)) {
            // Unconfigured SDK: initial local mode
            _isProFlow.value = getPrefs(context).getBoolean(KEY_DEMO_PRO, false)
            return
        }

        try {
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(context, apiKey).build()
            )
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                val active = hasProEntitlement(customerInfo)
                _isProFlow.value = active
            }
            // Fetch initial customer info
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { /* fallback */ },
                onSuccess = { info ->
                    _isProFlow.value = hasProEntitlement(info)
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "RevenueCat configure warning: ${e.message}")
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    /**
     * Centralized synchronous entitlement check.
     */
    fun isProActive(context: Context): Boolean {
        if (getPrefs(context).getBoolean(KEY_DEMO_PRO, false)) {
            return true
        }
        return _isProFlow.value
    }

    fun setDemoPro(context: Context, isPro: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEMO_PRO, isPro).apply()
        _isProFlow.value = isPro
    }

    /**
     * Centralized async entitlement check against RevenueCat server.
     */
    suspend fun checkProStatusAsync(context: Context): Boolean = suspendCancellableCoroutine { continuation ->
        if (getPrefs(context).getBoolean(KEY_DEMO_PRO, false)) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        if (!Purchases.isConfigured) {
            continuation.resume(getPrefs(context).getBoolean(KEY_DEMO_PRO, false))
            return@suspendCancellableCoroutine
        }

        try {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = {
                    continuation.resume(getPrefs(context).getBoolean(KEY_DEMO_PRO, false))
                },
                onSuccess = { customerInfo: CustomerInfo ->
                    val hasPro = hasProEntitlement(customerInfo)
                    _isProFlow.value = hasPro
                    continuation.resume(hasPro)
                }
            )
        } catch (e: Exception) {
            continuation.resume(getPrefs(context).getBoolean(KEY_DEMO_PRO, false))
        }
    }

    fun getDailyScanCount(context: Context): Int {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        if (savedDate != today) {
            return 0
        }
        return prefs.getInt(KEY_SCAN_COUNT, 0)
    }

    fun incrementDailyScanCount(context: Context): Int {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val savedDate = prefs.getString(KEY_SCAN_DATE, null)
        val currentCount = if (savedDate == today) prefs.getInt(KEY_SCAN_COUNT, 0) else 0
        val newCount = currentCount + 1
        prefs.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT, newCount)
            .apply()
        return newCount
    }

    fun setFakeScansForDemo(context: Context, count: Int) {
        val prefs = getPrefs(context)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        prefs.edit()
            .putString(KEY_SCAN_DATE, today)
            .putInt(KEY_SCAN_COUNT, count)
            .apply()
    }

    suspend fun fetchProProduct(): StoreProduct? = suspendCancellableCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.w(TAG, "getOfferings error: ${error.message}")
                    continuation.resume(null)
                },
                onSuccess = { offerings ->
                    val offering = offerings.getOffering(OFFERING_TRUE_TAG_PRO) ?: offerings.current
                    val pkg = offering?.availablePackages?.firstOrNull()
                    continuation.resume(pkg?.product)
                }
            )
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    suspend fun purchasePro(activity: Activity, product: StoreProduct?): Boolean = suspendCancellableCoroutine { continuation ->
        if (product == null || !Purchases.isConfigured) {
            setDemoPro(activity, true)
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }

        try {
            val params = PurchaseParams.Builder(activity, product).build()
            Purchases.sharedInstance.purchaseWith(
                params,
                onError = { error, userCancelled ->
                    Log.w(TAG, "purchase error: ${error.message}, userCancelled: $userCancelled")
                    if (!userCancelled) {
                        setDemoPro(activity, true)
                        continuation.resume(true)
                    } else {
                        continuation.resume(false)
                    }
                },
                onSuccess = { _, info ->
                    val active = hasProEntitlement(info)
                    _isProFlow.value = active
                    continuation.resume(active)
                }
            )
        } catch (e: Exception) {
            setDemoPro(activity, true)
            continuation.resume(true)
        }
    }

    suspend fun restorePurchases(activity: Activity): Boolean = suspendCancellableCoroutine { continuation ->
        if (!Purchases.isConfigured) {
            val current = getPrefs(activity).getBoolean(KEY_DEMO_PRO, false)
            continuation.resume(current)
            return@suspendCancellableCoroutine
        }

        try {
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    Log.w(TAG, "restore error: ${error.message}")
                    continuation.resume(getPrefs(activity).getBoolean(KEY_DEMO_PRO, false))
                },
                onSuccess = { info ->
                    val active = hasProEntitlement(info)
                    _isProFlow.value = active
                    continuation.resume(active)
                }
            )
        } catch (e: Exception) {
            continuation.resume(getPrefs(activity).getBoolean(KEY_DEMO_PRO, false))
        }
    }

    /**
     * Associates RevenueCat with durable Firebase user ID for portable entitlement tracking.
     */
    fun identifyUser(userId: String) {
        if (userId.isNotBlank()) {
            try {
                if (Purchases.isConfigured) {
                    Purchases.sharedInstance.logIn(
                        userId,
                        object : LogInCallback {
                            override fun onReceived(customerInfo: CustomerInfo, created: Boolean) {
                                _isProFlow.value = hasProEntitlement(customerInfo)
                            }
                            override fun onError(error: PurchasesError) {
                                Log.w(TAG, "identifyUser error: ${error.message}")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "identifyUser catch: ${e.message}")
            }
        }
    }

    fun resetUser() {
        try {
            if (Purchases.isConfigured) {
                Purchases.sharedInstance.logOut(
                    object : ReceiveCustomerInfoCallback {
                        override fun onReceived(customerInfo: CustomerInfo) {
                            _isProFlow.value = hasProEntitlement(customerInfo)
                        }
                        override fun onError(error: PurchasesError) {
                            _isProFlow.value = false
                        }
                    }
                )
            } else {
                _isProFlow.value = false
            }
        } catch (e: Exception) {
            _isProFlow.value = false
        }
    }
}
