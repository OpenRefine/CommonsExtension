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
  CFR.registerFunction("extractFromTemplate", new Packages.org.openrefine.extensions.commons.functions.ExtractFromTemplate());

  // Register importer and exporter
  var IM = Packages.com.google.refine.importing.ImportingManager;

  IM.registerController(
    module,
    "commons-importing-controller",
    new Packages.org.openrefine.extensions.commons.utils.CommonsImportingController()
  );

  // Script files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/scripts",
    module,
    [
      "scripts/index/commons-importing-controller.js",
      "scripts/index/commons-source-ui.js",
      /* add suggest library from core */
      "externals/suggest/suggest-4_3a.js",
      "scripts/index/category-suggest.js"
    ]
  );

  // Style files to inject into /index page
  ClientSideResourceManager.addPaths(
    "index/styles",
    module,
    [
      "externals/suggest/css/suggest-4_3.min.css"
    ]
  );
}
