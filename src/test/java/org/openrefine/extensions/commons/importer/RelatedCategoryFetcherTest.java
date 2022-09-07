package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class RelatedCategoryFetcherTest {

    /*
     * Test list generation of categories related to a group of titles
     */
    @Test
    public void testGetRelatedCategories() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127722\":"
                    + "{\"pageid\":127722,\"ns\":6,\"title\":\"File:LasTres.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]},"
                    + "\"127752\":{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Yummy food\"},{\"ns\":14,\"title\":\"Category:Costa Rican dishes\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            FileRecord fr0 = new FileRecord("File:LasTres.jpg", "127722", null, null);
            FileRecord fr1 = new FileRecord("File:Pejiballes.jpg", "127752", null, null);
            List<FileRecord> originalRecords = Arrays.asList(fr0, fr1);
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), originalRecords.iterator());

            List<Object> rows = new ArrayList<>();
            rows.add(rcf.getRelatedCategories(originalRecords));
            List<List<String>> categories = Arrays.asList(Arrays.asList("Category:Costa Rica", "Category:Cute dogs", "Category:Costa Rican dogs"),
                    Arrays.asList("Category:Costa Rica", "Category:Yummy food", "Category:Costa Rican dishes"));

            Assert.assertEquals(rows.get(0), categories);

        }

    }

    /**
     * Test list generation of categories related to given files over multiple api calls
     */
    @Test
    public void testNext() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse1 = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127722\":"
                    + "{\"pageid\":127722,\"ns\":6,\"title\":\"File:LasTres.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponse1));
            String jsonResponse2 = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127752\":"
                    + "{\"pageid\":127752,\"ns\":6,\"title\":\"File:Pejiballes.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Yummy food\"},{\"ns\":14,\"title\":\"Category:Costa Rican dishes\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponse2));
            FileRecord fr0 = new FileRecord("File:LasTres.jpg", "127722", null, null);
            FileRecord fr1 = new FileRecord("File:Pejiballes.jpg", "127752", null, null);
            List<FileRecord> originalRecords = Arrays.asList(fr0, fr1);
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), originalRecords.iterator());
            rcf.setApiLimit(1);

            List<Object> rows = new ArrayList<>();
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
            Assert.assertFalse(rcf.hasNext());
            List<String> categoriesFile0 = Arrays.asList("Category:Costa Rica", "Category:Cute dogs", "Category:Costa Rican dogs");
            List<String> categoriesFile1 = Arrays.asList("Category:Costa Rica", "Category:Yummy food", "Category:Costa Rican dishes");
            FileRecord file0 = new FileRecord("File:LasTres.jpg", "127722", categoriesFile0, null);
            FileRecord file1 = new FileRecord("File:Pejiballes.jpg", "127752", categoriesFile1, null);

            Assert.assertEquals(rows.get(0), file0);
            Assert.assertEquals(rows.get(1), file1);
            server.close();

        }
    }

    /**
     * Test error message generation when next() encounters an IO error during categories fetching
     */
    @Test
    public void testNextError() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponse = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127723\":"
                    + "{\"pageid\":127723,\"ns\":6,\"title\":\"File:LasTres_2.jpg\","
                    + "{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponse));
            String jsonResponse2 = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127722\":"
                    + "{\"pageid\":127722,\"ns\":6,\"title\":\"File:LasTres.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponse2));
            List<FileRecord> originalRecords = new ArrayList<>();
            FileRecord fr0 = new FileRecord("File:LasTres_2.jpg", "127723", null, null);
            FileRecord fr1 = new FileRecord("File:LasTres.jpg", "127722", null, null);
            originalRecords.add(fr0);
            originalRecords.add(fr1);
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), originalRecords.iterator());
            rcf.setApiLimit(1);

            List<Object> rows = new ArrayList<>();
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
            Assert.assertFalse(rcf.hasNext());
            String error = "Could not fetch related categories: Unexpected character ('{' (code 123)): was expecting double-quote to start field name"
                    + "\n at [Source: (String)\"{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"127723\":{\"pageid\":127723,\"ns\":6,"
                    + "\"title\":\"File:LasTres_2.jpg\",{\"ns\":14,\"title\":\"Category:Cute dogs\"},{\"ns\":14,\"title\":\"Category:Costa Rican dogs\"}]}}}}\"; line: 1, column: 102]";
            List<String> categories = Arrays.asList("Category:Costa Rica", "Category:Cute dogs", "Category:Costa Rican dogs");
            FileRecord file0 = new FileRecord("File:LasTres_2.jpg", "127723", null, error);
            FileRecord file1 = new FileRecord("File:LasTres.jpg", "127722", categories, null);

            Assert.assertEquals(rows.get(0), file0);
            Assert.assertEquals(rows.get(1), file1);
            server.close();

        }
    }

}
