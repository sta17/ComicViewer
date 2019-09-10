package com.example.comicviewer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.JsonReader
import android.util.Log
import java.io.*


class Filehandler(private val context: Context,private val address: String) {

    fun download(url: String, fileName: String, filetype: String): Long {
        Log.d("download", "download file name: $fileName")
        val downloadManagervar =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        Log.d("download", "download url: $downloadUri")
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        val description = "Comic Viewer Downloading comic $filetype"
        request.setTitle(description)
        request.setDescription(description)
        request.setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            address + fileName
        )
        return downloadManagervar.enqueue(request)
    }

    @Throws(IOException::class)
    fun savelist(comiclist: List<Int>, FILE_NAME: String) {
        val text = comiclist.toString()

        var fos: FileOutputStream? = null
        try {
            Log.d("Saver", "Saver starting with comiclist")
            //File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + FILE_NAME)
            fos = FileOutputStream(
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    address + FILE_NAME
                )
            )

            //fos = context.openFileOutput(FILE_NAME, MODE_PRIVATE)
            fos.write(text.toByteArray())
            fos.close()
            Log.d("saver", "Saved to " + context.getFileStreamPath(FILE_NAME))
        } finally {
            if (fos != null) {
                try {
                    fos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun loadlist(FILE_NAME: String): MutableList<Int> {
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    address + FILE_NAME
                )
            )
            val input = BufferedReader(InputStreamReader(fis)).readText()
            val inititalinput = input.removeSurrounding("[", "]")
            if (inititalinput.isEmpty()) {
                return mutableListOf()
            }
            val comiclist =
                inititalinput.split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
            Log.d("loader", "loaded list of size: " + comiclist.size.toString())
            return comiclist
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun loadJson(FILE_NAME: String): Comic {
        val fis = FileInputStream(
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                address + FILE_NAME
            )
        )

        val reader = JsonReader(InputStreamReader(fis, "UTF-8"))
            reader.use {
                var number = -1
                var title = ""
                var altText = ""
                var urlPath = ""

                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    val input = reader.nextString()
                    while (name == "img") {
                        urlPath = input
                    }
                    while (name == "alt") {
                        altText = input
                    }
                    while (name == "num") {
                        number = input.toInt()
                    }
                    while (name == "title") {
                        title = input
                    }
                    reader.skipValue()
                }

                reader.endObject()
                reader.close()
                return Comic(number, title, altText, urlPath, "")

            }
    }
}