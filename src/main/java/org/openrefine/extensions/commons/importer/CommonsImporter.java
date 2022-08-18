package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;

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

        FileFetcher fileFetcher;
        RelatedCategoryFetcher rcf;
        JSONUtilities.safePut(options, "headerLines", 0);
        /* get user-input from the Post request parameters */
        JsonNode categoryInput = options.get("categoryJsonValue");
        boolean mIdsColumn = options.get("mIdsColumn").asBoolean();
        boolean categoriesColumn = options.get("categoriesColumn").asBoolean();
        List<String> categories = new ArrayList<>();
        for (JsonNode category: categoryInput) {
            categories.add(category.get("category").asText());
        }
        String apiUrl = "https://commons.wikimedia.org/w/api.php";//FIXME

        // initializes progress reporting with the name of the first category
        setProgress(job, categories.get(0), 0);

        fileFetcher = new FileFetcher(apiUrl, categories.get(0));
        List<FileRecord> fileRecords = new ArrayList<>();
        if (fileFetcher.hasNext()) {
            fileRecords.add(fileFetcher.next());
        }
        rcf = new RelatedCategoryFetcher(apiUrl, fileRecords.iterator());
        List<FileRecord> filesWithCategories = new ArrayList<>();
        if (rcf.hasNext()) {
            filesWithCategories.add(rcf.next());
        }

        TabularImportingParserBase.readTable(
                project,
                job,
                new FileRecordToRows(fileFetcher, categoriesColumn, mIdsColumn),
                limit,
                options,
                exceptions
        );
        setProgress(job, categories.get(categories.size()-1), 100);
    }

    static private void setProgress(ImportingJob job, String category, int percent) {
        job.setProgress(percent, "Reading " + category);
    }

}
