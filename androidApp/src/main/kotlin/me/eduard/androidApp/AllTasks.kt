@file:OptIn(DelicateCoroutinesApi::class)

package me.eduard.androidApp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.eduard.androidApp.model.TaskModel
import java.util.regex.Pattern


class AllTasks : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_tasks)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        val layout = findViewById<LinearLayout>(R.id.alltaskslayout)
        val search = findViewById<EditText>(R.id.searchBar)
        val searchBtn = findViewById<ImageView>(R.id.searchButton)
        /*val category*/

        val profile = findViewById<Button>(R.id.profile_icon)
        val myTask = findViewById<TextView>(R.id.mytask)
        val newTask = findViewById<CardView>(R.id.add_new_task)

        val spinner: Spinner = findViewById(R.id.spinner)

        val categories = resources.getStringArray(R.array.categories_array)
        val arrayAdapter: ArrayAdapter<Any?> = ArrayAdapter<Any?>(this, R.layout.spinner_list, categories)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_list)
        spinner.adapter = arrayAdapter

        spinner.onItemSelectedListener = this

        findViewById<Button>(R.id.help_icon).setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        searchBtn.setOnClickListener{
            GlobalScope.launch{

                val querySnapshot = if(spinner.selectedItem.toString() != ""){
                    getTasksFromDB(spinner.selectedItem.toString())
                }else{
                    getTasksFromDB()
                }



                val tasksMap = LinkedHashMap<String, TaskModel>()

                val searchText = search.text.toString()

                val p = Pattern.compile(searchText)

                querySnapshot.documents.forEach{it1 -> tasksMap[it1.id] = it1.toObject(TaskModel::class.java)!! }

                runOnUiThread {

                    layout.removeAllViews()

                    if(searchText == ""){
                        tasksMap.filter{it.value.status == "intended"}.forEach{it -> addOnLayout(layout, it.value, getChildrenViews(layout), it.key)}
                    }else{
                        tasksMap.filter{p.matcher(it.value.name.toString()).find()}.filter{it.value.status == "intended"}.forEach{it -> addOnLayout(layout, it.value, getChildrenViews(layout), it.key)}
                    }



                }
            }
        }

        newTask.setOnClickListener {
            val intent = Intent(this, TaskCreation::class.java)
            startActivity(intent)
            finish()
        }

        profile.setOnClickListener {
            val intent = Intent(this, SideMenu::class.java)
            startActivity(intent)
        }

        GlobalScope.launch{
            val querySnapshot = getTasksFromDB()
            
            val tasksMap = LinkedHashMap<String, TaskModel>()

            querySnapshot.documents.forEach{it1 -> tasksMap[it1.id] = it1.toObject(TaskModel::class.java)!! }

            runOnUiThread {
                layout.removeAllViews()
                tasksMap.filter{it.value.status == "intended"}.forEach{it -> addOnLayout(layout, it.value, getChildrenViews(layout), it.key)}

            }
        }

        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }

    }

    fun getChildrenViews(parent: ViewGroup): Int {
        return parent.childCount
    }

    fun addOnLayout(layout: LinearLayout, task: TaskModel, index: Int, taskId: String){
        val inflater =
            getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val taskView: View = inflater.inflate(R.layout.task_in_main_page, null)

        taskView.findViewById<TextView>(R.id.task_main_page_name).text = task.name
        taskView.findViewById<TextView>(R.id.task_main_page_description).text = task.smallDescription
        taskView.findViewById<TextView>(R.id.task_main_page_price).text = task.cost.toString()
        val imageView: ImageView = taskView.findViewById(R.id.taskview)

        if(task.category == "business"){
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

        taskView.setOnClickListener{

            if(task.ownerId == getSharedPreferences("me.eduard.androidApp", Context.MODE_PRIVATE).getString("userId", "")){
                val intent = Intent(this@AllTasks, TaskActivity::class.java)
                val b = Bundle()
                b.putString("taskId", taskId)

                intent.putExtras(b)

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent)
            }else{
                val intent = Intent(this@AllTasks, TaskAcceptation::class.java)
                val b = Bundle()
                b.putString("taskId", taskId)

                intent.putExtras(b)

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent)
            }



        }

        layout.addView(taskView, index)
    }

    companion object {
        suspend fun getTasksFromDB(): QuerySnapshot = Firebase.firestore
            .collection("task")
            .get()
            .await()
    }

    private suspend fun getTasksFromDB(category: String): QuerySnapshot = Firebase.firestore
            .collection("task").whereEqualTo("category", category)
            .get()
            .await()

    override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {

    }

    override fun onNothingSelected(parent: AdapterView<*>) {

    }


}

