package me.eduard.androidApp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import me.eduard.androidApp.model.UserModel
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {
    var birthdateView: TextView? = null
    var cal: Calendar = Calendar.getInstance()
    val db = Firebase.firestore
    private fun updateDateInView() {
        val myFormat = "MM/dd/yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.GERMAN)
        birthdateView!!.text = sdf.format(cal.time)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        db.collection("timestamp").document("last")
            .set(mapOf("timestamp" to System.currentTimeMillis()))


        val name = findViewById<EditText>(R.id.inputName)
        val email = findViewById<EditText>(R.id.inputEmail)
        val phone = findViewById<EditText>(R.id.inputPhone)
        val password = findViewById<EditText>(R.id.inputPasswordRegister)
        val passwordRepeat = findViewById<EditText>(R.id.inputPasswordRepeat)
        val submit = findViewById<Button>(R.id.register)
        birthdateView = findViewById(R.id.inputBirthdate)

        birthdateView!!.text = "01/01/1970"

        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        birthdateView!!.setOnClickListener {
            DatePickerDialog(
                this@RegisterActivity,
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        findViewById<TextView>(R.id.alreadyHasAccount).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        submit.setOnClickListener {
            if (name.text.length < 3) {
                Toast.makeText(this, "Name must be at least 3 characters", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.text).matches()) {
                Toast.makeText(this, "Email is in wrong format", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            if (!android.util.Patterns.PHONE.matcher(phone.text).matches()) {
                Toast.makeText(this, "Phone is in wrong format", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }


            if (password.text.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_LONG)
                    .show()
                return@setOnClickListener

            }
            if (password.text.toString() != passwordRepeat.text.toString()) {
                Toast.makeText(this, "Password doesn't match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            println("GOOD")

            // check email existing
            db.collection("users")
                .whereEqualTo("email", email.text.toString())
                .get()
                .addOnSuccessListener { res ->
                    if (res.isEmpty) {
                        val user = UserModel(
                            name.text.toString(),
                            email.text.toString(),
                            password.text.toString(),
                            phone.text.toString(),
                            birthdateView!!.text.toString()
                        )
                        user.create({
                            val sharedPreferences: SharedPreferences =
                                getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE)
                            val editor: SharedPreferences.Editor = sharedPreferences.edit()
                            editor.putString("userId", it.id)
                            editor.apply()
                            val intent = Intent(this, MyTasks::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }, { err ->
                            println("FAILED")
                            println(err.message)
                            Toast.makeText(
                                this,
                                "Failed to register. Try again later",
                                Toast.LENGTH_SHORT
                            ).show()
                        })
                    } else {
                        Toast.makeText(this, "Email already exist", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener { err ->
                    println("FAILED")
                    println(err.message)
                    Toast.makeText(
                        this,
                        "Failed to register. Try again later",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    fun showLogin(view: View) {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}