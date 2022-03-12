package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.system.Os.close
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.io.IOException
import java.time.Duration
import java.util.*


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val TAG = SelectLocationFragment::class.java.simpleName

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null
    private var cameraPosition: CameraPosition? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        binding.selectLocationButton.setOnClickListener {
            onLocationSelected()
        }
        binding.locationSettings.setOnClickListener {
            goToSettings()
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        if (::map.isInitialized){
            Log.d(TAG, "onStart checkPermission")
            checkLocationPermission()
        }
    }


    /**
     * Saves the state of the map when the activity is paused.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    private fun onLocationSelected() {
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        val homeLatLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, ZOOM))

        setMapStyle(map)
        checkLocationPermission()
        onSetPoi(map)
        setMapLongClick(map)
        setMapClick(map)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.maps_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun onSetPoi(map: GoogleMap) {
        map.setOnPoiClickListener { pointOfInterest ->
            map.clear()

            val marker = map.addMarker(
                MarkerOptions()
                    .title(pointOfInterest.name)
                    .position(pointOfInterest.latLng)
            )
            marker?.showInfoWindow()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pointOfInterest.latLng,
                    ZOOM
                )
            )
            setSelectedLocation(pointOfInterest.latLng, pointOfInterest.name, pointOfInterest)
        }
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            map.clear()
            val geocoder = Geocoder(context, Locale.getDefault())
            var address: String = getString(R.string.unknown_address)
            try {
                val addresses: List<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (addresses.isNotEmpty()){
                    address = addresses[0].getAddressLine(0)
                }
            } catch (ex: IOException){
                Log.d(TAG, "Error during getting address")
            }
            // A Snippet is Additional text that's displayed below the title.
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(address)
                    .snippet(snippet)

            )?.showInfoWindow()
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLng,
                    ZOOM
                )
            )
            setSelectedLocation(latLng, address)
        }
    }

    private fun setMapClick(map: GoogleMap) {
        map.setOnMapClickListener { _ ->
            map.clear()
            setSelectedLocation(null, "")
        }
    }

    private fun loadLocation(){
        binding.locationSettings.visibility = if (locationPermissionApproved()) View.GONE else View.VISIBLE
        // Turn on the My Location layer and the related control on the map.
        if (_viewModel.latitude.value != null && _viewModel.longitude.value != null){
            getSelectedLocation()
        } else {
            // Get the current location of the device and set the position of the map.
            getDeviceLocation()
        }
    }

    private fun setSelectedLocation(latLng: LatLng?, name: String, poi: PointOfInterest? = null) {
        _viewModel.latitude.value = latLng?.latitude
        _viewModel.longitude.value = latLng?.longitude
        _viewModel.selectedPOI.value = poi
        _viewModel.reminderSelectedLocationStr.value = name
    }

    @SuppressLint("MissingPermission")
    private fun getSelectedLocation() {
        try {
            setSelectedLocation(
                LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!),
                _viewModel.reminderSelectedLocationStr.value ?: "",
                _viewModel.selectedPOI.value
            )
            map.moveCamera(CameraUpdateFactory
                .newLatLngZoom(LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!), ZOOM))

            map.addMarker(
                MarkerOptions()
                    .title(_viewModel.reminderSelectedLocationStr.value )
                    .position(LatLng(_viewModel.latitude.value!!, _viewModel.longitude.value!!))
            )?.showInfoWindow()
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionApproved()) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), ZOOM
                            ))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(LatLng(latitude, longitude), ZOOM))
                        map.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkLocationPermission() {
        if (locationPermissionApproved() && ::map.isInitialized) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermissions()
        }
        loadLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.e(TAG, "onRequestPermissionsResult")
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkLocationPermission()
            } else {
                showPermissionMessage()
            }
        }
    }

    private fun showPermissionMessage() {
        val snakbar = Snackbar.make(
            binding.mapConstraint,
            R.string.click_go_to_setting,
            Snackbar.LENGTH_INDEFINITE
        )
        snakbar.setAction(R.string.dismiss) {
            snakbar.dismiss()
        }.show()
    }

    private fun goToSettings(){
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    @TargetApi(29)
    private fun locationPermissionApproved(): Boolean {
        return  (
                PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    this.requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
        )
    }

    @TargetApi(29)
    private fun requestLocationPermissions() {
        // Else request the permission
        // this provides the result[LOCATION_PERMISSION_INDEX]
        val permissionsArray = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            REQUEST_LOCATION_PERMISSION
        )
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        //Maps default values
        private val latitude = 37.33486823649444
        private val longitude = -122.00893468243808
        private const val ZOOM = 16f
        private val overlaySize = 100f
        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}
