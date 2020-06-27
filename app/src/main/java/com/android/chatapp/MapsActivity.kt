package com.android.chatapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var latLng: LatLng
    lateinit var addressLine: String
    private var lastKnownLocation: Location? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10
    var map: GoogleMap? = null
    private val TAG = MapsActivity::class.java.simpleName
    private val DEFAULT_ZOOM = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_maps)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        editText.setOnEditorActionListener(object : TextView.OnEditorActionListener {
            override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                   searchLocation()
                }
                return true
            }
        })

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        updateLocationUI()
        getDeviceLocation()

        map?.setOnMapClickListener { point ->
            latLng = point
            map?.clear()
            var addressList: List<Address>? = null
            val geocoder = Geocoder(this)
            try {
                addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if(!addressList.isNullOrEmpty()) {
                val address: Address = addressList[0]
                addressLine = address.getAddressLine(0)
                val marker = map?.addMarker(MarkerOptions().position(latLng).title(addressLine))
                marker?.showInfoWindow()
            }
        }

        map?.setOnInfoWindowClickListener {
            val intent = Intent()
            intent.putExtra("lat", latLng.latitude)
            intent.putExtra("long", latLng.longitude)
            intent.putExtra("address", addressLine)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        map?.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            // Return null here, so that getInfoContents() is called next.
            override fun getInfoWindow(arg0: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                // Inflate the layouts for the info window, title and snippet.
                val infoWindow = layoutInflater.inflate(R.layout.custom_info_contents,
                    findViewById<FrameLayout>(R.id.map), false)
                val title = infoWindow.findViewById<TextView>(R.id.title)
                title.text = marker.title
                return infoWindow
            }
        })

    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
            updateLocationUI()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionGranted = true
                    updateLocationUI()
                    getDeviceLocation()
                }
            }
        }
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient!!.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            latLng = LatLng(lastKnownLocation!!.latitude,lastKnownLocation!!.longitude)
                            var addressList: List<Address>? = null
                            val geocoder = Geocoder(this)
                            try {
                                addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1)
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                            val address: Address = addressList!![0]
                            addressLine = address.getAddressLine(0)
                            val marker = map?.addMarker(MarkerOptions().position(latLng).title(addressLine))
                            marker?.showInfoWindow()
                            map?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    fun searchLocation() {
        val location = editText.text.toString().trim()
        var addressList: List<Address>? = null
        if (location != null || location != "") {
            val geocoder = Geocoder(this)
            try {
                addressList = geocoder.getFromLocationName(location, 1)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            if(addressList!!.isNotEmpty()) {
                val address: Address = addressList[0]
                latLng = LatLng(address.getLatitude(), address.getLongitude())
                addressLine = address.getAddressLine(0)
                map?.clear()
                val marker = map?.addMarker(MarkerOptions().position(latLng).title(addressLine))
                marker?.showInfoWindow()
                map?.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            }
            else{
                Toast.makeText(this,"Location not found",Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun clearText(view: View) {
        editText.setText("")
    }
}