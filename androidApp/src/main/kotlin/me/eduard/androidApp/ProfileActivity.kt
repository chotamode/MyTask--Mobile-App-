package me.eduard.androidApp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.TaskModel
import me.eduard.androidApp.model.UserModel
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap

@DelicateCoroutinesApi
class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val goback = findViewById<Button>(R.id.goback)

        goback.setOnClickListener {
            onBackPressed()
        }

        val b = intent.extras
        var value = ""

        if (b != null) value = b.getString("candidateId").toString()

        GlobalScope.launch {
            val description = findViewById<TextView>(R.id.description)
            val status = findViewById<TextView>(R.id.status_settings)
            val tasksLayout = findViewById<LinearLayout>(R.id.profile_tasks_layout)
            val userName = findViewById<TextView>(R.id.user_name_profile)
            val userSnapshot = getUserFromDB(
                value
            )

            runOnUiThread{
                val user = userSnapshot!!.toObject(UserModel::class.java)

                description.text = user?.description ?: ""
                status.text = user?.status ?: ""
                userName.text = user?.name ?: ""

                val profilePicture = findViewById<ImageView>(R.id.profile_picture)
                if(user!!.userImage != ""){
                    val localfile = File.createTempFile("tempImage", "jpg")
                    FirebaseStorage
                        .getInstance()
                        .getReference(user.userImage.toString())
                        .getFile(localfile).addOnSuccessListener{

                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            profilePicture.setImageBitmap(bitmap)

                            findViewById<pl.droidsonroids.gif.GifImageView>(R.id.loading).visibility = View.GONE
                            findViewById<ImageView>(R.id.profile_picture).visibility = View.VISIBLE

                        }.addOnFailureListener{
                            Toast.makeText(this@ProfileActivity, "Failed to load picture", Toast.LENGTH_SHORT).show()
                        }
                }else{
                    findViewById<pl.droidsonroids.gif.GifImageView>(R.id.loading).visibility = View.GONE
                    profilePicture.setImageDrawable(resources.getDrawable(R.drawable.profile))
                    findViewById<ImageView>(R.id.profile_picture).visibility = View.VISIBLE
                }


            }

            //tasks
            val querySnapshot1 = getTasksFromDBOwner(
                value
            )
            val querySnapshot2 = getTasksFromDBPerformer(
                value
            )

            val tasksMap = LinkedHashMap<String, TaskModel>()

            querySnapshot1.documents.forEach{it1 -> tasksMap[it1.id] = it1.toObject(TaskModel::class.java)!! }
            querySnapshot2.documents.forEach{it1 -> tasksMap[it1.id] = it1.toObject(TaskModel::class.java)!! }

            runOnUiThread {


                tasksMap.filter{
                    it.value.status == "done"
                }.forEach{it -> addOnLayout(tasksLayout, it.value, getChildrenViews(tasksLayout), it.key)}

            }

        }

    }

    fun getChildrenViews(parent: ViewGroup): Int {
        return parent.childCount
    }

    fun addOnLayout(layout: LinearLayout, task: TaskModel, index: Int, taskId: String){
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val taskView: View = inflater.inflate(R.layout.task_in_profile, null)

        taskView.findViewById<TextView>(R.id.name__task_profile).text = task.name
        taskView.findViewById<TextView>(R.id.description_task_profile).text = task.smallDescription
        taskView.findViewById<TextView>(R.id.price_task_profile).text = task.cost.toString()

        val imageView: ImageView = taskView.findViewById(R.id.imageView3)

        if(task.category == "business"){
            imageView.setImageResource(R.drawable.shop)
        }
        if(task.category == "travel"){
            imageView.setImageResource(R.drawable.travel)
        }
        if(task.category == "cars"){
            imageView.setImageResource(R.drawable.group_31)
        }
        if(task.category == "real estate"){
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
        if(task.category == "garden"){
            imageView.setImageResource(R.drawable.group_30)
        }

        if(task.stars!! > 0){
            taskView.findViewById<ImageView>(R.id.star).alpha = 1F
        }
        if(task.stars!! > 1){
            taskView.findViewById<ImageView>(R.id.star1).alpha = 1F
        }
        if(task.stars!! > 2){
            taskView.findViewById<ImageView>(R.id.star2).alpha = 1F
        }
        if(task.stars!! > 3){
            taskView.findViewById<ImageView>(R.id.star3).alpha = 1F
        }
        if(task.stars!! > 4){
            taskView.findViewById<ImageView>(R.id.star4).alpha = 1F
        }

        taskView.setOnClickListener{

                val intent = Intent(this@ProfileActivity, TaskActivity::class.java)
                val b = Bundle()
                b.putString("taskId", taskId)

                intent.putExtras(b)

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent)
        }

        layout.addView(taskView, index)
    }

    suspend fun getTasksFromDBOwner(ownerId: String): QuerySnapshot = Firebase.firestore
        .collection("task").whereEqualTo("ownerId", ownerId)
        .get()
        .await()


    suspend fun getTasksFromDBPerformer(performerId: String): QuerySnapshot = Firebase.firestore
        .collection("task").whereEqualTo("performerId", performerId)
        .get()
        .await()

    suspend fun getUserFromDB(id: String): DocumentSnapshot? {

        return Firebase.firestore
            .collection("users").document(id)
            .get()
            .await()

    }

}