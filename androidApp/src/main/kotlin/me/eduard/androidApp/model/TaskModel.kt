package me.eduard.androidApp.model

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class TaskModel(
    var name: String?,
    var smallDescription: String?,
    var description: String?,
    var ownerId: String?,
    var cost: Int?,
    var stars: Int?,
    var status: String?,
    var performerId: String?,
    var category: String?,
    var ownerName: String?,
    var performerName: String?,
    var latitude: String?,
    var longitude: String?,
    var performerMessage: String?,
    var ownerMessage: String?,
    var imageUrl: String?,
) {

    constructor() : this("", "", "", "", 0, 0, "", "", "", "", "", "", "", "", "", "")

    fun create(
        onSuccessListener: OnSuccessListener<DocumentReference>,
        onFailureListener: OnFailureListener
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("task")
            .add(this)
            .addOnSuccessListener(onSuccessListener)
            .addOnFailureListener(onFailureListener)


    }

    fun isOpened(): Boolean {
        return !(status == "owner deleted" || status == "done")
    }

    fun isStatusCorrect(): Boolean {
        return (status == "owner deleted" ||
                status == "done" ||
                status == "intended" ||
                status == "awaiting completion" ||
                status == "awaiting final confirmation" ||
                status == "performerCanceled")
    }
}