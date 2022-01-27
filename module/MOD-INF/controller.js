
/*
 * Function invoked to initialize the extension.
 */
function init() {
   
  // Register our GREL functions so that they are visible in OpenRefine
  var CFR = Packages.com.google.refine.grel.ControlFunctionRegistry;

  CFR.registerFunction("extractCategories", new Packages.org.openrefine.extensions.commons.functions.ExtractCategories());
}
