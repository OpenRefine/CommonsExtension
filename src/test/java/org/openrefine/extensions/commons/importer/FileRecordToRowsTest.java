package org.openrefine.extensions.commons.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileRecordToRowsTest {

    @Test
    public void testGetNextRowOfCells() throws Exception {

        List<String> categories = new ArrayList<>();
        categories.add("Category:Costa Rica");
        categories.add("Category:Cute dogs");
        categories.add("Category:Costa Rican dogs");
        FileRecord file0 = new FileRecord("File:LasTres.jpg", "127722", categories, null);
        FileRecord file1 = new FileRecord("File:Playa Gandoca.jpg", "112933", null, null);
        List<FileRecord> fileRecords = new ArrayList<>();
        fileRecords.add(file0);
        fileRecords.add(file1);
        FileRecordToRows frtr = new FileRecordToRows(fileRecords.iterator());

        List<Object> rows = new ArrayList<>();
        rows.add(frtr.getNextRowOfCells());
        rows.add(frtr.getNextRowOfCells());

        Assert.assertEquals(rows.get(0), Arrays.asList(file0));
        Assert.assertEquals(rows.get(1), Arrays.asList(file1));

    }

}
