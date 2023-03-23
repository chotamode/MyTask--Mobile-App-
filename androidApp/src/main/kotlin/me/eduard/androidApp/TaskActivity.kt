@file:OptIn(DelicateCoroutinesApi::class)

package me.eduard.androidApp

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Transformations.map
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.Acceptation
import me.eduard.androidApp.model.TaskModel
import java.io.File


class TaskActivity : AppCompatActivity(), OnMapReadyCallback {

    private var marker = LatLng(50.075539, 14.437800)

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        val b = intent.extras
        var value = ""

        if (b != null) value = b.getString("taskId").toString()

        val nameTask = findViewById<TextView>(R.id.name_task)
        val priceTask = findViewById<TextView>(R.id.price_owner)
        val descriptionTask = findViewById<TextView>(R.id.description)
        val performerMessage = findViewById<TextView>(R.id.textView10)
        val hirerMessage = findViewById<TextView>(R.id.textView12)
        val owner = findViewById<TextView>(R.id.owner)
        val performer = findViewById<TextView>(R.id.performer)
        val miniDescription = findViewById<TextView>(R.id.status_settings)
        val image = findViewById<ImageView>(R.id.imageViewTask)
        val mapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)?.getMapAsync(this)

        val goback = findViewById<Button>(R.id.goback)


        goback.setOnClickListener {
            onBackPressed()
        }

        findViewById<TextView>(R.id.textView)?.setOnClickListener {
            val intent = Intent(this, AllTasks::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        GlobalScope.launch {
            val documentSnapshot = getTaskFromDB(value)

            val task = documentSnapshot?.toObject(TaskModel::class.java)

            val querySnapshot = getCandidatesFromDB(value)

            val candidates = querySnapshot?.documents?.let { ArrayList<Acceptation>(it.size) }

            querySnapshot?.documents?.forEach{ it -> it.toObject(Acceptation::class.java)
                ?.let { it1 ->
                    candidates?.add(it1)
                } }

            val layoutC = findViewById<LinearLayout>(R.id.candidates)

            runOnUiThread {

                val imageView: ImageView = findViewById(R.id.task_icon)

                if(task!!.category == "business"){
                    imageView.setImageResource(R.drawable.shop)
                }
                if(task.category == "travel"){
                    imageView.setImageResource(R.drawable.travel)
                }
                if(task.category == "cars"){
                    imageView.setImageResource(R.drawable.group_31)
                }
                if(task.category == "garden"){
                    imageView.setImageResource(R.drawable.group_32)
                }
                if(task.category == "pets"){
                    imageView.setImageResource(R.drawable.animals)
                }
                if(task.category == "video"){
                    imageView.setImageResource(R.drawable.photo)
                }
                if(task.category == "art"){
                    imageView.setImageResource(R.drawable.art)
                }
                if(task.category == "real estate"){
                    imageView.setImageResource(R.drawable.group_30)
                }

                if (task != null) {
                    nameTask.text = task.name
                    priceTask.text = task.cost.toString()
                    descriptionTask.text = task.description
                    performerMessage.text = "" //TODO
                    hirerMessage.text = ""
                    owner.text = task.ownerName
                    performer.text = task.performerName
                    miniDescription.text = task.smallDescription
                    marker = LatLng(task.latitude.toString().toDouble(), task.longitude.toString().toDouble())

                    if(task.imageUrl.toString() != ""){
                        image.visibility = View.VISIBLE
                        val localfile = File.createTempFile("tempImage", "jpg")
                        FirebaseStorage
                            .getInstance()
                            .getReference(task.imageUrl.toString())
                            .getFile(localfile).addOnSuccessListener{

                                val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                                image.setImageBitmap(bitmap)

                            }.addOnFailureListener{
                                Toast.makeText(this@TaskActivity, "Failed to load picture", Toast.LENGTH_SHORT).show()
                            }
                    }

                    if(task.latitude == ""){
                        task.latitude = 50.075539.toString()
                    }
                    if(task.longitude == ""){
                        task.longitude = 14.437800.toString()
                    }
                    marker = LatLng(task.latitude?.toDouble() ?: 50.075539, task.longitude?.toDouble() ?: 14.437800)

                    //owner buttons
                    if(task.ownerId == getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "")){

                        findViewById<View>(R.id.owner_but).visibility = View.VISIBLE

                        //owner buttons logic

                        if(
                            task.status == "owner deleted" ||
                            task.status == "done" ||
                            task.status == "intended"
                        ){
                            findViewById<Button>(R.id.find_candidates).visibility = View.GONE
                            findViewById<Button>(R.id.repeat_button).visibility = View.GONE
                        }
                        if(task.status == "intended" ||
                            task.status == "awaiting completion"){
                            findViewById<Button>(R.id.done_creator).visibility = View.GONE
                        }
                        if(
                            task.status == "done"
                        ){
                            findViewById<View>(R.id.owner_but).visibility = View.GONE
                        }
                        if(
                            task.status == "owner deleted" ||
                            task.status == "done"
                        ){
                            findViewById<View>(R.id.owner_but).visibility = View.GONE
                        }
                        val editBtn = findViewById<Button>(R.id.edit_creator)

                        editBtn.setOnClickListener{
                            nameTask.isEnabled = true
                            miniDescription.isEnabled = true
                            descriptionTask.isEnabled = true
                            priceTask.isEnabled = true
                            val done = findViewById<Button>(R.id.done_creator)
                            done.visibility = View.VISIBLE
                            done.setOnClickListener{
                                task.name = nameTask.text.toString()
                                task.description = descriptionTask.text.toString()
                                task.smallDescription = miniDescription.text.toString()
                                task.cost = priceTask.text.toString().toInt()
                                Firebase.firestore.collection("task").document(value).set(task)
                                nameTask.isEnabled = false
                                miniDescription.isEnabled = false
                                descriptionTask.isEnabled = false
                                priceTask.isEnabled = false
                                done.visibility = View.GONE
                            }
                        }

                        //Owner buttons
                        val delete = findViewById<Button>(R.id.delete_creator)
                        val repeatButton = findViewById<Button>(R.id.repeat_button)
                        val findCandidates = findViewById<Button>(R.id.find_candidates)
                        delete.setOnClickListener{
                            task.status = "owner deleted"
                            Firebase.firestore.collection("task").document(value).set(task)
                            val intent = Intent(this@TaskActivity, AllTasks::class.java)
                            Toast.makeText(this@TaskActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                            startActivity(intent)
                            finish()
                        }
                        repeatButton.setOnClickListener{
                            task.status = "awaiting completion"
                            task.performerId = ""
                            task.performerName = ""
                            Firebase.firestore.collection("task").document(value).set(task)
                        }
                        repeatButton.visibility = View.GONE
                        findCandidates.setOnClickListener {
                            task.status = "intended"
                            task.performerId = ""
                            task.performerName = ""
                            Firebase.firestore.collection("task").document(value).set(task)
                        }

                        if(task.status == "awaiting completion"){
                            repeatButton.visibility = View.GONE
                        }
                        if(task.status == "awaiting final confirmation"){
                            editBtn.visibility = View.INVISIBLE
                        }
                    }

                    if(task.performerId != null){
                        findViewById<TextView>(R.id.performer_task_activity).visibility = View.VISIBLE
                        performer.visibility = View.VISIBLE
                    }

                    if(task.status == "intended"){
                        if(task.ownerId == getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "")){
                            val candidatesLayout = findViewById<LinearLayout>(R.id.candidates)
                            candidatesLayout.visibility  = View.VISIBLE
                        }
                        candidates?.forEach { it -> addOnLayout(
                            layoutC, it, getChildrenViews(layoutC), it.userId.toString(), task, value
                        )
                        }
                    }

                    //performer buttons
                    if(
                        task.performerId == getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "") &&
                        task.status == "awaiting completion"){

                        findViewById<View>(R.id.perf_buttons).visibility = View.VISIBLE
                        val performerMessageLayout = findViewById<LinearLayout>(R.id.performer_message_layout)
                        performerMessageLayout.visibility = View.VISIBLE
                        val messagePerf = findViewById<EditText>(R.id.perf_message_edit)
                        messagePerf.visibility = View.VISIBLE

                        var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                        var notificationChannel  = NotificationChannel("i.apps.notifications", "Task abandoned/done", NotificationManager.IMPORTANCE_HIGH)

                        findViewById<Button>(R.id.done_performer).setOnClickListener {
                            task.performerMessage = messagePerf.text.toString()
                            task.status = "awaiting final confirmation"
                            Firebase.firestore
                                .collection("task").document(value)
                                .set(task)
                            Toast.makeText(this@TaskActivity, "Task done. Awaiting owner confirmation", Toast.LENGTH_SHORT).show()

                            val builder = Notification.Builder(this@TaskActivity, "i.apps.notifications")
                                .setContentTitle("Task done")
                                .setContentText("Task done and now we are waiting for owner confirmation")
                                .setSmallIcon(R.drawable.awaiting_completion)
                            notificationManager.createNotificationChannel(notificationChannel)
                            notificationManager.notify(1234, builder.build())

                            finish()
                            startActivity(intent)
                        }
                        findViewById<Button>(R.id.abandon_performer).setOnClickListener {
                            task.performerMessage = messagePerf.text.toString()
                            task.status = "performerCanceled"
                            Firebase.firestore
                                .collection("task").document(value)
                                .set(task)
                            Toast.makeText(this@TaskActivity, "Task abandoned", Toast.LENGTH_SHORT).show()

                            val builder = Notification.Builder(this@TaskActivity, "i.apps.notifications")
                                .setContentTitle("Task abandoned")
                                .setContentText("Task abandoned")
                                .setSmallIcon(R.drawable.awaiting_completion)
                            notificationManager.createNotificationChannel(notificationChannel)
                            notificationManager.notify(1234, builder.build())

                            finish()
                            startActivity(intent)
                        }

                    }

                    //owner
                    if(
                        task.ownerId == getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "") &&
                        task.status == "awaiting final confirmation"){

                        findViewById<View>(R.id.owner_but).visibility = View.VISIBLE

                        val ownerMessageLayout = findViewById<LinearLayout>(R.id.hirer_message_layout)
                        ownerMessageLayout.visibility = View.VISIBLE

                        val messageOwner= findViewById<EditText>(R.id.owner_message_edit)
                        messageOwner.visibility = View.VISIBLE

                        findViewById<LinearLayout>(R.id.stars_layout).visibility = View.VISIBLE

                        val star = findViewById<ImageView>(R.id.star)
                        val star1 = findViewById<ImageView>(R.id.star1)
                        val star2 = findViewById<ImageView>(R.id.star2)
                        val star3 = findViewById<ImageView>(R.id.star3)
                        val star4 = findViewById<ImageView>(R.id.star4)

                        var stars = 0

                        star.setOnClickListener {
                            star.alpha = 1F
                            star1.alpha = 0.5F
                            star2.alpha = 0.5F
                            star3.alpha = 0.5F
                            star4.alpha = 0.5F
                            stars = 1
                        }
                        star1.setOnClickListener {
                            star.alpha = 1F
                            star1.alpha = 1F
                            star2.alpha = 0.5F
                            star3.alpha = 0.5F
                            star4.alpha = 0.5F
                            stars = 2
                        }
                        star2.setOnClickListener {
                            star.alpha = 1F
                            star1.alpha = 1F
                            star2.alpha = 1F
                            star3.alpha = 0.5F
                            star4.alpha = 0.5F
                            stars = 3
                        }
                        star3.setOnClickListener {
                            star.alpha = 1F
                            star1.alpha = 1F
                            star2.alpha = 1F
                            star3.alpha = 1F
                            star4.alpha = 0.5F
                            stars = 4
                        }
                        star4.setOnClickListener {
                            star.alpha = 1F
                            star1.alpha = 1F
                            star2.alpha = 1F
                            star3.alpha = 1F
                            star4.alpha = 1F
                            stars = 5
                        }

                        findViewById<Button>(R.id.done_creator).setOnClickListener {
                            task.ownerMessage = messageOwner.text.toString()
                            task.status = "done"
                            task.stars = stars
                            Firebase.firestore
                                .collection("task").document(value)
                                .set(task)
                            val intent = Intent(this@TaskActivity, MyTasks::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }


                    if(task.performerMessage != ""){
                        val performerMessageLayout = findViewById<LinearLayout>(R.id.performer_message_layout)
                        performerMessageLayout.visibility = View.VISIBLE
                        val messagePerf = findViewById<TextView>(R.id.textView10)
                        messagePerf.text = task.performerMessage
                        messagePerf.visibility = View.VISIBLE
                    }

                    if(task.ownerMessage != ""){
                        val hirerMessageLayout = findViewById<LinearLayout>(R.id.hirer_message_layout)
                        hirerMessageLayout.visibility = View.VISIBLE
                        val messageOwner = findViewById<TextView>(R.id.textView12)
                        messageOwner.text = task.ownerMessage
                        messageOwner.visibility = View.VISIBLE
                    }

                    if(task.stars != null && task.stars != 0){
                        findViewById<LinearLayout>(R.id.stars_layout).visibility = View.VISIBLE

                        val star = findViewById<ImageView>(R.id.star)
                        val star1 = findViewById<ImageView>(R.id.star1)
                        val star2 = findViewById<ImageView>(R.id.star2)
                        val star3 = findViewById<ImageView>(R.id.star3)
                        val star4 = findViewById<ImageView>(R.id.star4)

                        if(task.stars!! > 0){
                            star.alpha = 1F
                        }
                        if(task.stars!! > 1){
                            star1.alpha = 1F
                        }
                        if(task.stars!! > 2){
                            star2.alpha = 1F
                        }
                        if(task.stars!! > 3){
                            star3.alpha = 1F
                        }
                        if(task.stars!! > 4){
                            star4.alpha = 1F
                        }

                    }

                }

            }

        }

    }

    fun getChildrenViews(parent: ViewGroup): Int {
        return parent.childCount
    }

    fun addOnLayout(layout: LinearLayout, candidate: Acceptation, index: Int, candidateId: String, task: TaskModel, taskId: String){
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val taskView: View = inflater.inflate(R.layout.candidate, null)

        taskView.findViewById<TextView>(R.id.candidate_name).text = candidate.namePerf
        taskView.findViewById<TextView>(R.id.candidate_message).text = candidate.message
        taskView.findViewById<TextView>(R.id.price_owner_acceptation).text = candidate.price.toString()

        taskView.findViewById<LinearLayout>(R.id.cand_lay).setOnClickListener{

            val intent = Intent(this@TaskActivity, ProfileActivity::class.java)
            val b = Bundle()
            b.putString("candidateId", candidateId)

            intent.putExtras(b)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent)
            finish()

        }

        taskView.findViewById<TextView>(R.id.choose).setOnClickListener{

            task.performerId = candidate.userId
            task.performerName = candidate.namePerf
            task.status = "awaiting completion"

            Firebase.firestore
                .collection("task").document(taskId)
                .set(task).addOnSuccessListener{
                    Toast.makeText(this, "Performer chosen", Toast.LENGTH_SHORT)
                        .show()
                }
            val intent = Intent(this, MyTasks::class.java)
            startActivity(intent)
        }

        layout.addView(taskView, index)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        googleMap.addMarker(
            MarkerOptions()
                .position(marker)
                .draggable(true)
                .title("Here is task")
        )
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(marker))
    }

    suspend fun getCandidatesFromDB(taskId: String): QuerySnapshot? {

        return Firebase.firestore
            .collection("acceptations")
            .whereEqualTo("taskId", taskId)
            .get()
            .await()
    }

    suspend fun getTaskFromDB(id: String): DocumentSnapshot? {

        return Firebase.firestore
            .collection("task").document(id)
            .get()
            .await()

    }

}