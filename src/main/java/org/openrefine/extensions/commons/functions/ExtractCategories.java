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

import de.fau.cs.osr.ptk.common.AstVisitor;

public class ExtractCategories implements Function {

    public static class CategoriesExtractor extends AstVisitor<WtNode>{

        private List<String> categories = new ArrayList<>();

        public void visit(WtNode node) {
            iterate(node);
        }
        public void visit(WtInternalLink internalLink) {
            String currentInternalLink = internalLink.getTarget().getAsString();
            if (currentInternalLink.startsWith("Category:")) {
                categories.add(currentInternalLink);
            }
        }

    }

    // Set-up a simple wiki configuration
    ParserConfig parserConfig = new SimpleParserConfig();

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length != 1 || !(args[0] instanceof String)) {
            return new EvalError("Unexpected arguments for "+ControlFunctionRegistry.getFunctionName(this) + "(): got '" + new Type().call(bindings, args) + "' but expected a single String as an argument");
        }

        try {
            WtParsedWikitextPage parsedArticle = WikitextParsingUtilities.parseWikitext((String) args[0]);

            CategoriesExtractor extractor = new CategoriesExtractor();
            extractor.go(parsedArticle);
            List<String> result = extractor.categories;

            return result;

        } catch(IOException |xtc.parser.ParseException  e1) {
            return new EvalError("Could not parse wikitext: "+e1.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "extracts the list of categories from the wikitext of a page";
    }

    @Override
    public String getReturns() {
        return "arrays of strings";
    }

}
