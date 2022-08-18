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
    final Iterator<FileRecord> iteratorFileRecords;
    FileRecord fileRecord;
    final boolean categoriesColumn;
    final boolean mIdsColumn;
    int relatedCategoriesIndex = 0;

    public FileRecordToRows(Iterator<FileRecord> iteratorFileRecords, boolean categoriesColumn, boolean mIdsColumn) {

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

        List<Object> rowsOfCells;
        rowsOfCells = new ArrayList<>();
        // check if there's rows remaining from a previous call
        if (fileRecord != null && fileRecord.relatedCategories != null
                && relatedCategoriesIndex < fileRecord.relatedCategories.size()) {
            rowsOfCells.add(null);
            if (mIdsColumn) {
                rowsOfCells.add(null);
            }
            rowsOfCells.add(fileRecord.relatedCategories.get(relatedCategoriesIndex));
            relatedCategoriesIndex++;
            return rowsOfCells;
        } else if (iteratorFileRecords.hasNext()) {
            fileRecord = iteratorFileRecords.next();
            relatedCategoriesIndex = 0;
            rowsOfCells.add(fileRecord.fileName);
            if (mIdsColumn) {
                rowsOfCells.add("M" + fileRecord.pageId);
            }
            if (categoriesColumn) {
                if (fileRecord.error != null) {
                    rowsOfCells.add(fileRecord.error);
                } else if (fileRecord.relatedCategories != null) {
                    rowsOfCells.add(fileRecord.relatedCategories.get(0));
                    relatedCategoriesIndex++;
                } else {
                    rowsOfCells.add(fileRecord.relatedCategories);
                }
            } else if (mIdsColumn && categoriesColumn) {
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

}
