package org.openrefine.extensions.commons.functions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.grel.Function;

public class ExtractCategoriesTests {

    Function function = new ExtractCategories();
    
    @Test
    public void testValidWikitext() {
        String wikitext = "{{Artwork\n"
                + "|author=John Doe\n"
                + "|date=1984-03-09"
                + "}}\n"
                + "[[Ignored internal link]]\n"
                + "\n"
                + "[[Category:Sculptures by John Doe]]\n"
                + "[[Category:Uploads by StoneWizard]]";
        
        Object result = function.call(new Properties(), new Object[] {wikitext});
              
        Assert.assertEquals(result, Arrays.asList("Category:Sculptures by John Doe", "Category:Uploads by StoneWizard"));
    }
    
    @Test
    public void testInvalidWikitext() {
        String wikitext = "{{Artwork\n"
                + "|author=John Doe\n"
                + "|date=1984-03-09"
                + "[[Category:Sculptures by John Doe";
        
        Object result = function.call(new Properties(), new Object[] {wikitext});
              
        Assert.assertEquals(result, Collections.<String>emptyList());
    }
}
