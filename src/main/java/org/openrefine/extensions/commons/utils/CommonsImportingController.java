package org.openrefine.extensions.commons.utils;

import java.io.IOException;
import java.io.Writer;
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
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
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

        /* Stub for upcoming parsing options preview */

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
        String cmtitle = JSONUtilities.getString(options, "categoryInput", null);
        if (!cmtitle.startsWith("Category:")) {
            cmtitle = "Category:" + cmtitle;
        }
        String pageName = cmtitle;
        String fileSource = pageName;//FIXME: add filename
        String apiUrl = "https://commons.wikimedia.org/w/api.php";

        setProgress(job, fileSource, 0);

        TabularImportingParserBase.readTable(
                project,
                job,
                new FilesBatchRowReader(job, fileSource, cmtitle, apiUrl),
                limit,
                options,
                exceptions
        );
        setProgress(job, fileSource, 100);
    }

        static private void setProgress(ImportingJob job, String fileSource, int percent) {
            job.setProgress(percent, "Reading " + fileSource);
        }

        static protected class FilesBatchRowReader implements TableDataReader {
            final ImportingJob job;
            final String fileSource;
            String apiUrl;
            HttpUrl urlBase;
            HttpUrl urlContinue;
            JsonNode files;
            String cmtitle;
            String cmcontinue;
            private int indexRow = 0;
            List<Object> rowsOfCells;

            public FilesBatchRowReader(ImportingJob job, String fileSource, String cmtitle, String apiUrl) throws IOException {

                this.job = job;
                this.fileSource = fileSource;
                this.cmtitle = cmtitle;
                this.apiUrl = apiUrl;
                getFiles();

            }

            public void getFiles() throws IOException {

                OkHttpClient client = new OkHttpClient.Builder().build();
                urlBase = HttpUrl.parse(apiUrl).newBuilder()
                        .addQueryParameter("action", "query")
                        .addQueryParameter("list", "categorymembers")
                        .addQueryParameter("cmtitle", cmtitle)
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

            @Override
            public List<Object> getNextRowOfCells() throws IOException {

                if (files.size() > 0) {
                    setProgress(job, fileSource, 100 * indexRow / files.size());
                } else if (indexRow == files.size()) {
                    setProgress(job, fileSource, 100);
                }

                if (indexRow == files.size() && !cmcontinue.isBlank()) {
                    urlContinue = HttpUrl.parse(urlBase.toString()).newBuilder()
                            .addQueryParameter("cmcontinue", cmcontinue).build();
                    getFiles(urlContinue);
                    indexRow = 0;
                }

                if (indexRow < files.size()) {
                    rowsOfCells = Collections.singletonList(files.get(indexRow++).findValue("title").asText());

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
