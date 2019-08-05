package com.example.comicviewer

import android.util.JsonReader
import java.io.InputStream
import java.io.InputStreamReader

class readData {

    fun createMsgToData(inputStream: InputStream?): jsonComic {

        val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
        var altText: String = "";
        var imgLink: String = "";
        var title: String = "";
        var num: Int = -1;

        try {
            reader.beginArray()
            while (reader.hasNext()) {
                val name = reader.nextName();
                if (name.equals("Alt")) {
                    altText = reader.nextString();
                } else if (name.equals("img")) {
                    imgLink = reader.nextString();
                } else if (name.equals("title")) {
                    title = reader.nextString();
                } else if (name.equals("num")) {
                    num = reader.nextInt()
                } else {
                    reader.skipValue();
                }
            }
            reader.endArray()
        } finally {
            reader.close()
        }

        var jsoncomic: jsonComic = jsonComic("",num,"","","","","",altText,imgLink,title,"")
        return jsoncomic;
    }
}