package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

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
            String jsonResponseContinue = "{\"batchcomplete\":\"\",\"continue\":{\"cmcontinue\":"
                    + "\"file|4492e4a5047|13935\",\"continue\":\"-||\"},\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":127722,\"ns\":6,\"title\":\"File:LasTres.jpg\",\"type\":\"file\"},"
                    + "{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseContinue));
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":112928,\"ns\":6,\"title\":\"File:Esferas de CR.jpg\",\"type\":\"file\"},"
                    + "{\"pageid\":112933,\"ns\":6,\"title\":\"File:Playa Gandoca.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            List<String> category = Collections.singletonList("Category:Costa Rica");
            FileFetcher fileFetcher = new FileFetcher(url.toString(), category.get(0));

            List<Object> rows = new ArrayList<>();
            rows.add(fileFetcher.next());
            rows.add(fileFetcher.next());
            rows.add(fileFetcher.next());
            rows.add(fileFetcher.next());
            FileRecord file0 = new FileRecord("File:LasTres.jpg", "M127722", null);
            FileRecord file1 = new FileRecord("File:Pejiballes.jpg", "M127752", null);
            FileRecord file2 = new FileRecord("File:Esferas de CR.jpg", "M112928", null);
            FileRecord file3 = new FileRecord("File:Playa Gandoca.jpg", "M112933", null);

            Assert.assertEquals(rows.get(0), file0);
            Assert.assertEquals(rows.get(1), file1);
            Assert.assertEquals(rows.get(2), file2);
            Assert.assertEquals(rows.get(3), file3);

        }
    }

}
