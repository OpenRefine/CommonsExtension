package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileFetcher implements Iterator<FileRecord>{
    String category;
    String apiUrl = "https://commons.wikimedia.org/w/api.php";//FIXME
    HttpUrl urlBase;
    HttpUrl urlContinue;
    JsonNode files;
    private int indexRow = 0;
    String cmcontinue;
    Iterator<FileRecord> fileRecord;

    public FileFetcher(String category) throws IOException {
        this.category = category;
        getFiles(category);
    }

    public void getFiles(String category) throws IOException {

        OkHttpClient client = new OkHttpClient.Builder().build();
        urlBase = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("list", "categorymembers")
                .addQueryParameter("cmtitle", category)
                .addQueryParameter("cmtype", "file")
                .addQueryParameter("cmprop", "title|type|ids")
                .addQueryParameter("cmlimit", "500")
                .addQueryParameter("format", "json").build();
        Request request = new Request.Builder().url(urlBase).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        files = jsonNode.path("query").path("categorymembers");
        cmcontinue = jsonNode.path("continue").path("cmcontinue").asText();

    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FileRecord next() {
        FileRecord fileRecord = new FileRecord();
        fileRecord.fileName = files.get(indexRow).findValue("title").asText();
        fileRecord.mId = "M" + files.get(indexRow).findValue("pageid").asText();
        indexRow++;

        //return new instance of FileRecord
        return fileRecord;
    }

}
