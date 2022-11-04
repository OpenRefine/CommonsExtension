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

/**
 * This class takes an existing iterator over file records, and enriches
 * the file records with the list of categories they belong to
 *
 * @param apiUrl
 * @param iteratorFileRecords
 */
public class RelatedCategoryFetcher implements Iterator<FileRecord> {
    /* FIXME: Increase API_TITLES_LIMIT to the maximum of 50 once
     * clcontinue has been implemented
     * https://github.com/OpenRefine/CommonsExtension/issues/53
     * */
    public static int API_TITLES_LIMIT = 10;
    public int apiLimit = API_TITLES_LIMIT;
    Iterator<FileRecord> iteratorFileRecords;
    List<FileRecord> fileRecordNew = new ArrayList<>();
    String apiUrl;
    int fileRecordNewIndex = 0;

    public RelatedCategoryFetcher(String apiUrl, Iterator<FileRecord> iteratorFileRecords) {
        this.apiUrl = apiUrl;
        this.iteratorFileRecords = iteratorFileRecords;
    }

    /**
     * Utility method for testing api calls with variable number of titles
     *
     * @param limit
     */
    public void setApiLimit(int limit) {
        apiLimit = limit;
    }

    /**
     * API call for fetching the related categories in batches of up to the number set by apiLimit
     *
     * @param list of file records
     * @return list of related categories listed per file
     */
    public List<List<String>> getRelatedCategories(List <FileRecord> fileRecordOriginal) throws IOException {

        String titles = fileRecordOriginal.get(0).fileName;
        int titlesIndex =1;
        while (titlesIndex < fileRecordOriginal.size()) {
            titles += "|" + fileRecordOriginal.get(titlesIndex++).fileName;
        }
        OkHttpClient client = new OkHttpClient.Builder().build();
        HttpUrl urlRelatedCategoriesBase = HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("prop", "categories")
                .addQueryParameter("titles", titles)
                .addQueryParameter("cllimit", "500")
                .addQueryParameter("format", "json").build();
        Request request = new Request.Builder().url(urlRelatedCategoriesBase).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        List<JsonNode> relatedCategories = new ArrayList<>();
        List<List<String>> toCategoriesColumn = new ArrayList<>();
        for (int i = 0; i < fileRecordOriginal.size(); i++) {
            relatedCategories.add(jsonNode.path("query").path("pages").path(fileRecordOriginal.get(i).pageId).path("categories"));
            List<String> categoriesPerFile = new ArrayList<>();
            for (int j = 0; j < relatedCategories.get(i).size(); j++) {
                categoriesPerFile.add(relatedCategories.get(i).get(j).findValue("title").asText());
            }
            toCategoriesColumn.add(categoriesPerFile);
        }

        return toCategoriesColumn;
    }

    /**
     * Returns {@code true} if the iteration has more elements for
     * which to fetch related categories.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements or if there's
     * new file records already fetched from a previous api call
     */
    @Override
    public boolean hasNext() {
        return fileRecordNewIndex < fileRecordNew.size() || iteratorFileRecords.hasNext();
    }

    /**
     * This method iterates over each of the categories related to a file
     * and stores them as a list in the relatedCategories parameter of
     * each file record
     *
     * @return an instance of the FileRecord updated to include its related categories
     */
    @Override
    public FileRecord next() {

        if (fileRecordNewIndex >= fileRecordNew.size() && iteratorFileRecords.hasNext()) {
            List <FileRecord> fileRecordOriginal = new ArrayList<>();
            List<List<String>> toCategoriesColumn = new ArrayList<>();
            String fetchingErrors = "";
            while (iteratorFileRecords.hasNext() && fileRecordOriginal.size() < apiLimit) {
                fileRecordOriginal.add(iteratorFileRecords.next());
            }
            try {
                toCategoriesColumn = getRelatedCategories(fileRecordOriginal);
            } catch (IOException e) {
                fetchingErrors = "Could not fetch related categories: " + e.getMessage();
            }
            for (int i = 0; i < fileRecordOriginal.size(); i++) {
                if (fetchingErrors.isBlank()) {
                    fileRecordNew.add(new FileRecord(fileRecordOriginal.get(i).fileName, fileRecordOriginal.get(i).pageId,
                            toCategoriesColumn.get(i), null));
                } else {
                    fileRecordNew.add(new FileRecord(fileRecordOriginal.get(i).fileName, fileRecordOriginal.get(i).pageId,
                            null, fetchingErrors));
                }
            }
        }
        if (fileRecordNewIndex < fileRecordNew.size()) {
            return fileRecordNew.get(fileRecordNewIndex++);
        } else {
            fileRecordNewIndex = 0;

            return null;
        }
    }

}
