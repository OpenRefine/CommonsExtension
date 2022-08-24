package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
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
            List<FileRecord> originalRecords = new ArrayList<>();
            FileRecord fr0 = new FileRecord("File:LasTres.jpg", "127722", null, null);
            FileRecord fr1 = new FileRecord("File:Pejiballes.jpg", "127752", null, null);
            originalRecords.add(fr0);
            originalRecords.add(fr1);
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), originalRecords.iterator());

            List<Object> rows = new ArrayList<>();
            rows.add(rcf.getRelatedCategories(originalRecords));
        }

    }

}
