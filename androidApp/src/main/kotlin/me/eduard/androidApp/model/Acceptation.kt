package me.eduard.androidApp.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
class Acceptation(
    val message: String?,
    val price: String?,
    val userId: String?,
    val taskId: String?,
    var namePerf: String?
) {
    constructor() : this("", "", "", "", "")
}