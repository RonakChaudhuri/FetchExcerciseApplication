package com.example.fetchapplication

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.fetchapplication.ui.theme.FetchApplicationTheme
import android.widget.ListView
import android.os.AsyncTask
import android.widget.ArrayAdapter
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private lateinit var listView: ListView

    // Sets the view to activity main xml, initializes list view to data
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.listView)

        FetchDataTask().execute("https://fetch-hiring.s3.amazonaws.com/hiring.json")
    }

    // Class to asynchronously fetch data in the background
    inner class FetchDataTask : AsyncTask<String, Void, List<Item>>() {
        // runs background thread and fetches JSON data from the URL
        override fun doInBackground(vararg params: String?): List<Item> {
            val url = URL(params[0])
            val connection = url.openConnection() as HttpURLConnection

            try {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }

                val jsonArray = JSONArray(response.toString())
                return parseJsonArray(jsonArray)
            } finally {
                connection.disconnect()
            }
        }

        // takes list of items and displays it on UI
        override fun onPostExecute(result: List<Item>?) {
            super.onPostExecute(result)
            result?.let {
                displayItems(it)
            }
        }
    }

    // parses the JSON array and returns a list of items(the item class object)
    private fun parseJsonArray(jsonArray: JSONArray): List<Item> {
        val items = mutableListOf<Item>()
        for (i in 0 until jsonArray.length()) {
            val itemJson = jsonArray.getJSONObject(i)
            val id = itemJson.getInt("id")
            val listId = itemJson.getInt("listId")
            val name = itemJson.optString("name", null)
            if (!name.isNullOrBlank() && name != "null") {
                items.add(Item(id, listId, name))
            }
        }
        return items
    }

    // groups items by listId, sets custom adapter for list view
    private fun displayItems(items: List<Item>) {
        val groupedItems = items.groupBy { it.listId }
        val customAdapter = CustomAdapter(groupedItems, this@MainActivity)
        listView.adapter = customAdapter
    }

    // Custom Adapter class that takes of map of grouped items, sorts the keys(listIds) and then
    // iterates through the values(items), sorts them by name, and adds string to the adapter
    private class CustomAdapter(private val groupedItems: Map<Int, List<Item>>, context: Context) :
        ArrayAdapter<String>(context, android.R.layout.simple_list_item_1) {

        init {
            var keys = groupedItems.keys.toList()
            keys = keys.sorted()
            addAll(keys.flatMap { key ->
                val items = groupedItems[key]?.sortedBy { it.name } ?: emptyList()
                items.mapIndexed { index, item ->
                    if (index == 0) "ListId: $key\n${item.name}" else item.name
                }
            })
        }
    }

}