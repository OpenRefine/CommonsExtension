package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.refine.expr.EvalError;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/*
 * This class takes an existing iterator over file records, and enriches
 * the file records with the list of categories they belong to
 *
 * @param apiUrl
 * @param iteratorFileRecords
 */
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

    /*
     * API call for fetching the related categories
     * @param filename
     * @param pageId
     * @return list of related categories
     */
    public List<String> getRelatedCategories(String filename, String pageId) throws IOException {

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
            toCategoriesColumn.add(category.findValue("title").asText());
        }

        return toCategoriesColumn;
    }

    /*
     * Returns {@code true} if the iteration has more elements for
     * which to fetch related categories.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {

        return iteratorFileRecords.hasNext();
    }

    /*
     * This method iterates over each of the categories related to a file
     * and stores them as a list in the relatedCategories parameter of
     * each file record
     *
     * @return an instance of the FileRecord updated to include its related categories
     */
    @Override
    public FileRecord next() {

        FileRecord fileRecordOriginal;
        FileRecord fileRecordNew = null;
        if (iteratorFileRecords.hasNext() != false) {
            fileRecordOriginal = iteratorFileRecords.next();
            try {
                fileRecordNew = new FileRecord(fileRecordOriginal.fileName, fileRecordOriginal.pageId,
                        getRelatedCategories(fileRecordOriginal.fileName, fileRecordOriginal.pageId), null);
            } catch (IOException e) {
                fileRecordNew = new FileRecord(fileRecordOriginal.fileName, fileRecordOriginal.pageId, null,
                        new EvalError("Could not fetch related categories: " + e.getMessage()).message);
            }
        }
        return fileRecordNew;
    }

    @Override
    public String toString() {
        return "RelatedCategoryFetcher [filename=" + filename
                + ", pageId=" + pageId + ", toCategoriesColumn=" + toCategoriesColumn + "]";
    }


}
