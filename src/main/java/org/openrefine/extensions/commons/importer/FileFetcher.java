package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class iterates over the members contained in a given category
 */
public class FileFetcher implements Iterator<JsonNode>{
    String apiUrl;
    String categoryName;
    String cmType;
    JsonNode callResults;
    private int indexRow = 0;
    String cmcontinue;

    public FileFetcher(String apiUrl, String categoryName, boolean subcategories) {
        this.apiUrl = apiUrl;
        this.categoryName = categoryName;
        this.cmType = subcategories ? "subcat" : "file";
        try {
            // TODO: it's weird that this uses categoryName as a parameter, but subcategories implicitly from the field
            getCallResults(categoryName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * API call for fetching files from a given category
     * @param category
     */
    public void getCallResults(String category) throws IOException {

        HttpUrl urlBase = buildBaseUrl(category, cmType);
        JsonNode jsonNode = getJson(urlBase);
        callResults = jsonNode.path("query").path("categorymembers");
        cmcontinue = jsonNode.path("continue").path("cmcontinue").asText();

    }

    private HttpUrl buildBaseUrl(String category, String cmtype) {
        return HttpUrl.parse(apiUrl).newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("list", "categorymembers")
                .addQueryParameter("cmtitle", category)
                .addQueryParameter("cmtype", cmtype)
                .addQueryParameter("cmprop", "title|type|ids")
                .addQueryParameter("cmlimit", "500")
                .addQueryParameter("format", "json").build();
    }

    /**
     * API call when a cmcontinue token is part of the response
     * @param urlContinue: URL containing the cmcontinue token
     */
    private void getContinuationResults(HttpUrl urlContinue) throws IOException {

        JsonNode jsonNode = getJson(urlContinue);
        callResults = jsonNode.path("query").path("categorymembers");
        cmcontinue = jsonNode.path("continue").path("cmcontinue").asText();

    }

    private JsonNode getJson(HttpUrl url) throws IOException {

        Request request = new Request.Builder().url(url).build();
        try (Response response = HttpClient.getClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (response.body() != null) {
                    return new ObjectMapper().readTree(response.body().string());
                } else  {
                    return new ObjectMapper().readTree("[]");
                }
            } else {
                throw new IOException("API request failed with status code: " + response.code() + ", body: " + response.message());
            }
        }

    }

    /**
     * Internal function used to iterate over the paginated results of the MediaWiki API
     * when fetching files or categories.
     * @param endpoint
     * @param categoryName: category to fetch from
     * @param subcategories: set to true to fetch categories and false to fetch files
     *
     * @return an Iterator of JsonNodes with the paginated results of the API call
     */
    static Iterator<JsonNode> fetchCategoryMembers(String endpoint, String categoryName, boolean subcategories) {
        return new FileFetcher(endpoint, categoryName, subcategories);
     }

    /**
     * Fetches the files which are direct members of a given category, from the MediaWiki API.
     * @param endpoint
     * @param categoryName: category to fetch from
     *
     * @return an Iterator of FileRecords with the direct file members of the category
     */
    static Iterator<FileRecord> fetchDirectFileMembers(String endpoint, String categoryName) {
        Iterator<JsonNode> fetchedCategoryMembers = fetchCategoryMembers(endpoint, categoryName, false);
        return Iterators.transform(fetchedCategoryMembers, jsonNode->
            new FileRecord(jsonNode.findValue("title").asText(), jsonNode.findValue("pageid").asText(), null, null));
     }

    /**
     * Fetches the direct subcategories of a given category, from the MediaWiki API.
     * @param endpoint
     * @param categoryName: category to fetch from
     *
     * @return an Iterator of String with the category's subcategories
     */
    static Iterator<String> fetchSubcategories(String endpoint, String categoryName) {
        Iterator<JsonNode> fetchedCategoryMembers = fetchCategoryMembers(endpoint, categoryName, true);
        return Iterators.transform(fetchedCategoryMembers, jsonNode->
            jsonNode.findValue("title").asText());
    }

    /**
     * Fetches a category recursively, up to the given depth, from the MediaWiki API.
     * @param endpoint
     * @param categoryName: category to fetch from
     * @param depth: set to 0 to ignore subcategories
     *
     * @return an Iterator of FileRecords with both the direct file members of the category and,
     * if specified, the file members of the category's subcategories up to the given depth
     */
    static Iterator<FileRecord> listCategoryMembers(String endpoint, String categoryName, int depth) {
        Iterator<FileRecord> fetchedDirectFileMembers = fetchDirectFileMembers(endpoint, categoryName);// depth 0
        if (depth > 0) {
            Iterator<String> fetchedSubcategories = fetchSubcategories(endpoint, categoryName);
            Iterator<Iterator<FileRecord>> listedCategoryMembers = Iterators.transform(fetchedSubcategories, cat->
                listCategoryMembers(endpoint, cat, depth-1));
            return Iterators.concat(fetchedDirectFileMembers, Iterators.concat(listedCategoryMembers));
        } else {
            return fetchedDirectFileMembers;
        }
    }

    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    @Override
    public boolean hasNext() {

        return (indexRow < callResults.size());
    }

    /**
     * This method iterates over each of the fetched files and populates a FileRecord
     * with a single entry's filename and MID
     *
     * @return an instance of the FileRecord
     */
    @Override
    public JsonNode next() {

        JsonNode file = callResults.get(indexRow);
        indexRow++;

        if ((indexRow == callResults.size()) && !cmcontinue.isBlank()) {
            HttpUrl urlContinue = buildBaseUrl(categoryName, cmType).newBuilder()
                    .addQueryParameter("cmcontinue", cmcontinue).build();
            try {
                getContinuationResults(urlContinue);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            indexRow = 0;
        }
        return file;

    }

}
