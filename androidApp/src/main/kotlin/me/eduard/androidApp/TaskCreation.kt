package me.eduard.androidApp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import me.eduard.androidApp.model.TaskModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class TaskCreation : AppCompatActivity(), OnMapReadyCallback, AdapterView.OnItemSelectedListener,
    GoogleMap.OnCameraIdleListener/*, OnMarkerDragListener*/ {

    private val REQUEST_IMAGE_CAPTURE = 22
    private val M_MAX_ENTRIES = 100
    private val GALLERY_REQUEST_CODE: Int = 0
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0
    private val marker = LatLng(50.075539, 14.437800)
    private var fileName: String = ""
    private lateinit var file_uri: Uri
    private var DEFAULT_ZOOM = 15

    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null

    private lateinit var placesClient: PlacesClient

    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var locationPermissionGranted = false

    private var lastKnownLocation: Location? = null

    private var photoFile: File? = null

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var startMarker: Marker
    private lateinit var finishMarker: Marker
    private var searchType: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_creation)

        var mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)?.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        Places.initialize(applicationContext, getString(R.string.maps_api_key))
        placesClient = Places.createClient(this)

        // Construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val name = findViewById<EditText>(R.id.task_name_creation)
        val description = findViewById<EditText>(R.id.task_description_creation)
        val smallDescription = findViewById<EditText>(R.id.task_small_description)
        val price = findViewById<EditText>(R.id.priceSpace)

        val db = Firebase.firestore

        val create = findViewById< Button>(R.id.create)

        val goBack = findViewById<Button>(R.id.goback)

        goBack.setOnClickListener {
            onBackPressed()
        }

        val spinner: Spinner = findViewById(R.id.spinner)

        val categories = resources.getStringArray(R.array.categories_array)
        val arrayAdapter: ArrayAdapter<Any?> = ArrayAdapter<Any?>(this, R.layout.spinner_list, categories)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_list)
        spinner.adapter = arrayAdapter

        spinner.onItemSelectedListener = this

        findViewById<ImageView>(R.id.photo).setOnClickListener {
            selectImageFromGallery()
        }

        findViewById<ImageView>(R.id.takePhoto).setOnClickListener {
            dispatchTakePictureIntent()
        }

        create.setOnClickListener {
            db.collection("users")
                .get()
                .addOnSuccessListener{

                    if(this::file_uri.isInitialized){
                        uploadImageToFirebase(file_uri)
                    }

                    val task = TaskModel()
                    task.name = name.text.toString()
                    task.description = description.text.toString()
                    task.smallDescription = smallDescription.text.toString()
                    task.cost = price.text.toString().toInt()
                    task.ownerId  = getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "")
                    task.ownerName   = getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userName", "")
                    task.status = "intended"
                    task.category = spinner.selectedItem.toString()
                    var latitude = marker.latitude.toString()
                    task.latitude = marker.latitude.toString()
                    task.longitude = marker.longitude.toString()
                    task.imageUrl = fileName

                    db.collection("task")
                        .add(task)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Task created", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MyTasks::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Task not created", Toast.LENGTH_SHORT).show()
                        }

                }

        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val tempFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
        tempFile.apply {
            // Save a file: path for use with ACTION_VIEW intents
            file_uri = Uri.fromFile(tempFile)
        }
        return tempFile
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->

            takePictureIntent.resolveActivity(packageManager)?.also {

                photoFile = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this@TaskCreation, "Camera error", Toast.LENGTH_SHORT).show()
                    null
                }

                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun selectImageFromGallery() {

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Please select..."
            ),
            GALLERY_REQUEST_CODE
        )
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == GALLERY_REQUEST_CODE
            && resultCode == Activity.RESULT_OK
            && data != null
            && data.data != null
        ) {

            // Get the Uri of data
            file_uri = data.data!!
            findViewById<ImageView>(R.id.photo).setImageURI(file_uri)
            findViewById<ImageView>(R.id.takePhoto).setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_add_a_photo_24))
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE
            && resultCode == RESULT_OK) {


            file_uri = Uri.fromFile(photoFile)
            findViewById<ImageView>(R.id.takePhoto).setImageURI(file_uri)
            findViewById<ImageView>(R.id.photo).setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_add_photo_alternate_24))
        }
    }

    private fun uploadImageToFirebase(fileUri: Uri) {

        fileName = UUID.randomUUID().toString() +".jpg"

        val refStorage = FirebaseStorage.getInstance().getReference(fileName)

        refStorage.putFile(fileUri)
            .addOnSuccessListener(
                OnSuccessListener<UploadTask.TaskSnapshot> { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener {
                        val imageUrl = it.toString()
                    }
                })

            .addOnFailureListener(OnFailureListener { e ->
                print(e.message)
            })
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
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

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(marker, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.addMarker(
            MarkerOptions()
                .position(marker)
                .draggable(true)
                .title("Here is task")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(marker))

        googleMap.isMyLocationEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled  = true

        googleMap.setOnCameraMoveStartedListener(GoogleMap.OnCameraMoveStartedListener { i ->
            if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                blockScrollView()
            }
        })

        googleMap.setOnCameraMoveCanceledListener(GoogleMap.OnCameraMoveCanceledListener { enableScrollView() })

        // Prompt the user for permission.
        getLocationPermission()

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI()

        // Get the current location of the device and set the position of the map.
        getDeviceLocation()
    }

    private fun enableScrollView() {
        findViewById<CustomScrollView>(R.id.scroll_view_creation).isEnableScrolling = true
    }

    private fun blockScrollView() {
        findViewById<CustomScrollView>(R.id.scroll_view_creation).isEnableScrolling = false
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }

    override fun onCameraIdle() {

    }

    /*private fun setStartLocation(lat: Double, lng: Double, addr: String){
        var address = "Current address"
        if (addr.isEmpty()){
            val gcd = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>
            try {
                addresses = gcd.getFromLocation(lat, lng, 1)
                if (addresses.isNotEmpty()) {
                    address = addresses[0].getAddressLine(0)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            address = addr
        }
        val icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.resources, R.drawable.ic_pickup))
        startMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title("Start Location")
                .snippet("Near $address")
                .icon(icon)
                .draggable(true)
        )!!
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(17f)
            .build()
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }*/

    /*private fun setFinishLocation(lat: Double, lng: Double, addr: String){
        var address = "Destination address"
        if (addr.isEmpty()){
            val gcd = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>
            try {
                addresses = gcd.getFromLocation(lat, lng, 1)
                if (addresses.isNotEmpty()) {
                    address = addresses[0].getAddressLine(0)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            address = addr
        }
        val icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(this.resources, R.drawable.drop_pin))
        finishMarker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title("Finish Location")
                .snippet("Near $address")
                .icon(icon)
                .draggable(true)
        )!!
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(lat, lng))
            .zoom(17f)
            .build()
    }*/

/*    override fun onMarkerDragEnd(p0: Marker?) {
        if (p0.equals(marker)){
            setStartLocation(p0.position.latitude, p0.position.longitude, "")
        } else if (p0 == finishMarker){
            setFinishLocation(p0.position.latitude, p0.position.longitude, "")
        }
    }
    override fun onMarkerDragStart(p0: Marker?) {
        Toast.makeText(this, "Changing location", Toast.LENGTH_SHORT).show()
    }
    override fun onMarkerDrag(p0: Marker?) {}*/

}