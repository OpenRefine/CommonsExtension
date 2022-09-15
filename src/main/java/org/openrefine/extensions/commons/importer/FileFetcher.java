package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * This class iterates over the file names contained in a given category
 */
public class FileFetcher implements Iterator<FileRecord>{
    String category;
    String apiUrl;
    HttpUrl urlBase;
    HttpUrl urlContinue;
    JsonNode files;
    private int indexRow = 0;
    String cmcontinue;

    public FileFetcher(String apiUrl, String category) throws IOException {
        this.apiUrl = apiUrl;
        this.category = category;
        getFiles(category);
    }

    /*
     * API call for fetching files from a user-specified category
     * @param category
     */
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

    /*
     * API call when a cmcontinue token is part of the response
     * @param urlContinue: URL containing the cmcontinue token
     */
    private void getFiles(HttpUrl urlContinue) throws IOException {

        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(urlContinue).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        files = jsonNode.path("query").path("categorymembers");
        cmcontinue = jsonNode.path("continue").path("cmcontinue").asText();

    }

    /*
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {

        return (indexRow < files.size());
    }

    /*
     * This method iterates over each of the fetched files and populates a FileRecord
     * with a single entry's filename and MID
     * @return an instance of the FileRecord
     */
    @Override
    public FileRecord next() {

        String fileName = files.get(indexRow).findValue("title").asText();
        String pageId = files.get(indexRow).findValue("pageid").asText();
        FileRecord fileRecord = new FileRecord(fileName, pageId, null, null);
        indexRow++;

        if ((indexRow == files.size()) && !cmcontinue.isBlank()) {
            urlContinue = HttpUrl.parse(urlBase.toString()).newBuilder()
                    .addQueryParameter("cmcontinue", cmcontinue).build();
            try {
                getFiles(urlContinue);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            indexRow = 0;
        }
        return fileRecord;

    }

}
