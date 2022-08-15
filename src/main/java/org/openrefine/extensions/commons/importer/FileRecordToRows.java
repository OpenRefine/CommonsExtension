package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.refine.importers.TabularImportingParserBase.TableDataReader;

public class FileRecordToRows implements TableDataReader {
    List<Object> rowsOfCells;

    Iterator<FileRecord> iteratorFileRecords;

    public FileRecordToRows(Iterator<FileRecord> iteratorFileRecords) {
        this.iteratorFileRecords = iteratorFileRecords;
    }
    @Override
    public List<Object> getNextRowOfCells() throws IOException {
        rowsOfCells = new ArrayList<>();
        rowsOfCells.add(iteratorFileRecords.next());

        return rowsOfCells;
    }
    @Override
    public String toString() {
        return "FileRecordToRows [rowsOfCells=" + rowsOfCells + ", iteratorFileRecords=" + iteratorFileRecords + "]";
    }

}
