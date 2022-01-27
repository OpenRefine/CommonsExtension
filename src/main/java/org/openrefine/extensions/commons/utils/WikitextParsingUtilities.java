package org.openrefine.extensions.commons.utils;

import java.io.IOException;

import org.sweble.wikitext.parser.ParserConfig;
import org.sweble.wikitext.parser.WikitextEncodingValidator;
import org.sweble.wikitext.parser.WikitextParser;
// import org.sweble.wikitext.parser.WikitextParser;
import org.sweble.wikitext.parser.WikitextPreprocessor;
import org.sweble.wikitext.parser.encval.ValidatedWikitext;
import org.sweble.wikitext.parser.nodes.WtParsedWikitextPage;
import org.sweble.wikitext.parser.nodes.WtPreproWikitextPage;
import org.sweble.wikitext.parser.parser.PreprocessorToParserTransformer;
import org.sweble.wikitext.parser.preprocessor.PreprocessedWikitext;
import org.sweble.wikitext.parser.utils.SimpleParserConfig;

import xtc.parser.ParseException;

public class WikitextParsingUtilities {
    // Set-up a simple wiki configuration
    private static ParserConfig parserConfig = new SimpleParserConfig();
    private static WikitextPreprocessor wikitextPreprocessor = new WikitextPreprocessor(parserConfig);
    private static WikitextParser parser = new WikitextParser(parserConfig);
    private static WikitextEncodingValidator wikitextEncodingValidator = new WikitextEncodingValidator();
    
    public static WtParsedWikitextPage parseWikitext(String wikitext) throws IOException, ParseException {
        String title = "Page title";
        ValidatedWikitext validated = wikitextEncodingValidator.validate(parserConfig, wikitext, title);


        WtPreproWikitextPage prepArticle =
                        (WtPreproWikitextPage) wikitextPreprocessor.parseArticle(validated, title, false);

        // Parsing
        PreprocessedWikitext ppw = PreprocessorToParserTransformer .transform(prepArticle);

        return (WtParsedWikitextPage) parser.parseArticle(ppw, title);
    }
}
