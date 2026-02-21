package com.mediabridge.app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var selectButton: Button
    private lateinit var convertButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var modeSpinner: Spinner

    private lateinit var loadingOverlay: View
    private lateinit var loadingText: TextView
    private lateinit var mainCard: View

    private val BASE_URL = "http://192.168.1.2:8080/MediaBridge/"
    private var selectedFileUri: Uri? = null
    private var currentJobId: String? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = getColor(R.color.primaryColor)
        window.navigationBarColor = getColor(R.color.primaryColor)

        setContentView(R.layout.activity_main)

        selectButton = findViewById(R.id.selectButton)
        convertButton = findViewById(R.id.convertButton)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        modeSpinner = findViewById(R.id.modeSpinner)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        loadingText = findViewById(R.id.loadingText)
        mainCard = findViewById(R.id.mainCard)

        // Initial UI State
        loadingOverlay.visibility = View.GONE
        statusText.visibility = View.GONE
        progressBar.visibility = View.GONE

        // Spinner setup
        val modes = arrayOf("Safe (720p)", "High (1080p)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        modeSpinner.adapter = adapter

        selectButton.setOnClickListener {
            filePicker.launch("video/*")
        }

        convertButton.setOnClickListener {
            selectedFileUri?.let {
                uploadVideo(it)
            } ?: Toast.makeText(this, "Select a video first", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------------------
    // FILE PICKER
    // ------------------------------
    private val filePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            selectedFileUri = uri
            statusText.visibility = View.VISIBLE
            statusText.text = "Video Selected"
        }

    // ------------------------------
    // LOADING CONTROLS
    // ------------------------------
    private fun showLoading(message: String) {
        runOnUiThread {
            loadingOverlay.visibility = View.VISIBLE
            loadingText.text = message
            mainCard.alpha = 0.4f
            mainCard.isEnabled = false
            convertButton.isEnabled = false
        }
    }

    private fun hideLoading() {
        runOnUiThread {
            loadingOverlay.visibility = View.GONE
            mainCard.alpha = 1f
            mainCard.isEnabled = true
            convertButton.isEnabled = true
        }
    }

    // ------------------------------
    // UPLOAD
    // ------------------------------
    private fun uploadVideo(uri: Uri) {

        statusText.visibility = View.GONE
        progressBar.visibility = View.GONE

        showLoading("Uploading...")

        val inputStream = contentResolver.openInputStream(uri)
        val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}.mov")

        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val selectedMode =
            if (modeSpinner.selectedItemPosition == 1) "high" else "safe"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                tempFile.name,
                RequestBody.create("video/*".toMediaTypeOrNull(), tempFile)
            )
            .addFormDataPart("mode", selectedMode)
            .build()

        val request = Request.Builder()
            .url("${BASE_URL}media/convert")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                hideLoading()
                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Upload Failed"
                }
            }

            override fun onResponse(call: Call, response: Response) {

                val body = response.body?.string()
                val jobId = body
                    ?.substringAfter("jobId\":\"")
                    ?.substringBefore("\"")

                if (jobId != null) {
                    currentJobId = jobId
                    showLoading("Processing...")
                    startPolling(jobId)
                } else {
                    hideLoading()
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Invalid server response"
                    }
                }
            }
        })
    }

    // ------------------------------
    // STATUS POLLING
    // ------------------------------
    private fun startPolling(jobId: String) {

        Thread {
            while (true) {

                try {
                    val request = Request.Builder()
                        .url("${BASE_URL}media/status/$jobId")
                        .build()

                    val response = client.newCall(request).execute()
                    val result = response.body?.string()

                    if (result != null) {

                        val json = JSONObject(result)
                        val status = json.getString("status")

                        if (status == "COMPLETED") {
                            downloadVideo(jobId)
                            break
                        }

                        if (status == "FAILED") {
                            hideLoading()
                            runOnUiThread {
                                statusText.visibility = View.VISIBLE
                                statusText.text = "Conversion Failed"
                            }
                            break
                        }
                    }

                    Thread.sleep(2000)

                } catch (e: Exception) {
                    hideLoading()
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Status Error"
                    }
                    break
                }
            }
        }.start()
    }

    // ------------------------------
    // DOWNLOAD
    // ------------------------------
    private fun downloadVideo(jobId: String) {

        runOnUiThread {
            loadingText.text = "Downloading..."
        }

        Thread {
            try {

                val request = Request.Builder()
                    .url("${BASE_URL}media/download/$jobId")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    hideLoading()
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Download Failed"
                    }
                    return@Thread
                }

                val inputStream = response.body?.byteStream()
                val fileName = "MediaBridge_$jobId.mp4"

                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MediaBridge")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }

                val resolver = contentResolver
                val collection =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val videoUri = resolver.insert(collection, values)

                if (videoUri != null && inputStream != null) {

                    resolver.openOutputStream(videoUri)?.use { output ->
                        inputStream.copyTo(output)
                    }

                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(videoUri, values, null, null)

                    hideLoading()
                    playSuccessAnimation()
                }

            } catch (e: Exception) {
                hideLoading()
                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Download Error"
                }
            }
        }.start()
    }

    // ------------------------------
    // SUCCESS ANIMATION
    // ------------------------------
    private fun playSuccessAnimation() {

        runOnUiThread {

            statusText.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            statusText.text = "Conversion Completed!"
            progressBar.progress = 100

            val scaleX = ObjectAnimator.ofFloat(progressBar, "scaleX", 1f, 1.15f, 1f)
            val scaleY = ObjectAnimator.ofFloat(progressBar, "scaleY", 1f, 1.15f, 1f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY)
            animatorSet.duration = 400
            animatorSet.start()
        }
    }
}
