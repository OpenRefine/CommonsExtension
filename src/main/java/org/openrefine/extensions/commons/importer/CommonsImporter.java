package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonsImporter {
    static public void parsePreview(
            Project project,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws IOException {

        parse(
                project,
                metadata,
                job,
                limit,
                options,
                exceptions
        );

    }

    static public void parse(
            Project project,
            ProjectMetadata metadata,
            final ImportingJob job,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) throws IOException {

        JSONUtilities.safePut(options, "headerLines", 0);
        /* get user-input from the Post request parameters */
        JsonNode categoryInput = options.get("categoryJsonValue");
        String mIdsColumn = options.get("mIdsColumn").asText();
        String categoriesColumn = options.get("categoriesColumn").asText();
        List<String> categories = new ArrayList<>();
        for (JsonNode category: categoryInput) {
            categories.add(category.get("category").asText());
        }
        String apiUrl = "https://commons.wikimedia.org/w/api.php";//FIXME

        // initializes progress reporting with the name of the first category
        setProgress(job, categories.get(0), 0);

        TabularImportingParserBase.readTable(
                project,
                job,
                new FilesBatchRowReader(job, categories, categoriesColumn, mIdsColumn, apiUrl),
                limit,
                options,
                exceptions
        );
        setProgress(job, categories.get(categories.size()-1), 100);
    }

    static private void setProgress(ImportingJob job, String category, int percent) {
        job.setProgress(percent, "Reading " + category);
    }

    static protected class FilesBatchRowReader implements TableDataReader {
        final ImportingJob job;
        String apiUrl;
        HttpUrl urlBase;
        HttpUrl urlContinue;
        HttpUrl urlRelatedCategories;
        JsonNode files;
        List<String> categories;
        String categoriesColumn;
        String category;
        String cmcontinue;
        String file;
        String pageId;
        JsonNode relatedCategories;
        List<JsonNode> toCategoriesColumn;
        String mIdsColumn;
        private int indexRow = 0;
        private int indexCategories = 1;
        List<Object> rowsOfCells;

        public FilesBatchRowReader(ImportingJob job, List<String> categories,
                String categoriesColumn, String mIdsColumn, String apiUrl) throws IOException {

            this.job = job;
            this.categories = categories;
            this.categoriesColumn = categoriesColumn;
            this.mIdsColumn = mIdsColumn;
            this.apiUrl = apiUrl;
            getFiles(categories.get(0));

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

        public void getFiles(HttpUrl urlContinue) throws IOException {

            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(urlContinue).build();
            Response response = client.newCall(request).execute();
            JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
            files = jsonNode.path("query").path("categorymembers");
            cmcontinue = jsonNode.path("continue").path("cmcontinue").asText();

        }

        public String getCategoriesColumn(String file) throws IOException {

            OkHttpClient client = new OkHttpClient.Builder().build();
            urlRelatedCategories = HttpUrl.parse(apiUrl).newBuilder()
                    .addQueryParameter("action", "query")
                    .addQueryParameter("prop", "categories")
                    .addQueryParameter("titles", file)
                    .addQueryParameter("format", "json").build();
            Request request = new Request.Builder().url(urlRelatedCategories).build();
            Response response = client.newCall(request).execute();
            JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
            pageId = files.get(indexRow).findValue("pageid").asText();
            relatedCategories = jsonNode.path("query").path("pages").path(pageId).path("categories");
            toCategoriesColumn = relatedCategories.findValues("title");

            return toCategoriesColumn.get(0).asText();
        }

        @Override
        public List<Object> getNextRowOfCells() throws IOException {

            for (int i = 1; i < categories.size(); i++) {
                if (files.size() > 0) {
                    setProgress(job, categories.get(i), 100 * indexRow / files.size());
                } else if (indexRow == files.size()) {
                    setProgress(job, categories.get(i), 100);
                }
            }

            if ((indexRow == files.size()) && indexCategories < categories.size()) {
                if (cmcontinue.isBlank()) {
                    getFiles(categories.get(indexCategories++));
                } else {
                    urlContinue = HttpUrl.parse(urlBase.toString()).newBuilder()
                            .addQueryParameter("cmcontinue", cmcontinue).build();
                    getFiles(urlContinue);
                }
                indexRow = 0;
            }

            if ((indexRow == files.size()) && indexCategories == categories.size()) {
                if (!cmcontinue.isBlank()) {
                    urlContinue = HttpUrl.parse(urlBase.toString()).newBuilder()
                            .addQueryParameter("cmcontinue", cmcontinue).build();
                    getFiles(urlContinue);
                    indexRow = 0;
                }
            }

            if (indexRow < files.size()) {
                rowsOfCells = new ArrayList<>();
                rowsOfCells.add(files.get(indexRow).findValue("title").asText());

                if ((categoriesColumn.contentEquals("true")) && (mIdsColumn.contentEquals("true"))) {
                    rowsOfCells.add(getCategoriesColumn(files.get(indexRow).findValue("title").asText()));
                    rowsOfCells.add("M" + files.get(indexRow).findValue("pageid").asText());

                } else if (categoriesColumn.contentEquals("true")) {
                    //FIXME
                    rowsOfCells.add(getCategoriesColumn(files.get(indexRow).findValue("title").asText()));

                } else if (mIdsColumn.contentEquals("true")) {
                    rowsOfCells.add("M" + files.get(indexRow).findValue("pageid").asText());

                }
                indexRow++;

                return rowsOfCells;

            } else {
                return null;
            }

        }

    }

}
