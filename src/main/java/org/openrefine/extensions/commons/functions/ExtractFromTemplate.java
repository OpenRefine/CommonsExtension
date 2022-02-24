package org.openrefine.extensions.commons.functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.openrefine.extensions.commons.utils.WikitextParsingUtilities;
import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.nodes.WtNode;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtTemplate;
import org.sweble.wikitext.parser.nodes.WtTemplateArgument;
import org.sweble.wikitext.parser.nodes.WtTemplateArguments;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.Type;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

import de.fau.cs.osr.ptk.common.AstVisitor;

public class ExtractFromTemplate implements Function {

    public class FindTemplateValues extends AstVisitor<WtNode> {

        private String templateName;
        private String paramName;
        private List<String> values = new ArrayList<>();

        public FindTemplateValues(String tName, String pName) {
            this.templateName = tName;
            this.paramName = pName;
        }

        public void visit(WtNode node) {
            iterate(node);
        }
        public void visit(WtTemplate template) {
            // only render templates if we are told to do so or inside a reference
            if (templateName.equals(template.getName().toString())) {
                WtTemplateArguments args = template.getArgs();
                for (int i = 0; i != args.size(); i++) {
                    iterate(args.get(i));//add WtTemplateArgument
                }
            }
            iterate(template);
        }
        public void visit(WtTemplateArgument args) {
            // do not render templates that are inside a reference
            if (paramName.equals(args.getName().getAsString())) {
                values.add(args.getAttribute(paramName).toString());
            }
            iterate(args);
        }

    }

    // Set-up a simple wiki configuration
    ParserConfig parserConfig = new SimpleParserConfig();

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length != 3 || !(args[0] instanceof String)) {
            return new EvalError("Unexpected arguments for "+ControlFunctionRegistry.getFunctionName(this) + "(): got '" + new Type().call(bindings, args) + "' but expected a single String as an argument");
        }

        try {
            WtParsedWikitextPage parsedArticle = WikitextParsingUtilities.parseWikitext((String) args[0]);
            String tName = (String) args[1];
            String pName = (String) args[2];

            FindTemplateValues extractor = new FindTemplateValues(tName, pName);
            extractor.go(parsedArticle);

            List<String> values = extractor.values;

            return values;

        } catch(IOException |xtc.parser.ParseException  e1) {
            return new EvalError("Could not parse wikitext: "+e1.getMessage());
        }
    }


    @Override
    public String getDescription() {
        return "extracts the list of values of a given parameter from the wikitext of a template";
    }

    public String getParams() {
        return "";
    }

    @Override
    public String getReturns() {
        return "arrays of strings";
    }

}
