package com.example.comicviewer

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Nullable


class DownloadComicService : IntentService("DownloadComicService") {
    override fun onHandleIntent(@Nullable intent: Intent?) {
        val destinationPath = intent!!.getStringExtra(DESTINATION_PATH)
        val downloadPath    = intent.getStringExtra(DOWNLOAD_PATH)
        downloadComicData(downloadPath, destinationPath)

    }

    companion object {
        private val DESTINATION_PATH = "com.example.androiddownloadmanager_DownloadComicService_Destination_path"
        private val DOWNLOAD_PATH = "com.example.androiddownloadmanager_DownloadComicService_Download_path"
        fun getDownloadService(callingClassContext: Context, downloadPath: String, destinationPath: String): Intent {
            return Intent(callingClassContext, DownloadComicService::class.java)
                .putExtra(DESTINATION_PATH, destinationPath)
                .putExtra(DOWNLOAD_PATH, downloadPath)
        }
    }

    private fun downloadComicData(downloadPath: String?, destinationPath: String?){
        val uri = Uri.parse(downloadPath) // Path where you want to download file.
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)  // Tell on which network you want to download file.
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)  // This will show notification on top when downloading the file.
        request.setTitle("Downloading Comic Data") // Title for notification.
        request.setDestinationInExternalPublicDir(destinationPath, uri.lastPathSegment)  // Storage directory path
        (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) // This will start downloading
    }

}