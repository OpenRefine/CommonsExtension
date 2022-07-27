package org.openrefine.extensions.commons.importer;

import java.util.List;
import java.util.Objects;

/**
 * This class represents the contents of a record in a project
 *
 */
public class FileRecord {
    final String fileName;
    final String mId;
    final List<String> relatedCategories;

    public FileRecord(String fileName, String mId, List<String> relatedCategories) {
        this.fileName = fileName;
        this.mId = mId;
        this.relatedCategories = relatedCategories;
    }

    @Override
    public String toString() {
        return "FileRecord [fileName=" + fileName + ", mId=" + mId + ", relatedCategories=" + relatedCategories + "]";
    }
    @Override
    public int hashCode() {
        return Objects.hash(fileName, mId, relatedCategories);
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
        return Objects.equals(fileName, other.fileName) && Objects.equals(mId, other.mId)
                && Objects.equals(relatedCategories, other.relatedCategories);
    }

}
