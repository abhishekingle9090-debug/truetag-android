package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.TaxLocation
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

sealed interface GpsLocationState {
    object Idle : GpsLocationState
    object PermissionRequired : GpsLocationState
    object GpsDisabled : GpsLocationState
    object Locating : GpsLocationState
    object Unavailable : GpsLocationState
    data class NonUsCountry(val countryName: String, val latitude: Double, val longitude: Double) : GpsLocationState
    data class Success(val taxLocation: TaxLocation, val latitude: Double, val longitude: Double) : GpsLocationState
}

/**
 * Real device GPS provider using Google Play Services FusedLocationProviderClient.
 * Strictly avoids hardcoded fallback cities (NO Chicago, NO Illinois, NO 60601).
 *
 * Includes an explicit, visible Test Location Mode for demoing outside the US.
 */
object LocationHelper {

    private const val TAG = "TrueTagGPS"
    private const val PREFS_LOC = "truetag_location_cache"
    private const val KEY_MANUAL_OVERRIDE_ZIP = "manual_override_zip"
    private const val KEY_TEST_MODE_ENABLED = "test_location_mode_enabled"
    private const val KEY_TEST_LOCATION_ZIP = "test_location_zip"
    private const val KEY_KEEP_FOR_NEXT_LAUNCH = "test_location_keep_next_launch"

    // Reactive StateFlow for Test Location Mode
    private val _testLocationState = MutableStateFlow<TaxLocation?>(null)
    val testLocationFlow: StateFlow<TaxLocation?> = _testLocationState.asStateFlow()

    val testModeRevision = MutableStateFlow(0L)

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        val keep = prefs.getBoolean(KEY_KEEP_FOR_NEXT_LAUNCH, false)
        if (!keep) {
            // The override never persists across app restarts unless the user explicitly taps "Keep for next launch"
            prefs.edit()
                .putBoolean(KEY_TEST_MODE_ENABLED, false)
                .remove(KEY_TEST_LOCATION_ZIP)
                .apply()
            _testLocationState.value = null
        } else {
            val enabled = prefs.getBoolean(KEY_TEST_MODE_ENABLED, false)
            val zip = prefs.getString(KEY_TEST_LOCATION_ZIP, null)
            if (enabled && zip != null) {
                _testLocationState.value = TaxRepository.findByZip(zip)
            } else {
                _testLocationState.value = null
            }
        }
    }

    fun isTestLocationActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_TEST_MODE_ENABLED, false) && getTestLocation(context) != null
    }

    fun getTestLocation(context: Context): TaxLocation? {
        val currentInMemory = _testLocationState.value
        if (currentInMemory != null) return currentInMemory

        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_TEST_MODE_ENABLED, false)) return null
        val zip = prefs.getString(KEY_TEST_LOCATION_ZIP, null) ?: return null
        val loc = TaxRepository.findByZip(zip)
        _testLocationState.value = loc
        return loc
    }

    fun setTestLocation(context: Context, location: TaxLocation?, keepForNextLaunch: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        if (location == null) {
            prefs.edit()
                .putBoolean(KEY_TEST_MODE_ENABLED, false)
                .remove(KEY_TEST_LOCATION_ZIP)
                .apply()
            _testLocationState.value = null
        } else {
            prefs.edit()
                .putBoolean(KEY_TEST_MODE_ENABLED, true)
                .putString(KEY_TEST_LOCATION_ZIP, location.zip)
                .putBoolean(KEY_KEEP_FOR_NEXT_LAUNCH, keepForNextLaunch)
                .apply()
            _testLocationState.value = location
        }
        testModeRevision.value = System.currentTimeMillis()
    }

    fun disableTestLocation(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_TEST_MODE_ENABLED, false)
            .remove(KEY_TEST_LOCATION_ZIP)
            .apply()
        _testLocationState.value = null
        testModeRevision.value = System.currentTimeMillis()
    }

    fun isKeepForNextLaunch(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_FOR_NEXT_LAUNCH, false)
    }

    fun setKeepForNextLaunch(context: Context, keep: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_FOR_NEXT_LAUNCH, keep).apply()
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsProviderEnabled(context: Context): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    fun getManualOverride(context: Context): TaxLocation? {
        return getTestLocation(context)
    }

    fun setManualOverride(context: Context, location: TaxLocation?) {
        setTestLocation(context, location, isKeepForNextLaunch(context))
    }

    fun getCachedLocation(context: Context): TaxLocation? {
        val testLoc = getTestLocation(context)
        if (testLoc != null) return testLoc

        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        val zip = prefs.getString("cached_zip", null)
        val city = prefs.getString("cached_city", null)
        val state = prefs.getString("cached_state", null)
        if (!state.isNullOrBlank() && !zip.isNullOrBlank()) {
            return TaxRepository.resolveTaxForUsLocation(
                city = city ?: "",
                state = state,
                county = prefs.getString("cached_county", "") ?: "",
                zip = zip,
                street = prefs.getString("cached_street", "") ?: ""
            )
        }
        return null
    }

    private fun cacheLocation(context: Context, loc: TaxLocation) {
        val prefs = context.getSharedPreferences(PREFS_LOC, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cached_zip", loc.zip)
            .putString("cached_city", loc.city)
            .putString("cached_state", loc.state)
            .putString("cached_county", loc.county)
            .putString("cached_street", loc.streetAddress)
            .apply()
    }

    /**
     * Subscribes to real device location updates as a reactive Flow<Location>.
     */
    @SuppressLint("MissingPermission")
    fun getLocationFlow(context: Context): Flow<Location> = callbackFlow {
        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Location permission not granted for getLocationFlow")
            close()
            return@callbackFlow
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
            if (lastLoc != null) {
                Log.d(TAG, "Real GPS initial lastLocation: lat=${lastLoc.latitude}, lng=${lastLoc.longitude}")
                trySend(lastLoc)
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 6000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                Log.d(TAG, "Real GPS update received: lat=${loc.latitude}, lng=${loc.longitude}")
                trySend(loc)
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            Log.d(TAG, "Unsubscribing from real GPS updates")
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Subscribes to full reactive GpsLocationState updates.
     * Evaluates permission, GPS toggle, reverse geocoding, and US country boundary.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(context: Context, refreshSignal: Long = 0L): Flow<GpsLocationState> = callbackFlow {
        val override = getManualOverride(context)
        if (override != null) {
            Log.d(TAG, "Manual US override active: ${override.city}, ${override.state}")
            trySend(GpsLocationState.Success(override, 0.0, 0.0))
            awaitClose { }
            return@callbackFlow
        }

        if (!hasLocationPermission(context)) {
            Log.d(TAG, "Real GPS: Location permission required")
            trySend(GpsLocationState.PermissionRequired)
            awaitClose { }
            return@callbackFlow
        }

        if (!isGpsProviderEnabled(context)) {
            Log.d(TAG, "Real GPS: Location provider disabled")
            trySend(GpsLocationState.GpsDisabled)
            awaitClose { }
            return@callbackFlow
        }

        trySend(GpsLocationState.Locating)

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

        // Try last known location first for instantaneous resolution
        fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
            if (lastLoc != null) {
                Log.d(TAG, "Real GPS lastLocation: lat=${lastLoc.latitude}, lng=${lastLoc.longitude}")
                processCoordinates(context, lastLoc.latitude, lastLoc.longitude) { state ->
                    trySend(state)
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 6000L)
            .setMinUpdateIntervalMillis(3000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                Log.d(TAG, "Real GPS continuous update: lat=${loc.latitude}, lng=${loc.longitude}")
                processCoordinates(context, loc.latitude, loc.longitude) { state ->
                    trySend(state)
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            Log.d(TAG, "Closing GPS location callback")
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * One-shot real GPS lookup.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOnce(context: Context): GpsLocationState = suspendCancellableCoroutine { cont ->
        val override = getManualOverride(context)
        if (override != null) {
            cont.resume(GpsLocationState.Success(override, 0.0, 0.0))
            return@suspendCancellableCoroutine
        }

        if (!hasLocationPermission(context)) {
            cont.resume(GpsLocationState.PermissionRequired)
            return@suspendCancellableCoroutine
        }

        if (!isGpsProviderEnabled(context)) {
            cont.resume(GpsLocationState.GpsDisabled)
            return@suspendCancellableCoroutine
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()

        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Acquired Real Lat: ${location.latitude}, Lng: ${location.longitude}")
                    processCoordinates(context, location.latitude, location.longitude) { state ->
                        cont.resume(state)
                    }
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            processCoordinates(context, lastLoc.latitude, lastLoc.longitude) { state ->
                                cont.resume(state)
                            }
                        } else {
                            cont.resume(GpsLocationState.Unavailable)
                        }
                    }.addOnFailureListener {
                        cont.resume(GpsLocationState.Unavailable)
                    }
                }
            }
            .addOnFailureListener {
                cont.resume(GpsLocationState.Unavailable)
            }
    }

    private fun processCoordinates(
        context: Context,
        latitude: Double,
        longitude: Double,
        onResult: (GpsLocationState) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val addr = addresses.firstOrNull()
                        val state = evaluateAddress(context, addr, latitude, longitude)
                        onResult(state)
                    }

                    override fun onError(errorMessage: String?) {
                        Log.e(TAG, "Geocoder error: $errorMessage")
                        onResult(GpsLocationState.Unavailable)
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val state = evaluateAddress(context, addresses?.firstOrNull(), latitude, longitude)
                onResult(state)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during geocoding real GPS coordinates", e)
            onResult(GpsLocationState.Unavailable)
        }
    }

    private fun evaluateAddress(
        context: Context,
        address: Address?,
        latitude: Double,
        longitude: Double
    ): GpsLocationState {
        if (address == null) {
            Log.w(TAG, "No reverse-geocoded address found for Lat=$latitude, Lng=$longitude")
            return GpsLocationState.Unavailable
        }

        val country = address.countryName ?: ""
        val countryCode = address.countryCode ?: ""
        val state = address.adminArea ?: ""
        val city = address.locality ?: address.subAdminArea ?: ""
        val postal = address.postalCode ?: ""

        Log.d(TAG, "Real GPS reverse geocoded: Lat=$latitude, Lng=$longitude, Country='$country' ($countryCode), State='$state', City='$city', ZIP='$postal'")

        // CRITICAL CHECK: Must be United States
        val isUs = country.equals("United States", ignoreCase = true) ||
                country.equals("United States of America", ignoreCase = true) ||
                country.equals("USA", ignoreCase = true) ||
                country.equals("US", ignoreCase = true) ||
                countryCode.equals("US", ignoreCase = true) ||
                countryCode.equals("USA", ignoreCase = true)

        if (!isUs) {
            val displayCountry = country.ifBlank { countryCode }.ifBlank { "International" }
            Log.w(TAG, "Location is outside United States: $displayCountry")
            return GpsLocationState.NonUsCountry(displayCountry, latitude, longitude)
        }

        // Real US location
        val street = address.thoroughfare?.let { thorough ->
            val num = address.subThoroughfare ?: ""
            if (num.isNotBlank()) "$num $thorough" else thorough
        } ?: address.getAddressLine(0)?.split(",")?.firstOrNull() ?: ""

        val county = address.subAdminArea ?: "County"

        val taxLocation = TaxRepository.resolveTaxForUsLocation(
            city = city.ifBlank { "Local Store" },
            state = state.ifBlank { "US" },
            county = county,
            zip = postal,
            street = street
        )

        cacheLocation(context, taxLocation)
        Log.d(TAG, "Resolved Real US Tax Location: ${taxLocation.city}, ${taxLocation.state} ${taxLocation.zip} (${taxLocation.ratePercentageFormatted} tax)")
        return GpsLocationState.Success(taxLocation, latitude, longitude)
    }
}
