package me.eduard.androidApp.model

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserModel(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val birthdate: String = "",
    var description: String = "",
    var status: String = "",
    var userImage: String = ""
) {
    constructor() : this("", "", "", "", "", "", "", "")

    fun create(
        onSuccessListener: OnSuccessListener<DocumentReference>,
        onFailureListener: OnFailureListener
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .add(this)
            .addOnSuccessListener(onSuccessListener)
            .addOnFailureListener(onFailureListener)
    }
}