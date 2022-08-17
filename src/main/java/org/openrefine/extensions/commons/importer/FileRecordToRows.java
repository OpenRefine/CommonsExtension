package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.refine.importers.TabularImportingParserBase.TableDataReader;

public class FileRecordToRows implements TableDataReader {
    List<Object> rowsOfCells;
    FileRecord fileRecord;
    Iterator<FileRecord> iteratorFileRecords;
    int relatedCategoriesIndex = 0;

    public FileRecordToRows(Iterator<FileRecord> iteratorFileRecords) {
        this.iteratorFileRecords = iteratorFileRecords;
    }

    @Override
    public List<Object> getNextRowOfCells() throws IOException {
        rowsOfCells = new ArrayList<>();
        // check if there's rows remaining from a previous call
        if (fileRecord != null && relatedCategoriesIndex < fileRecord.relatedCategories.size()) {
            rowsOfCells.add(null);
            rowsOfCells.add(null);
            rowsOfCells.add(fileRecord.relatedCategories.get(relatedCategoriesIndex));
            relatedCategoriesIndex++;
            return rowsOfCells;
        } else if (iteratorFileRecords.hasNext() != false) {
            fileRecord = iteratorFileRecords.next();
            rowsOfCells.add(fileRecord.fileName);
            rowsOfCells.add(fileRecord.pageId);
            if (fileRecord.error != null) {
                rowsOfCells.add(fileRecord.error);
            } else if (fileRecord.relatedCategories != null) {
                rowsOfCells.add(fileRecord.relatedCategories.get(0));
                relatedCategoriesIndex++;
            } else {
                rowsOfCells.add(fileRecord.relatedCategories);
            }
            return rowsOfCells;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "FileRecordToRows [rowsOfCells=" + rowsOfCells + ", iteratorFileRecords=" + iteratorFileRecords + "]";
    }

}
