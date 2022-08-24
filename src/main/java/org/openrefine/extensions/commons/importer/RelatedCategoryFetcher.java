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
    FileRecord fileRecordNew;
    int filesIndex;
    List <FileRecord> fileRecordOriginal;
    String apiUrl;
    HttpUrl urlRelatedCategories;

    public RelatedCategoryFetcher(String apiUrl, Iterator<FileRecord> iteratorFileRecords) {
        this.apiUrl = apiUrl;
        this.iteratorFileRecords = iteratorFileRecords;
        fileRecordOriginal = new ArrayList<>();
    }

    /*
     * API call for fetching the related categories
     * @param filename -> change to list of files
     * @param pageId
     * @return list of related categories
     */
    public List<List<String>> getRelatedCategories(List <FileRecord> fileRecordOriginal) throws IOException {

        String titles = fileRecordOriginal.get(0).fileName;
        int titlesIndex =1;
        if (titlesIndex < fileRecordOriginal.size()) {
            titles += "|" + fileRecordOriginal.get(titlesIndex++).fileName;
        }
        OkHttpClient client = new OkHttpClient.Builder().build();
        urlRelatedCategories = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("prop", "categories")
                .addQueryParameter("titles", titles)
                .addQueryParameter("format", "json").build();
        Request request = new Request.Builder().url(urlRelatedCategories).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        List<JsonNode> relatedCategories = new ArrayList<>();
        List<String> toCategoriesColumn = new ArrayList<>();
        List<List<String>> toCategoriesColumn2 = new ArrayList<>();
        for (int i = 0; i < fileRecordOriginal.size(); i++) {
            relatedCategories.add(jsonNode.path("query").path("pages").path(fileRecordOriginal.get(i).pageId).path("categories"));
            //toCategoriesColumn.add(fileRecordOriginal.get(i).pageId);
        }
        for (JsonNode category: relatedCategories) {
            toCategoriesColumn.add(category.findValue("title").asText());
        }

        // to return list of lists
        return toCategoriesColumn2;
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

        fileRecordNew = null;
        filesIndex = 0;
        // use for loop to create list of 20 .next()
        if (filesIndex < 20) {
            if (iteratorFileRecords.hasNext()) {
                fileRecordOriginal.add(iteratorFileRecords.next());
                /*try {
                } catch (IOException e) {
                    fileRecordNew = new FileRecord(fileRecordOriginal.fileName, fileRecordOriginal.pageId, null,
                            new EvalError("Could not fetch related categories: " + e.getMessage()).message);
                }*/
                filesIndex++;
            }
        }
        // send list with 20 files
        try {
            getRelatedCategories(fileRecordOriginal);
        } catch (IOException e) {
            // FIXME
            e.printStackTrace();
        }
        if (iteratorFileRecords.hasNext()) {
            filesIndex = 0;
        }
        return fileRecordNew;
    }

}
