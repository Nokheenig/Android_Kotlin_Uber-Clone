package com.example.uberclone.ui.ui.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.uberclone.R
import com.example.uberclone.databinding.FragmentHomeBinding
import com.example.uberclone.utils.Constants
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment(), OnMapReadyCallback {
    private val className = "HomeFragment"
    private val LOCATION_PERMISSION_REQUEST_CODE = 123487
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mMap: GoogleMap

    private lateinit var mapFragment: SupportMapFragment

    private lateinit var locationCallback: LocationCallback

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Online system
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driversLocationReference: DatabaseReference
    private lateinit var geoFire: GeoFire

    private val onlineValueEventListener = object: ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Constants.log(className = className, methodName = "onlineValueEvtListener-onDataChange", message = "User disconnected. Removing currentUser from database...")
            currentUserRef.onDisconnect().removeValue()
        }

        override fun onCancelled(error: DatabaseError) {
            Constants.log(className = className, methodName = "onlineValueEvtListener-onCancelled", message = "Error: ${error.message}")
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        init()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return root
    }

    @SuppressLint("MissingPermission")
    private fun init() {
        val funcName: String = "init"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")

        Constants.log(className = className, methodName = "$funcName", message = "Checking if app needs to ask runtime permissions for location")
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Constants.log(className = className, methodName = "$funcName", message = "Location permission not yet granted. Aborting init() and calling requestPermission ...")
            requestPermission()
            return
        } else {
            Constants.log(className = className, methodName = "$funcName", message = "Location permission already granted. Proceeding with initialization...")
        }

        onlineRef = FirebaseDatabase.getInstance(Constants.DATABASE_URL).reference.child(".info/connected")
        driversLocationReference = FirebaseDatabase.getInstance(Constants.DATABASE_URL).getReference(Constants.RIDERS_LOCATION_REFERENCE)
        currentUserRef = FirebaseDatabase.getInstance(Constants.DATABASE_URL).getReference(Constants.RIDERS_LOCATION_REFERENCE).child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        geoFire = GeoFire(driversLocationReference)

        Constants.log(className = className, methodName = "$funcName", message = "Calling registerOnlineSystem...")
        registerOnlineSystem()

        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Constants.log(className = className, methodName = "$funcName", message = "locationCallback - onLocationResult triggered")
                super.onLocationResult(locationResult)

                val newPos = LatLng(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )
                Constants.log(className = className, methodName = "$funcName", message = "locationCallback - moving camera to new position... Position: ${newPos}")
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 10f))

                geoFire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(
                        locationResult.lastLocation?.latitude!!,
                        locationResult.lastLocation?.longitude!!
                        )
                ) {key: String?, error: DatabaseError? ->
                    if (error != null){
                        Constants.log(className = className, methodName = "$funcName", message = "locationCallback - geoFire.setLocation error: ${error.message}")
                        Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
                    } else {
                        Constants.log(className = className, methodName = "$funcName", message = "locationCallback - geoFire.setLocation success - You are online !")
                        Snackbar.make(mapFragment.requireView(), "You are online !", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }

        Constants.log(className = className, methodName = "$funcName", message = "Getting fusedLocationProviderClient ...")
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
        Constants.log(className = className, methodName = "$funcName", message = "fusedLocationProviderClient - requestingLocationUpdates and passing locationUpdatesCallback...")
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        ).addOnSuccessListener {
            Constants.log(className = className, methodName = "$funcName", message = "fusedLocationProviderClient - Successfully subscribed for locationUpdates")
        }.addOnFailureListener {
            Constants.log(className = className, methodName = "$funcName", message = "fusedLocationProviderClient - Error while subscribing for locationUpdates: ${it.message}")
        }
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }

    private fun registerOnlineSystem(){
        val funcName: String = "registerOnlineSystem"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")
        onlineRef.addValueEventListener(onlineValueEventListener)
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }

    override fun onDestroy() {
        val funcName: String = "onDestroy"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val funcName: String = "onMapReady"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")

        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.uber_maps_style))
            if (!success) {
                //Log.d("Google Map", "error")
                Constants.log(className = className, methodName = "$funcName", message = "Error while loading Google Maps Styles")
            } else {
                Constants.log(className = className, methodName = "$funcName", message = "Successfully loaded Google Maps Styles")
            }
        } catch (e: Resources.NotFoundException) {
            Constants.log(className = className, methodName = "$funcName", message = "Error while loading Google Maps Styles: resource not found ")
            e.printStackTrace()
        }
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))

        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }

    private  fun requestPermission() {
        val funcName: String = "requestPermission"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)){
            // This function checks if we should show the user why we need this permission,
            // and this happens if the user denies the permissions before and tries to access again a functionnality requiring it
            //Log.d("dBug", "test if shouldShowRequestPermissionRationale case")
            Constants.log(className = className, methodName = "$funcName", message = "User previously declined location permissions. Displaying permission rationale dialog...")
            AlertDialog.Builder(requireContext())
                .setPositiveButton(R.string.dialog_button_yes) { _,_ ->
                    ActivityCompat.requestPermissions(requireActivity(),
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        ),
                        LOCATION_PERMISSION_REQUEST_CODE)
                }.setNegativeButton(R.string.dialog_button_no){
                        dialog, _ ->
                    dialog.cancel()
                }.setTitle("Permission needed")
                .setMessage("This permission is needed for accessing the device location.")
                .show()
        } else {
            Constants.log(className = className, methodName = "$funcName", message = "User hasn't been asked for location permissions yet. Asking for permissions...")
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE)
        }
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val funcName: String = "onRequestPermissionResult"
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - Start")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.size > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Constants.log(className = className, methodName = "$funcName", message = "User gave his permissions for device location. Resuming HomeFragment initialization, Calling init...")
            init()
        } else {
            Constants.log(className = className, methodName = "$funcName", message = "User declined permissions for device location. Cannot locate the user.")
            Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_LONG).show()

        }
        Constants.log(className = className, methodName = "$funcName", message = "$funcName - End")
    }
}