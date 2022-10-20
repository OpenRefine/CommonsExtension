package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileRecordToRowsTest {

    /**
     * Test row generation from file records
     */
    @Test
    public void testGetNextRowOfCells() throws Exception {

        List<String> categories = Arrays.asList("Category:Costa Rica", "Category:Cute dogs", "Category:Costa Rican dogs");
        FileRecord file0 = new FileRecord("File:LasTres.jpg", "127722", categories, null);
        FileRecord file1 = new FileRecord("File:Playa Gandoca.jpg", "112933", null, null);
        List<FileRecord> fileRecords = Arrays.asList(file0, file1);
        FileRecordToRows frtr = new FileRecordToRows(fileRecords.iterator(), true, true);

        List<Object> rows = new ArrayList<>();
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());

        Assert.assertEquals(rows.get(0).toString(), Arrays.asList("File:LasTres.jpg, M127722, Category:Costa Rica").toString());
        Assert.assertEquals(rows.get(1), Arrays.asList(null, null, "Category:Cute dogs"));
        Assert.assertEquals(rows.get(2), Arrays.asList(null, null, "Category:Costa Rican dogs"));
        Assert.assertEquals(rows.get(3).toString(), Arrays.asList("File:Playa Gandoca.jpg", "M112933", null).toString());
        Assert.assertEquals(rows.get(4), null);

    }

}
