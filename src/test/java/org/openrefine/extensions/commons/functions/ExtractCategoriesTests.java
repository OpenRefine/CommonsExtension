package org.openrefine.extensions.commons.functions;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ExtractCategoriesTests {

    Function function = new ExtractCategories();
    
    @BeforeClass
    public void registerFunction() {
    	ControlFunctionRegistry.registerFunction("extractCategories", function);
    }
    
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
    @Test
    public void testSymbolParsing() {
        String wikitext = "{{Information\n"
                + "|Description={{en|1=View of Earth taken during ISS Expedition 30.}}\n"
                + "|Source=[https://eol.jsc.nasa.gov/SearchPhotos/photo.pl?mission=ISS030&roll=E&frame=226922 JSC Gateway to Astronaut Photography of Earth]\n"
                + "{{InFi|name=Altitude|value={{convert|211|nmi|km}}}}\n"
                + "}}\n"
                + "\n"
                + "== {{int:license-header}} ==\n"
                + "[[Category:ISS Expedition 30 Crew Earth Observations (dump)|226922]]\n"
                + "[[Category:Taken with Nikon D3s]]\n";

        Object result = function.call(new Properties(), new Object[] {wikitext});

        Assert.assertEquals(result, Arrays.asList("Category:ISS Expedition 30 Crew Earth Observations (dump)", "Category:Taken with Nikon D3s"));
    }
}
