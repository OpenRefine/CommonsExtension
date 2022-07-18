package org.openrefine.extensions.commons.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.commands.HttpUtilities;
import com.google.refine.importers.TabularImportingParserBase;
import com.google.refine.importers.TabularImportingParserBase.TableDataReader;
import com.google.refine.importing.ImportingController;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.model.Project;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonsImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("CommonsImportingController");
    protected RefineServlet servlet;
    public static int DEFAULT_PREVIEW_LIMIT = 50;
    public static int DEFAULT_PROJECT_LIMIT = 0;

    @Override
    public void init(RefineServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        HttpUtilities.respond(response, "error", "GET not implemented");
    }

    /* Handling of http requests between frontend and OpenRefine servlet */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        if(logger.isDebugEnabled()){
            logger.debug("doPost Query String::{}", request.getQueryString());
        }
        response.setCharacterEncoding("UTF-8");
        Properties parameters = ParsingUtilities.parseUrlParameters(request);

        String subCommand = parameters.getProperty("subCommand");

        if(logger.isDebugEnabled()){
            logger.info("doPost::subCommand::{}", subCommand);
        }

        if ("initialize-parser-ui".equals(subCommand)) {
            doInitializeParserUI(request, response, parameters);
        } else if ("parse-preview".equals(subCommand)) {
            try {

                doParsePreview(request, response, parameters);

            } catch (Exception e) {
                logger.error("doPost::DatabaseServiceException::{}", e);
                HttpUtilities.respond(response, "error", "Unable to parse preview");
            }
        } else if ("create-project".equals(subCommand)) {
            doCreateProject(request, response, parameters);
        } else {
            HttpUtilities.respond(response, "error", "No such sub command");
        }

    }

    /**
     * 
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     */
    private void doInitializeParserUI(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {
        if(logger.isDebugEnabled()) {
            logger.debug("::doInitializeParserUI::");
        }

        ObjectNode result = ParsingUtilities.mapper.createObjectNode();
        ObjectNode options = ParsingUtilities.mapper.createObjectNode();
        JSONUtilities.safePut(result, "status", "ok");
        JSONUtilities.safePut(result, "options", options);

        JSONUtilities.safePut(options, "skipDataLines", 0); 
        if(logger.isDebugEnabled()) {
            logger.debug("doInitializeParserUI:::{}", result.toString());
        }

        HttpUtilities.respond(response, result.toString());

    }

    /**
     * doParsePreview
     * @param request
     * @param response
     * @param parameters
     * @throws ServletException
     * @throws IOException
     * @throws DatabaseServiceException 
     */
    private void doParsePreview(
            HttpServletRequest request, HttpServletResponse response, Properties parameters)
                throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
            request.getParameter("options"));

        List<Exception> exceptions = new LinkedList<Exception>();

        job.prepareNewProject();

        parsePreview(
                job.project,
                job.metadata,
                job,
                DEFAULT_PREVIEW_LIMIT,
                optionObj,
                exceptions
        );

        Writer w = response.getWriter();
        JsonGenerator writer = ParsingUtilities.mapper.getFactory().createGenerator(w);
        try {
            writer.writeStartObject();
            if (exceptions.size() == 0) {
                job.project.update(); // update all internal models, indexes, caches, etc.

                writer.writeStringField("status", "ok");
            } else {
                writer.writeStringField("status", "error");

                writer.writeArrayFieldStart("errors");
                writer.writeEndArray();
            }
            writer.writeEndObject();
        } catch (IOException e) {
            throw new ServletException(e);
        } finally {
            writer.flush();
            writer.close();
            w.flush();
            w.close();
        }

        job.touch();
        job.updating = false;
    }

    private static void parsePreview(
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
                DEFAULT_PREVIEW_LIMIT ,
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
                urlBase = HttpUrl.parse(apiUrl).newBuilder()
                        .addQueryParameter("action", "query")
                        .addQueryParameter("prop", "categories")
                        .addQueryParameter("titles", file)
                        .addQueryParameter("format", "json").build();
                Request request = new Request.Builder().url(urlBase).build();
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

    private void doCreateProject(HttpServletRequest request, HttpServletResponse response, Properties parameters)
            throws ServletException, IOException {

        long jobID = Long.parseLong(parameters.getProperty("jobID"));
        final ImportingJob job = ImportingManager.getJob(jobID);
        if (job == null) {
            HttpUtilities.respond(response, "error", "No such import job");
            return;
        }

        job.updating = true;
        final ObjectNode optionObj = ParsingUtilities.evaluateJsonStringToObjectNode(
            request.getParameter("options"));

        final List<Exception> exceptions = new LinkedList<Exception>();

        job.setState("creating-project");

        final Project project = new Project();
        new Thread() {
            @Override
            public void run() {
                ProjectMetadata pm = new ProjectMetadata();
                pm.setName(JSONUtilities.getString(optionObj, "projectName", "Untitled"));
                pm.setEncoding(JSONUtilities.getString(optionObj, "encoding", "UTF-8"));

                try {
                    parse(
                            project,
                            pm,
                            job,
                            DEFAULT_PROJECT_LIMIT ,
                            optionObj,
                            exceptions
                    );
                } catch (IOException e) {
                    logger.error(ExceptionUtils.getStackTrace(e));
                }

                if (!job.canceled) {
                    if (exceptions.size() > 0) {
                        job.setError(exceptions);
                    } else {
                        project.update(); // update all internal models, indexes, caches, etc.

                        ProjectManager.singleton.registerProject(project, pm);

                        job.setState("created-project");
                        job.setProjectID(project.id);
                    }

                    job.touch();
                    job.updating = false;
                }
            }
        }.start();

        HttpUtilities.respond(response, "ok", "done");
    }
}
