package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

public class FileFetcherTest {

    /**
     * Test api parameters and row generation from mocked api calls and paging with a cmcontinue token
     */
    @Test
    public void testNext() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponseContinue = "{\"batchcomplete\":\"\",\"continue\":{\"cmcontinue\":"
                    + "\"file|4492e4a5047|13935\",\"continue\":\"-||\"},\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":127722,\"ns\":6,\"title\":\"File:3 Puppies.jpg\",\"type\":\"file\"},"
                    + "{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseContinue));
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":112928,\"ns\":6,\"title\":\"File:Esferas de CR.jpg\",\"type\":\"file\"},"
                    + "{\"pageid\":112933,\"ns\":6,\"title\":\"File:Playa Gandoca.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            List<String> category = Collections.singletonList("Category:Costa Rica");
            FileFetcher fileFetcher = new FileFetcher(url.toString(), category.get(0), false);

            List<JsonNode> rows = new ArrayList<>();
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertFalse(fileFetcher.hasNext());
            String file0 = "{\"pageid\":127722,\"ns\":6,\"title\":\"File:3 Puppies.jpg\",\"type\":\"file\"}";
            String file1 = "{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\",\"type\":\"file\"}";
            String file2 = "{\"pageid\":112928,\"ns\":6,\"title\":\"File:Esferas de CR.jpg\",\"type\":\"file\"}";
            String file3 = "{\"pageid\":112933,\"ns\":6,\"title\":\"File:Playa Gandoca.jpg\",\"type\":\"file\"}";
            RecordedRequest recordedRequest = server.takeRequest();
            RecordedRequest recordedRequestContinue = server.takeRequest();
            String queryParameters = "/w/api.php?action=query&list=categorymembers&"
                    + "cmtitle=Category%3ACosta%20Rica&cmtype=file&cmprop=title%7Ctype%7Cids&cmlimit=500&format=json";
            String queryParametersContinue = "/w/api.php?action=query&list=categorymembers&"
                    + "cmtitle=Category%3ACosta%20Rica&cmtype=file&cmprop=title%7Ctype%7Cids&cmlimit=500&format=json"
                    + "&cmcontinue=file%7C4492e4a5047%7C13935";

            Assert.assertEquals(recordedRequest.getPath(), queryParameters);
            Assert.assertEquals(recordedRequestContinue.getPath(), queryParametersContinue);
            Assert.assertEquals(rows.get(0).toString(), file0);
            Assert.assertEquals(rows.get(1).toString(), file1);
            Assert.assertEquals(rows.get(2).toString(), file2);
            Assert.assertEquals(rows.get(3).toString(), file3);

            server.close();
        }
    }

    /**
     * Test row generation from multiple api calls for recursive depth fetching
     */
    @Test
    public void testListCategoryMembersDepth() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":112928,\"ns\":6,\"title\":\"File:Esferas de CR.jpg\",\"type\":\"file\"},"
                    + "{\"pageid\":112933,\"ns\":6,\"title\":\"File:Playa Gandoca.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            String jsonResponseSubcategories = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":12772,\"ns\":14,\"title\":\"Category:Costa Rican dogs\",\"type\":\"subcat\"},"
                    + "{\"pageid\":12775,\"ns\":14,\"title\":\"Category:Costa Rican food\",\"type\":\"subcat\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseSubcategories));
            String jsonResponseFilesSubcat1 = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":127722,\"ns\":6,\"title\":\"File:3 Puppies.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseFilesSubcat1));
            String jsonResponseFilesSubcat2 = "{\"batchcomplete\":\"\",\"query\":{\"categorymembers\":"
                    + "[{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\",\"type\":\"file\"}]}}";
            server.enqueue(new MockResponse().setBody(jsonResponseFilesSubcat2));
            List<String> category = Collections.singletonList("Category:Costa Rica");
            Iterator<FileRecord> fileFetcher = FileFetcher.listCategoryMembers(url.toString(), category.get(0), 1);

            List<FileRecord> rows = new ArrayList<>();
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertTrue(fileFetcher.hasNext());
            rows.add(fileFetcher.next());
            Assert.assertFalse(fileFetcher.hasNext());
            FileRecord file0 = new FileRecord("File:Esferas de CR.jpg", "112928", null, null);
            FileRecord file1 = new FileRecord("File:Playa Gandoca.jpg", "112933", null, null);
            FileRecord file2 = new FileRecord("File:3 Puppies.jpg", "127722", null, null);
            FileRecord file3 = new FileRecord("File:Pejiballes.jpg", "127752", null, null);

            Assert.assertEquals(rows.get(0), file0);
            Assert.assertEquals(rows.get(1), file1);
            Assert.assertEquals(rows.get(2), file2);
            Assert.assertEquals(rows.get(3), file3);

            server.close();
        }
    }

}
