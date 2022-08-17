package org.openrefine.extensions.commons.importer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.refine.importers.TabularImportingParserBase.TableDataReader;

/*
 * This class takes an Iterator<FileRecord> and converts each FileRecord to one or more rows
 *
 * @param iteratorFileRecords
 */
public class FileRecordToRows implements TableDataReader {
    Iterator<FileRecord> iteratorFileRecords;
    FileRecord fileRecord;
    String categoriesColumn;
    String mIdsColumn;
    List<Object> rowsOfCells;
    int relatedCategoriesIndex = 0;

    public FileRecordToRows(Iterator<FileRecord> iteratorFileRecords, String categoriesColumn, String mIdsColumn) {

        this.iteratorFileRecords = iteratorFileRecords;
        this.categoriesColumn = categoriesColumn;
        this.mIdsColumn = mIdsColumn;

    }

    /*
     * This method iterates over the parameters of a file record spreading them in rows
     *
     * @return a row containing a cell per file record parameter
     */
    @Override
    public List<Object> getNextRowOfCells() throws IOException {

        rowsOfCells = new ArrayList<>();
        // check if there's rows remaining from a previous call
        if (fileRecord != null && fileRecord.relatedCategories != null
                && relatedCategoriesIndex < fileRecord.relatedCategories.size()) {
            rowsOfCells.add(null);
            rowsOfCells.add(null);
            rowsOfCells.add(fileRecord.relatedCategories.get(relatedCategoriesIndex));
            relatedCategoriesIndex++;
            return rowsOfCells;
        } else if (iteratorFileRecords.hasNext()) {
            fileRecord = iteratorFileRecords.next();
            rowsOfCells.add(fileRecord.fileName);
            if (mIdsColumn.contentEquals("true")) {
                rowsOfCells.add("M" + fileRecord.pageId);
            }
            if (categoriesColumn.contentEquals("true")) {
                if (fileRecord.error != null) {
                    rowsOfCells.add(fileRecord.error);
                } else if (fileRecord.relatedCategories != null) {
                    rowsOfCells.add(fileRecord.relatedCategories.get(0));
                    relatedCategoriesIndex++;
                } else {
                    rowsOfCells.add(fileRecord.relatedCategories);
                }
            } else if ((mIdsColumn.contentEquals("true")) && (categoriesColumn.contentEquals("true"))) {
                rowsOfCells.add("M" + fileRecord.pageId);
                if (fileRecord.error != null) {
                    rowsOfCells.add(fileRecord.error);
                } else if (fileRecord.relatedCategories != null) {
                    rowsOfCells.add(fileRecord.relatedCategories.get(0));
                    relatedCategoriesIndex++;
                } else {
                    rowsOfCells.add(fileRecord.relatedCategories);
                }
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
