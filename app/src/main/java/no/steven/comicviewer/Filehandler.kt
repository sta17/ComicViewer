package no.steven.comicviewer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.util.JsonReader
import java.io.*


class Filehandler(private val context: Context, private val address: String) {

    fun download(url: String, filename: String, filetype: String): Long {
        val downloadManagervar =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadUri = Uri.parse(url)
        val request = DownloadManager.Request(downloadUri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle(context.resources.getString(R.string.comic_viewer_downloading_comic) + filetype)
        request.setDescription(context.resources.getString(R.string.comic_viewer_downloading_comic) + filetype)
        request.setDestinationInExternalFilesDir(
            context,
            DIRECTORY_DOWNLOADS,
            address + filename
        )
        return downloadManagervar.enqueue(request)
    }

    @Throws(IOException::class)
    fun savelist(comiclist: List<Int>, filename: String) {
        val text = comiclist.toString()

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(
                File(
                    context.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                    address + filename
                )
            )

            //fos = context.openFileOutput(filename, MODE_PRIVATE)
            fos.write(text.toByteArray())
            fos.close()
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
    fun loadlist(filename: String): MutableList<Int> {
        var fis: FileInputStream? = null

        try {
            fis = FileInputStream(
                File(
                    context.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                    address + filename
                )
            )
            val input = BufferedReader(InputStreamReader(fis)).readText()
            val inititalInput = input.removeSurrounding("[", "]")
            if (inititalInput.isEmpty()) {
                return mutableListOf()
            }

            return inititalInput.split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
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
    fun loadJson(filename: String): Comic {
        val fis = FileInputStream(
            File(
                context.getExternalFilesDir(DIRECTORY_DOWNLOADS),
                address + filename
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
                when (reader.nextName().toString()) {
                    "img" -> urlPath = reader.nextString()
                    "alt" -> altText = reader.nextString()
                    "num" -> number = reader.nextInt()
                    "title" -> title = reader.nextString()
                    else -> reader.skipValue()
                }
            }

            reader.endObject()
            reader.close()
            return Comic(number, title, altText, urlPath, "")

        }
    }
}