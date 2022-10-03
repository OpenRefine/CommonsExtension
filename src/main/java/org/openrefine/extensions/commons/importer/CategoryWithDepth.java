package org.openrefine.extensions.commons.importer;

import java.util.Objects;

/**
 * This class stores the user-defined categories and
 * corresponding subcategories depth level
 */
public class CategoryWithDepth {
    String categoryName;
    int depth;

    public CategoryWithDepth(String categoryName, int depth) {
        this.categoryName = categoryName;
        this.depth = depth;
    }

    @Override
    public String toString() {
        return "CategoryWithDepth [categoryName=" + categoryName + ", depth=" + depth + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryName, depth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CategoryWithDepth other = (CategoryWithDepth) obj;
        return Objects.equals(categoryName, other.categoryName) && depth == other.depth;
    }

}