package me.eduard.androidApp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.UserModel
import java.io.File

class SideMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_side_menu)

        val mytasks = findViewById<TextView>(R.id.myTasks)
        val settings = findViewById<TextView>(R.id.settings)
        val help = findViewById<TextView>(R.id.help)
        val logout = findViewById<TextView>(R.id.logout)

        val goBack = findViewById<Button>(R.id.goback)
        val myTask = findViewById<TextView>(R.id.textView)

        GlobalScope.launch {
            val userSnapshot = getUserFromDB(
                getSharedPreferences(
                    "me.eduard.androidApp",
                    Context.MODE_PRIVATE
                ).getString("userId", "")!!
            )
            val user = userSnapshot!!.toObject(UserModel::class.java)

            runOnUiThread{
                val profilePicture = findViewById<ImageView>(R.id.side_menu_pic)
                if(user!!.userImage != ""){
                    val localfile = File.createTempFile("tempImage", "jpg")
                    FirebaseStorage
                        .getInstance()
                        .getReference(user.userImage)
                        .getFile(localfile).addOnSuccessListener{

                            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                            profilePicture.setImageBitmap(bitmap)
                            profilePicture.background = null

                            findViewById<pl.droidsonroids.gif.GifImageView>(R.id.loading).visibility = View.GONE
                            findViewById<ImageView>(R.id.side_menu_pic).visibility = View.VISIBLE

                        }.addOnFailureListener{
                            Toast.makeText(this@SideMenu, "Failed to load picture", Toast.LENGTH_SHORT).show()
                        }
                }else{
                    findViewById<pl.droidsonroids.gif.GifImageView>(R.id.loading).visibility = View.GONE
                    profilePicture.setImageDrawable(resources.getDrawable(R.drawable.profile))
                    findViewById<ImageView>(R.id.side_menu_pic).visibility = View.VISIBLE
                }
                findViewById<TextView>(R.id.name).text = user.name
            }

        }

        myTask.setOnClickListener {
            val intent = Intent(this, AllTasks::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        goBack.setOnClickListener {
            val intent = Intent(this, AllTasks::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        mytasks.setOnClickListener {
            val intent = Intent(this, MyTasks::class.java)
            startActivity(intent)
        }

        settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        help.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        logout.setOnClickListener {
            val sharedPref: SharedPreferences =
                getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("userId", null)
            editor.putString("userName", null)
            editor.apply()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    suspend fun getUserFromDB(id: String): DocumentSnapshot? {

        return Firebase.firestore
            .collection("users").document(id)
            .get()
            .await()

    }
}