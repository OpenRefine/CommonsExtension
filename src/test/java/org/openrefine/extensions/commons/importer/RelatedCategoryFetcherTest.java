package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class RelatedCategoryFetcherTest {

    /**
     * Test list generation of categories related to a given file
     */
    @Test
    public void testNext() throws Exception {

        try (MockWebServer server = new MockWebServer()) {
            server.start();
            HttpUrl url = server.url("/w/api.php");
            String jsonResponseContinue = "{\"batchcomplete\":\"\",\"query\":{\"pages\":{\"120352461\":"
                    + "{\"pageid\":120352461,\"ns\":6,\"title\":\"File:Esferas de CR.jpg\","
                    + "\"categories\":[{\"ns\":14,\"title\":\"Category:Costa Rica\"},"
                    + "{\"ns\":14,\"title\":\"Category:Historical Site\"},{\"ns\":14,\"title\":\"Category:Misteries of Life\"}]}}}}";
            server.enqueue(new MockResponse().setBody(jsonResponseContinue));
            FileRecord fr = new FileRecord("File:Esferas de CR.jpg", "120352461", null);
            Iterator<FileRecord> iteratorFileRecords = fr.iterator();
            RelatedCategoryFetcher rcf = new RelatedCategoryFetcher(url.toString(), iteratorFileRecords);

            List<Object> rows = new ArrayList<>();
            rows.add(rcf);

        }
    }

}
