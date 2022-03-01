package org.openrefine.extensions.commons.functions;

import java.util.Arrays;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ExtractFromTemplateTest {

    Function function = new ExtractFromTemplate();

    @BeforeClass
    public void registerFunction() {
        ControlFunctionRegistry.registerFunction("extractFromTemplate", function);
    }

    @Test
    public void testTemplateName() {
        String wikitext = "{{some template|bar=test}}\n"
                + "{{foo|bar={{other template}}}}\n"
                + "{{foo| foo = not important| bar = second value }}";
        
        Object result = function.call(new Properties(), new Object[] {wikitext, "foo", "bar"});

        Assert.assertEquals(result, Arrays.asList("{{other template}}", "second value"));
    }

}
