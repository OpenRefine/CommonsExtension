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
     * Test list generation of categories related to a given file
     */
    @Test
    public void testNext() throws Exception {

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
            Assert.assertTrue(rcf.hasNext());
            rows.add(rcf.next());
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
            Assert.assertEquals(rows.get(2), null);

        }
    }

}
