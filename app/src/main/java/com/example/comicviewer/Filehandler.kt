package com.example.comicviewer

import android.app.DownloadManager
import android.content.Context.MODE_PRIVATE
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.util.JsonReader
import java.io.*


class Filehandler(private val context: Context) {

    fun download(url: String, fileName: String, filetype: String): Long {
        Log.d("download", "download file name: " + fileName)
        var downloadManagervar = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var Download_Uri = Uri.parse(url)
        Log.d("download", "download url: " + Download_Uri.toString())
        val request = DownloadManager.Request(Download_Uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle("Comic Viewer Downloading comic " + filetype)
        request.setDescription("Comic Viewer Downloading comic " + filetype)
        request.setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,"/ComicViewer/" + fileName)
        //request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"/ComicViewer/" + fileName)

        //    Environment.DIRECTORY_DOWNLOADS,
        //    "/ComicViewer/" + fileName
        //)
        var refid = downloadManagervar.enqueue(request)
        return refid;
    }

    @Throws(IOException::class)
    fun saveString(text: String,FILE_NAME: String) {
        var fos: FileOutputStream? = null

        try {
            Log.d("Saver", "Saver starting with data")
            fos = context.openFileOutput(FILE_NAME, MODE_PRIVATE)
            fos!!.write(text.toByteArray())
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
    fun savelist(comiclist: List<Int>,FILE_NAME: String){
        var text = comiclist.toString()
        //for (x in comiclist){
        //    text = text + x + " \n "
        //}

        var fos: FileOutputStream? = null
        try {
            Log.d("Saver", "Saver starting with comiclist")
            //File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + FILE_NAME)
            val fos = FileOutputStream(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + FILE_NAME))

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
            val fis = FileInputStream(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + FILE_NAME))
            var input = BufferedReader(InputStreamReader(fis)).readText()
            val comiclist = input.removeSurrounding("[", "]").split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
            
            Log.d("loader", "loaded list of size: " + comiclist.size.toString())

            return comiclist;
        } finally {
            if (fis != null) {
                try {
                    fis.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return mutableListOf<Int>()
    }

    @Throws(IOException::class)
    fun loadJson(FILE_NAME: String): Comic{
        val fis = FileInputStream(File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),"/ComicViewer/" + FILE_NAME))

        val reader = JsonReader(InputStreamReader(fis, "UTF-8"))
        try {
            var number    = -1
            var title     = ""
            var altText   = ""
            var imgPath   = ""
            var urlPath   = ""

            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                if (name == "img") {
                    urlPath = reader.nextString()
                } else if (name == "alt") {
                    altText = reader.nextString()
                } else if (name == "num") {
                    number = reader.nextInt()
                } else if (name == "title") {
                    title = reader.nextString()
                } else {
                    reader.skipValue()
                }
            }
            reader.endObject()
            return Comic(number,title,altText,urlPath,imgPath)
        } finally {
            reader.close()
        }
    }
}