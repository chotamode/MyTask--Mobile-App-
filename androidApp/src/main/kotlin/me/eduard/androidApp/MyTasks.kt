@file:OptIn(DelicateCoroutinesApi::class)

package me.eduard.androidApp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.TaskModel

class MyTasks : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_tasks)

        val acceptedBtn = findViewById<Button>(R.id.accepted)
        val demandedBtn = findViewById<Button>(R.id.demanded)
        val openBtn = findViewById<Button>(R.id.open)
        val closedBtn = findViewById<Button>(R.id.closed)

        acceptedBtn.setBackgroundColor(resources.getColor(R.color.gray))
        demandedBtn.setBackgroundColor(resources.getColor(R.color.blue))
        openBtn.setBackgroundColor(resources.getColor(R.color.gray))
        closedBtn.setBackgroundColor(resources.getColor(R.color.blue))


        var accepted = false;
        var opened = false;

        val layout = findViewById<LinearLayout>(R.id.scrollableMyTasks)

        val sharedPreferences: SharedPreferences =
            getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

        val goBack = findViewById<Button>(R.id.goback)

        goBack.setOnClickListener {
            val intent = Intent(this, SideMenu::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }



        GlobalScope.launch{
            val querySnapshot = AllTasks.getTasksFromDB()

            val tasksMap = LinkedHashMap<String, TaskModel>()

            querySnapshot.documents.forEach{it1 -> tasksMap[it1.id] = it1.toObject(TaskModel::class.java)!! }

            runOnUiThread {
                layout.removeAllViews()
                tasksMap.filter{it -> it.value.isOpened() == opened}.filter{it -> if(accepted){
                    it.value.performerId == userId
                }else{
                    it.value.ownerId == userId
                }
                }.forEach{it ->
                    addOnLayout(layout, it.value, getChildrenViews(layout), it.key)
                }
            }

            acceptedBtn.setOnClickListener {
                accepted = true
                runOnUiThread {
                    layout.removeAllViews()
                    tasksMap.filter{it -> it.value.isOpened() == opened}.filter{it -> if(accepted){
                        it.value.performerId == userId
                    }else{
                        it.value.ownerId == userId
                    }
                    }.forEach{it ->
                        addOnLayout(layout, it.value, getChildrenViews(layout), it.key)
                    }
                }
                acceptedBtn.setBackgroundColor(resources.getColor(R.color.blue))
                demandedBtn.setBackgroundColor(resources.getColor(R.color.gray))
            }

            demandedBtn.setOnClickListener {
                accepted = false
                runOnUiThread {
                    layout.removeAllViews()
                    tasksMap.filter{it -> it.value.isOpened() == opened}.filter{it -> if(accepted){
                        it.value.performerId == userId
                    }else{
                        it.value.ownerId == userId
                    }
                    }.forEach{it ->
                        addOnLayout(layout, it.value, getChildrenViews(layout), it.key)
                    }
                }
                acceptedBtn.setBackgroundColor(resources.getColor(R.color.gray));
                demandedBtn.setBackgroundColor(resources.getColor(R.color.blue));
            }

            openBtn.setOnClickListener {
                opened = true
                runOnUiThread {
                    layout.removeAllViews()
                    tasksMap.filter{it -> it.value.isOpened() == opened}.filter{it -> if(accepted){
                        it.value.performerId == userId
                    }else{
                        it.value.ownerId == userId
                    }
                    }.forEach{it ->
                        addOnLayout(layout, it.value, getChildrenViews(layout), it.key)
                    }
                }
                openBtn.setBackgroundColor(resources.getColor(R.color.blue))
                closedBtn.setBackgroundColor(resources.getColor(R.color.gray))
            }

            closedBtn.setOnClickListener {
                opened = false
                runOnUiThread {
                    layout.removeAllViews()
                    tasksMap.filter{it -> it.value.isOpened() == opened}.filter{it -> if(accepted){
                        it.value.performerId == userId
                    }else{
                        it.value.ownerId == userId
                    }
                    }.forEach{it ->
                        addOnLayout(layout, it.value, getChildrenViews(layout), it.key)
                    }
                }
                openBtn.setBackgroundColor(resources.getColor(R.color.gray))
                closedBtn.setBackgroundColor(resources.getColor(R.color.blue))
            }
        }
    }

    fun getChildrenViews(parent: ViewGroup): Int {
        return parent.childCount
    }

    fun addOnLayout(layout: LinearLayout, task: TaskModel, index: Int, taskId: String){
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val taskView: View = inflater.inflate(R.layout.task_mytasks, null)

        taskView.findViewById<TextView>(R.id.task_name).text = task.name
        taskView.findViewById<TextView>(R.id.candidate_message).text = task.smallDescription

        when (task.status) {
            "done" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.done)
            }
            "owner deleted" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.owner_canceled)
            }
            "intended" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.searching_cand)
            }
            "awaiting completion" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.awaiting_completion)
            }
            "awaiting final confirmation" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.performer_confirmed)
            }
            "performerCanceled" -> {
                taskView.findViewById<ImageView>(R.id.imageView).setImageResource(R.drawable.performer_canceled)
            }
        }

        layout.addView(taskView, index)

        taskView.setOnClickListener{

            val intent = Intent(this@MyTasks, TaskActivity::class.java)
            val b = Bundle()
            b.putString("taskId", taskId)

            intent.putExtras(b)

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent)
        }
    }

    suspend fun getTasksFromDB(accepted: Boolean): QuerySnapshot? {


        val acceptedType = if(accepted){
            "owner"
        }else{
            "performer"
        }

        val sharedPreferences: SharedPreferences =
            getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", null)

    return Firebase.firestore
    .collection("task").whereEqualTo(acceptedType, userId)
    .get()
    .await()

    }
}