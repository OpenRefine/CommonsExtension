/*
 * Controller for Commons extension.
 *
 * This is run in the Butterfly (ie Refine) server context using the Rhino
 * Javascript interpreter.
 */

var html = "text/html";
var encoding = "UTF-8";
var version = "0.3";

// Register our Javascript (and CSS) files to get loaded
var ClientSideResourceManager = Packages.com.google.refine.ClientSideResourceManager;

/*
 * Function invoked to initialize the extension.
 */
function init() {
   
  // Register our GREL functions so that they are visible in OpenRefine
  var CFR = Packages.com.google.refine.grel.ControlFunctionRegistry;

  CFR.registerFunction("extractCategories", new Packages.org.openrefine.extensions.commons.functions.ExtractCategories());

}
