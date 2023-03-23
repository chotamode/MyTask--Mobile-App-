package me.eduard.androidApp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.Acceptation
import me.eduard.androidApp.model.TaskModel

class TaskAcceptation : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_acceptation)

        val b = intent.extras
        var value = ""

        if (b != null) value = b.getString("taskId").toString()

        val nameTask = findViewById<TextView>(R.id.name_task_n)
        val priceTask = findViewById<TextView>(R.id.price_owner_acceptation)
        val descriptionTask = findViewById<TextView>(R.id.description)
        val owner = findViewById<TextView>(R.id.owner)
        val miniDescription = findViewById<TextView>(R.id.status_settings)

        val message = findViewById<EditText>(R.id.task_acceptation_message);
        val price = findViewById<EditText>(R.id.price_task_acceptation);

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

            runOnUiThread {
                if (task != null) {
                    nameTask.text = task.name
                    priceTask.text = task.cost.toString()
                    descriptionTask.text = task.description
                    owner.text = task.ownerName
                    miniDescription.text = task.smallDescription
                }



            }

            var accept = findViewById<Button>(R.id.accept)

            val userId = getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "")
            var alreadyAccepted = isAlreadyAccepted(value, userId.toString())

            accept.setOnClickListener {

                val db = Firebase.firestore

                if(alreadyAccepted){
                    Toast.makeText(this@TaskAcceptation, "You already accepted this task", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@TaskAcceptation, AllTasks::class.java)
                    startActivity(intent)
                    finish()
                    return@setOnClickListener
                }else{

                    val acceptation = Acceptation(
                        message.text.toString(),
                        price.text.toString(),
                        getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", ""),
                        value,
                        getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userName", "")
                    )

                    db.collection("acceptations")
                        .document()
                        .set(acceptation).addOnSuccessListener {
                            Toast.makeText(this@TaskAcceptation, "Task acceptation message sent", Toast.LENGTH_SHORT)
                                .show()

                            var notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                            var notificationChannel  = NotificationChannel("i.apps.notifications", "Task accepted", NotificationManager.IMPORTANCE_HIGH)


                            var builder = Notification.Builder(this@TaskAcceptation, "i.apps.notifications")
/*                        .setSmallIcon(R.drawable.notification_icon)*/
                                .setContentTitle("Task accepted")
                                .setContentText("Task accepted and now we are waiting for owner to choose you")
                                .setSmallIcon(R.drawable.awaiting_completion)

                            notificationManager.createNotificationChannel(notificationChannel)

                            notificationManager.notify(1234, builder.build())

                            val intent = Intent(this@TaskAcceptation, MyTasks::class.java)
                            startActivity(intent)
                            finish()
                        }
                }

            }

        }


    }

    suspend fun isAlreadyAccepted(value: String, userId: String): Boolean{
        val result = Firebase.firestore.collection("acceptations")
            .whereEqualTo("taskId", value).whereEqualTo("userId", userId).get().await()

        return (result.documents.size != 0)
    }

    suspend fun getTaskFromDB(id: String): DocumentSnapshot? {

        return Firebase.firestore
            .collection("task").document(id)
            .get()
            .await()

    }

}