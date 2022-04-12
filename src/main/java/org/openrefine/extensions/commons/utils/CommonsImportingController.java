package org.openrefine.extensions.commons.utils;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
import com.google.refine.model.Cell;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CommonsImportingController implements ImportingController {
    private static final Logger logger = LoggerFactory.getLogger("CommonsImportingController");
    protected RefineServlet servlet;
    public static int DEFAULT_PREVIEW_LIMIT = 100;

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
                DEFAULT_PREVIEW_LIMIT ,
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

        String url = "https://commons.wikimedia.org./w/api.php"
                + "?action=query&list=categorymembers&cmtitle=Category:art&cmtype=file&cmlimit=5&format=json";
        OkHttpClient client = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        JsonNode jsonNode = new ObjectMapper().readTree(response.body().string());
        JsonNode files = jsonNode.path("query").path("categorymembers");

        parse(
            response,
            project,
            metadata,
            job,
            files,
            limit,
            options,
            exceptions
        );

    }

    static public void parse(
            Response response,
            Project project,
            ProjectMetadata metadata,
            final ImportingJob job,//importing job ID
            JsonNode files,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {

            int pageIndex = 0; // FIXME: we only parse 1 page

            parseOnePage(
                response,
                project,
                metadata,
                job,
                pageIndex,
                files,
                limit,
                options,
                exceptions);
        }

    static public void parseOnePage(
            Response response,
            Project project,
            ProjectMetadata metadata,
            final ImportingJob job,
            int pageIndex,
            JsonNode files,
            int limit,
            ObjectNode options,
            List<Exception> exceptions) {

            String pageId = "Category:art";//pageid?
            String pageName = "art";
            String fileSource = pageName + " # " +
                    files.findValue("title");
            setProgress(job, fileSource, 0);
            TabularImportingParserBase.readTable(
                project,
                metadata,
                job,
                new FilesBatchRowReader(job, fileSource, response, pageId, files/*, worksheetEntry*/),
                fileSource,
                limit,
                options,
                exceptions
            );
            setProgress(job, fileSource, 100);
        }

        static private void setProgress(ImportingJob job, String fileSource, int percent) {
            job.setProgress(percent, "Reading " + fileSource);
        }

        static private class FilesBatchRowReader implements TableDataReader {
            final ImportingJob job;
            final String fileSource;
            final Response response;
            final String pageId;
            JsonNode files;
            private int indexRow = 0;

            public FilesBatchRowReader(ImportingJob job, String fileSource,
                    Response response, String pageId, JsonNode files) {
                this.job = job;
                this.fileSource = fileSource;
                this.response = response;
                this.pageId = pageId;
                this.files = files;
            }

            @Override
            public List<Object> getNextRowOfCells() throws IOException {
                List<Object> rowsOfCells;

                if (files != null) {
                    String row = files.get(indexRow).findValue("title").asText();
                    rowsOfCells = Collections.singletonList(row);
                    System.out.println("\n1: " + rowsOfCells + "\n");
                    indexRow++;
                    return rowsOfCells;
                } else if (files == null) {
                    return null;
                }

                if (files.size() > 0) {
                    setProgress(job, fileSource, 100 * indexRow / files.size());
                } else if (indexRow == files.size()) {
                    setProgress(job, fileSource, 100);
                }

                if (indexRow < files.size()) {
                    return Collections.singletonList(files.get(indexRow++).findValue("title").asText());
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
                        parsePreview(
                            project,
                            pm,
                            job,
                            DEFAULT_PREVIEW_LIMIT ,
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
