package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterators;
import com.google.refine.ProjectMetadata;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importing.ImportingJob;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.ReconStats;
import com.google.refine.model.recon.StandardReconConfig;
import com.google.refine.model.recon.StandardReconConfig.ColumnDetail;
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

        List<CategoryWithDepth> categoriesWithDepth = new ArrayList<>();
        Iterator<FileRecord> rcf;
        Iterator<FileRecord> fetchedFiles = Collections.emptyIterator();
        JSONUtilities.safePut(options, "headerLines", 0);
        /* get user-input from the Post request parameters */
        JsonNode categoryInput = options.get("categoryJsonValue");
        boolean mIdsColumn = options.get("mIdsColumn").asBoolean();
        boolean categoriesColumn = options.get("categoriesColumn").asBoolean();
        for (JsonNode category: categoryInput) {
            categoriesWithDepth.add(new CategoryWithDepth(category.get("category").asText(),
                    category.get("depth").asInt()));
        }
        String apiUrl = "https://commons.wikimedia.org/w/api.php";//FIXME
        String service = "https://commonsreconcile.toolforge.org/en/api";

        // initializes progress reporting with the name of the first category
        setProgress(job, categoriesWithDepth.get(0).categoryName, 0);

        for(CategoryWithDepth categoryWithDepth: categoriesWithDepth) {
            fetchedFiles = Iterators.concat(fetchedFiles,
                    FileFetcher.listCategoryMembers(apiUrl, categoryWithDepth.categoryName, categoryWithDepth.depth));
        }
        if (categoriesColumn) {
            rcf = new RelatedCategoryFetcher(apiUrl, fetchedFiles);
        } else {
            rcf = fetchedFiles;
        }

        TabularImportingParserBase.readTable(
                project,
                job,
                new FileRecordToRows(rcf, categoriesColumn, mIdsColumn),
                limit,
                options,
                exceptions
        );

        Column col = project.columnModel.columns.get(0);
        StandardReconConfig cfg = new StandardReconConfig(
                service,
                "https://commons.wikimedia.org/entity/",
                "http://www.wikidata.org/prop/direct/",
                "",
                "entity",
                true,
                new ArrayList<ColumnDetail>(),
                1);
        col.setReconStats(ReconStats.create(project, 0));
        col.setReconConfig(cfg);

        setProgress(job, categoriesWithDepth.get(categoriesWithDepth.size()-1).categoryName, 100);
    }

    static private void setProgress(ImportingJob job, String category, int percent) {
        job.setProgress(percent, "Reading " + category);
    }

}
