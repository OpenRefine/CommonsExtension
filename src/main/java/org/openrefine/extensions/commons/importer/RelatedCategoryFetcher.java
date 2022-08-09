package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RelatedCategoryFetcher implements Iterator<FileRecord> {
    Iterator<FileRecord> iteratorFileRecords;
    String filename;
    String apiUrl;
    HttpUrl urlRelatedCategories;
    String pageId;
    JsonNode relatedCategories;
    List<String> toCategoriesColumn;

    public RelatedCategoryFetcher(String apiUrl, Iterator<FileRecord> iteratorFileRecords) {
        this.apiUrl = apiUrl;
        this.iteratorFileRecords = iteratorFileRecords;
    }

    public List<String> getRelatedCategories(String filename, String pageID) throws IOException {

        OkHttpClient client = new OkHttpClient.Builder().build();
        urlRelatedCategories = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("prop", "categories")
                .addQueryParameter("titles", filename)
                .addQueryParameter("format", "json").build();
        Request request = new Request.Builder().url(urlRelatedCategories).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        relatedCategories = jsonNode.path("query").path("pages").path(pageId).path("categories");
        toCategoriesColumn = new ArrayList<>();
        for (JsonNode category: relatedCategories) {
            toCategoriesColumn.add(category.findValue("title").toString());
        }

        return toCategoriesColumn;
    }

    @Override
    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FileRecord next() {

        FileRecord fileRecord;
        if (iteratorFileRecords.hasNext() != false) {
            fileRecord = iteratorFileRecords.next();
            try {
                new FileRecord(fileRecord.fileName, fileRecord.pageId, getRelatedCategories(fileRecord.fileName, fileRecord.pageId));
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            System.out.println("\n2: " + fileRecord);
        }

        return null;
    }

}
