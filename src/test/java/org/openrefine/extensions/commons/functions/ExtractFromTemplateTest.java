package org.openrefine.extensions.commons.functions;

import java.util.Arrays;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.refine.grel.Function;

public class ExtractFromTemplateTest {

    Function function = new ExtractFromTemplate();

    @Test
    public void testTemplateName() {
        String wikitext = "{{some template|bar=test}}\n"
                + "{{foo1|bar={{other template}}}}\n"
                + "{{foo2| foo3 = not important| bar = second value }}";
        
        Object result = function.call(new Properties(), new Object[] {wikitext});

        Assert.assertEquals(result, Arrays.asList("other template" + ": " + "second value"));
    }

}
