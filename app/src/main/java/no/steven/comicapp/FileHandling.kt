package no.steven.comicapp

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.JsonReader
import java.io.*

/*
Get the wanted Json file
*/
fun loadJson(filename: String,downloadLocation: File): Comic {
    val fis = FileInputStream(
        File(
            downloadLocation,
            filename
        )
    )

    val reader = JsonReader(InputStreamReader(fis, "UTF-8"))
    reader.use {
        var number = -2 // want it to be a number not used, and that is nonsense.
        var title = ""
        var altText = ""
        var urlPath = ""
        var year = -1
        var month = -1
        var day = -1

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName().toString()) {
                "img" -> urlPath = reader.nextString()
                "alt" -> altText = reader.nextString()
                "num" -> number = reader.nextInt()
                "title" -> title = reader.nextString()
                "year" -> year = reader.nextInt()
                "month" -> month = reader.nextInt()
                "day" -> day = reader.nextInt()
                else -> reader.skipValue()
            }
        }

        reader.endObject()
        reader.close()
        return Comic(number, title, altText, urlPath, "",year,month,day)

    }
}

@Throws(IOException::class)
fun saveList(comicList: List<Int>, filename: String, downloadLocation: File) {
    val text = comicList.toString()

    var fos: FileOutputStream? = null
    try {
        fos = FileOutputStream(
            File(
                downloadLocation,
                filename
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
fun loadList(filename: String, downloadLocation: File): MutableList<Int> {
    var fis: FileInputStream? = null

    try {
        fis = FileInputStream(
            File(
                downloadLocation,
                filename
            )
        )
        val input = BufferedReader(InputStreamReader(fis)).readText()
        val initialInput = input.removeSurrounding("[", "]")
        if (initialInput.isEmpty()) {
            return mutableListOf()
        }

        return initialInput.split(",").map { it.trim() }.map { it.toInt() }.toMutableList()
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

fun download(url: String, filename: String, fileType: String,title: String,context: Context): Long {
    val downloadManagerVar = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadUri = Uri.parse(url)
    val request = DownloadManager.Request(downloadUri)
    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
    request.setAllowedOverRoaming(false)
    request.setTitle(title + fileType)
    request.setDescription(title + fileType)
    request.setDestinationInExternalFilesDir(
        context,
        Environment.DIRECTORY_DOWNLOADS,
        filename
    )
    return downloadManagerVar.enqueue(request)
}

/*
    check if the number is to high or low
    returns true if legal number,
    false if not
     */
fun legalNumber(number: Int,firstComicNumber: Int,latestComicNumber: Int): Boolean {
    return when ((firstComicNumber <= number) && (number <= latestComicNumber)) {
        true -> true
        false -> false
    }
}

fun getComic(toGet: Int, comicList: MutableList<Int>,downloadLocation : File,downloadRefIdList: MutableMap<Long, Int>,jsonRefIdList: MutableMap<Long, Int>,imgRefIdList: MutableMap<Long, Int>,context: Context,firstComicNumber: Int,latestComicNumber: Int): Boolean {
    // Check if in list already:
    return when ((toGet in comicList) && legalNumber(toGet,firstComicNumber,latestComicNumber)) {
        true -> true
        false -> return when (toGet == 0 || toGet == -1) {
            true -> downloadLatest(toGet,downloadLocation,downloadRefIdList,jsonRefIdList,context)
            false -> return when ((toGet in comicList) && legalNumber(toGet,firstComicNumber,latestComicNumber)) {
                true -> true
                false -> return when (!File(downloadLocation, "$toGet.json").exists()) {
                    true -> downloadJson(toGet,downloadRefIdList,jsonRefIdList,context)
                    false -> return when (!File(
                        downloadLocation,
                        "$toGet.png"
                    ).exists()) {
                        false -> downloadImage(toGet,downloadLocation,downloadRefIdList,imgRefIdList,context)
                        true -> comicList.add(toGet)
                    }
                }
            }
        }
    }
}

private fun downloadLatest(toGet: Int,downloadLocation: File,downloadRefIdList: MutableMap<Long, Int>,jsonRefIdList: MutableMap<Long, Int>,context: Context): Boolean {
    val latest = File(
        downloadLocation,
        "latest.json"
    )
    latest.absoluteFile.delete()
    if (latest.exists()){
        latest.absoluteFile.delete()
    }

    val refIdJson = download("https://xkcd.com/info.0.json", "latest.json", "txt","Comic App",context)

    downloadRefIdList[refIdJson] = toGet
    jsonRefIdList[refIdJson] = toGet

    return false
}

private fun downloadJson(toGet: Int,downloadRefIdList: MutableMap<Long, Int>,jsonRefIdList: MutableMap<Long, Int>,context: Context): Boolean {
    val refIdJson = download("https://xkcd.com/$toGet/info.0.json", "$toGet.json", "txt","Comic App",context)

    downloadRefIdList[refIdJson] = toGet
    jsonRefIdList[refIdJson] = toGet
    return false
}

private fun downloadImage(toGet: Int,downloadLocation: File,downloadRefIdList: MutableMap<Long, Int>,imgRefIdList: MutableMap<Long, Int>,context: Context): Boolean {
    val refIdImg = download(
        loadJson("$toGet.json",downloadLocation).imgUrl, "$toGet.png", "png","Comic App",context)

    downloadRefIdList[refIdImg] = toGet
    imgRefIdList[refIdImg] = toGet
    return false
}