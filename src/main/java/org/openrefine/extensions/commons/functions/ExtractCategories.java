package org.openrefine.extensions.commons.functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrefine.extensions.commons.utils.WikitextParsingUtilities;
import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.nodes.WtInternalLink;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.Type;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ExtractCategories implements Function {
    // Set-up a simple wiki configuration
    ParserConfig parserConfig = new SimpleParserConfig();

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length != 1 || !(args[0] instanceof String)) {
            return new EvalError("Unexpected arguments for "+ControlFunctionRegistry.getFunctionName(this) + "(): got '" + new Type().call(bindings, args) + "' but expected a single String as an argument");
        }

        try {
            WtParsedWikitextPage parsedArticle = WikitextParsingUtilities.parseWikitext((String) args[0]);
            
            List<String> categories = new ArrayList<>();
            for (WtNode node : parsedArticle) {
                if (node instanceof WtInternalLink) {
                    String linkTarget = ((WtInternalLink)node).getTarget().getAsString();
                    if (linkTarget.startsWith("Category:")) { // TODO make this prefix configurable (passing another argument)
                        categories.add(linkTarget);
                    }
                }
            }
            return categories;
        } catch(IOException |xtc.parser.ParseException  e1) {
            return new EvalError("Could not parse wikitext: "+e1.getMessage());
        }
    }

    @Override
    public String getDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getReturns() {
        // TODO Auto-generated method stub
        return null;
    }

}
