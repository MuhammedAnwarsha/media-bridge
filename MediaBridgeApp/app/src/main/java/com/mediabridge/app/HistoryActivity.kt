package com.mediabridge.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {

            val listView = ListView(this)
            setContentView(listView)

            val historyManager = HistoryManager(this)
            val items = historyManager.getItems()

            if (items.isEmpty()) {
                Toast.makeText(this, "No history found", Toast.LENGTH_SHORT).show()
                return
            }

            val displayList = items.map { it.fileName }

            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                displayList
            )

            listView.adapter = adapter

            listView.setOnItemClickListener { _, _, position, _ ->

                try {

                    val selected = items[position]
                    val uri = Uri.parse(selected.fileUri)

                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "video/mp4")
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    startActivity(intent)

                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot open video", Toast.LENGTH_SHORT).show()
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "History crashed", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
