package org.openrefine.extensions.commons.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openrefine.extensions.commons.utils.CommonsImportingController.FilesBatchRowReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.RefineServlet;
import com.google.refine.importing.ImportingJob;
import com.google.refine.importing.ImportingManager;
import com.google.refine.io.FileProjectManager;
import com.google.refine.model.Column;
import com.google.refine.model.ModelException;
import com.google.refine.model.Project;
import com.google.refine.operations.OperationRegistry;
import com.google.refine.operations.column.ColumnAdditionByFetchingURLsOperation;
import com.google.refine.util.ParsingUtilities;

import edu.mit.simile.butterfly.ButterflyModule;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class CommonsImportingControllerTest {

    static final String ENGINE_JSON_URLS = "{\"mode\":\"row-based\"}";

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    protected Logger logger;

    protected RefineServlet servlet;

    // dependencies
    private Project project;
    private ProjectMetadata metadata;
    private ImportingJob job;

    // System under test
    private CommonsImportingController SUT = null;

    public static File createTempDirectory(String name)
            throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }

    protected ButterflyModule getCoreModule() {
        ButterflyModule coreModule = mock(ButterflyModule.class);
        when(coreModule.getName()).thenReturn("core");
        return coreModule;
    }

    @BeforeTest
    public void init() {
        logger = LoggerFactory.getLogger(this.getClass());
        OperationRegistry.registerOperation(getCoreModule(), "column-addition-by-fetching-urls",
                ColumnAdditionByFetchingURLsOperation.class);
    }

    protected Project createProjectWithColumns(String projectName, String... columnNames) throws IOException, ModelException {
        servlet = new RefineServletStub();
        ProjectManager.singleton = new ProjectManagerStub();
        ImportingManager.initialize(servlet);
        Project project = new Project();
        ProjectMetadata pm = new ProjectMetadata();
        pm.setName(projectName);
        ProjectManager.singleton.registerProject(project, pm);

        if (columnNames != null) {
            for (String columnName : columnNames) {
                int index = project.columnModel.allocateNewCellIndex();
                Column column = new Column(index, columnName);
                project.columnModel.addColumn(index, column, true);
            }
        }
        return project;
    }

    @BeforeMethod
    public void setUp() throws IOException, ModelException {

        MockitoAnnotations.initMocks(this);

        File dir = createTempDirectory("OR_CommonsExtension_Test_WorkspaceDir");
        FileProjectManager.initialize(dir);

        servlet = new RefineServlet();
        ImportingManager.initialize(servlet);
        project = new Project();
        metadata = new ProjectMetadata();

        metadata.setName("Commons Import Test Project");
        ProjectManager.singleton.registerProject(project, metadata);
        SUT = new CommonsImportingController();

    }

    @AfterMethod
    public void tearDown() {
        SUT = null;
        request = null;
        response = null;
        project = null;
        metadata = null;
        job = null;
    }

    @Test
    public void testDoGet() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        try {
            when(response.getWriter()).thenReturn(pw);

            SUT.doGet(request, response);
            System.out.print(sw +"1\n");

            String result = sw.getBuffer().toString().trim();
            System.out.print(result +"2\n");
            System.out.print(pw +"3\n");
            ObjectNode json = ParsingUtilities.mapper.readValue(result, ObjectNode.class);
            String code = json.get("status").asText();
            String message = json.get("message").asText();
            Assert.assertNotNull(code);
            Assert.assertNotNull(message);
            Assert.assertEquals(code, "error");
            Assert.assertEquals(message, "GET not implemented");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test row generation from mocked api calls and paging with a cmcontinue token
     */
    @Test
    public void testgetNextRowOfCells() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"continue\":{\"cmcontinue\":\"file|4492e4a5047|13935\",\"continue\":\"-||\"},"
                    + "\"query\":{\"categorymembers\":[{\"pageid\":127722,\"ns\":6,"
                    + "\"title\":\"File:Museo Nacional de Costa Rica Esfera.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            String jsonResponseContinue = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":[{\"pageid\":112928,\"ns\":6,"
                    + "\"title\":\"File:LasTres.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseContinue));
            /* create job.mock */
            job = Mockito.mock(ImportingJob.class);
            List<String> category = Collections.singletonList("Category:art");
            FilesBatchRowReader reader = new FilesBatchRowReader(job, category, url.toString());

            List<Object> currentRow = null;
            List<List<Object>> rows = new ArrayList<>();
            while ((currentRow = reader.getNextRowOfCells()) != null) {
                rows.add(currentRow);
            }

            Assert.assertEquals(rows.get(0), Arrays.asList("File:Museo Nacional de Costa Rica Esfera.jpg"));
            Assert.assertEquals(rows.get(1), Arrays.asList("File:LasTres.jpg"));

        }
    }

}
