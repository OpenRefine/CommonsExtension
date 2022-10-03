package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * This class iterates over the members contained in a given category
 */
public class FileFetcher implements Iterator<JsonNode>{
    String apiUrl;
    String categoryName;
    boolean subcategories;
    int depth;
    HttpUrl urlBase;
    JsonNode files;
    private int indexRow = 0;
    String cmcontinue;

    public FileFetcher(String apiUrl, String categoryName, boolean subcategories, int depth) {
        this.apiUrl = apiUrl;
        this.categoryName = categoryName;
        this.subcategories = subcategories;
        this.depth = depth;
        try {
            getFiles(categoryName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /*
     * API call for fetching files from a given category
     * @param category
     */
    public void getFiles(String category) throws IOException {

        OkHttpClient client = new OkHttpClient.Builder().build();
        urlBase = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("list", "categorymembers")
                .addQueryParameter("cmtitle", category)
                .addQueryParameter("cmtype", subcategories ? "subcat":"file")
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

    static Iterator<JsonNode> fetchCategoryMembers(String endpoint, String categoryName, boolean subcategories) {
        return new FileFetcher(endpoint, categoryName, subcategories, 0);
     }

    static Iterator<FileRecord> fetchDirectFileMembers(String endpoint, String categoryName) {
        Iterator<JsonNode> fetchedCategoryMembers = fetchCategoryMembers(endpoint, categoryName, false);
        return Iterators.transform(fetchedCategoryMembers, jsonNode->
            new FileRecord(jsonNode.findValue("title").asText(), jsonNode.findValue("pageid").asText(), null, null));
     }

    static Iterator<String> fetchSubcategories(String endpoint, String categoryName) {
        Iterator<JsonNode> fetchedCategoryMembers = fetchCategoryMembers(endpoint, categoryName, true);
        return Iterators.transform(fetchedCategoryMembers, jsonNode->
            jsonNode.findValue("title").asText());
    }

    static Iterator<FileRecord> listCategoryMembers(String endpoint, String categoryName, int depth) {
        Iterator<FileRecord> fetchedDirectFileMembers = fetchDirectFileMembers(endpoint, categoryName);// depth 0
        Iterator<String> fetchedSubcategories = fetchSubcategories(endpoint, categoryName);
        if (depth > 0) {
            Iterator<Iterator<FileRecord>> listedCategoryMembers = Iterators.transform(fetchedSubcategories, cat->
                listCategoryMembers(endpoint, cat, depth-1));
            return Iterators.concat(fetchedDirectFileMembers, Iterators.concat(listedCategoryMembers));
        } else {
            return fetchedDirectFileMembers;
        }
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
    public JsonNode next() {

        indexRow++;

        if ((indexRow == files.size()) && !cmcontinue.isBlank()) {
            HttpUrl urlContinue = HttpUrl.parse(urlBase.toString()).newBuilder()
                    .addQueryParameter("cmcontinue", cmcontinue).build();
            try {
                getFiles(urlContinue);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            indexRow = 0;
        }
        return files.get(indexRow);

    }

}
