package com.example.documenttreeexample

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    companion object {
        // Used for all shared preferences as a specific key for our app
        private const val PREFS = "SAMPLE_PREFS_ID"

        // Key for storing the URI in shared preferences for getting/setting
        private const val URI_TREE = "TREE_URI_KEY"
    }

    private lateinit var sharedPrefs: SharedPreferences

    // Non-deprecated way to use registerActivityForResult
    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            // Gets data from Intent
            val data: Intent? = result.data

            // Uri (path) of the folder we now have permission to access
            val uri = data?.data.toString()

            // These flags state that this folder should be remembered by the app as
            // to having permission to edit files in this folder
            // Otherwise, we will need to ask permission for this folder everytime the app starts
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // Makes the Uri remembered by the app, will still need to save it somewhere though
            contentResolver.takePersistableUriPermission(
                uri.toUri(),
                flags
            )

            // Saves the URI to shared preferences so we can get it anytime we want
            saveToSharedPrefs(URI_TREE, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPrefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val uriTextView = findViewById<TextView>(R.id.uriTextView)
        val setUriBtn = findViewById<TextView>(R.id.setTreeUri)
        val getUriBtn = findViewById<TextView>(R.id.getTreeUri)

        setUriBtn.setOnClickListener {
            requestDirPath()
        }

        getUriBtn.setOnClickListener {
            // Gets the URI from shared Preferences (this will be used to save the images to)
            val uri = getSharedPrefs(URI_TREE)?.toUri()

            // Checks if the URI doesn't exist
            if (uri == Uri.EMPTY || uri == null) {
                uriTextView.text = "No Uri Saved."
            }

            // Checks if the Permissions to read and write to the folder are still granted by the user
            try {
                // These throw a 'SecurityException' if the permissions to the folder have expired
                enforceUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION, "No longer have read access")
                enforceUriPermission(uri, Binder.getCallingPid(), Binder.getCallingUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION, "No longer have write access")
            } catch (e: SecurityException) {
                // Request access to folder again
                requestDirPath()
            }

            uriTextView.text = "Uri: $uri"

        }

    }

    private fun requestDirPath() {
        // Creates the Intent for Allowing the user to choose the folder we will have access to
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }

    private fun getSharedPrefs(key: String): String? {
        // Gets value from shared prefs
        return sharedPrefs.getString(key, "")
    }

    private fun saveToSharedPrefs(key: String, value: String) {
        // Saves the value to shared prefs
        with(sharedPrefs.edit()) {
            putString(key, value)
            commit()
        }
    }
}