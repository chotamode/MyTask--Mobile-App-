package me.eduard.androidApp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import me.eduard.androidApp.model.UserModel

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        val email = findViewById<EditText>(R.id.inputLogin)
        val pass = findViewById<EditText>(R.id.inputPasswordRegister)
        val submit = findViewById<Button>(R.id.submitlogin)
        submit.setOnClickListener {
            val db = Firebase.firestore
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
                Toast.makeText(this, "Email is in wrong format", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("users").whereEqualTo("email", email.text.toString())
                    .whereEqualTo("password", pass.text.toString()).get()
                    .addOnSuccessListener {
                        if (it.documents.size == 0) {
                            Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show()
                        } else {
                            val sharedPref: SharedPreferences =
                                    getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putString("userId", it.documents[0].id)
                            val userName = it.documents[0].toObject(UserModel::class.java)!!.name
                            editor.putString("userName", userName)
                            editor.apply()
                            val intent = Intent(this, AllTasks::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
        }

        val register = findViewById<TextView>(R.id.register)
        register.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    fun showRegister(view: View) {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }


}