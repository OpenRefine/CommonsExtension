package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.importing.ImportingJob;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class FileFetcherTest {

    /**
     * Test row generation from mocked api calls and paging with a cmcontinue token
     */
    @Test
    public void testNext() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":[{\"pageid\":112928,\"ns\":6,"
                    + "\"title\":\"File:LasTres.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            List<String> category = Collections.singletonList("Category:Costa Rica");
            FileFetcher fileFetcher = new FileFetcher(category.get(0));

            List<Object> rows = new ArrayList<>();
            rows.add(fileFetcher.next());

            Assert.assertEquals(rows.get(0), Arrays.asList("File:LasTres.jpg"));

        }
    }

}
