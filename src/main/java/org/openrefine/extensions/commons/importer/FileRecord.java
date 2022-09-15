package org.openrefine.extensions.commons.importer;

import java.util.List;
import java.util.Objects;

/**
 * This class represents the contents of a record in a project
 *
 */
public class FileRecord {
    final String fileName;
    final String pageId;
    final List<String> relatedCategories;
    final String error;

    public FileRecord(String fileName, String pageId, List<String> relatedCategories, String error) {
        this.fileName = fileName;
        this.pageId = pageId;
        this.relatedCategories = relatedCategories;
        this.error = error;
    }

    @Override
    public String toString() {
        return "FileRecord [fileName=" + fileName + ", pageId=" + pageId + ", relatedCategories=" + relatedCategories + ", error=" + error + "]";
    }
    @Override
    public int hashCode() {
        return Objects.hash(fileName, pageId, relatedCategories, error);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FileRecord other = (FileRecord) obj;
        return Objects.equals(fileName, other.fileName) && Objects.equals(pageId, other.pageId)
                && Objects.equals(relatedCategories, other.relatedCategories) && Objects.equals(error, other.error);
    }

}
