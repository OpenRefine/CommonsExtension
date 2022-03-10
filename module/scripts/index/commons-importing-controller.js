//Internationalization init
var lang = navigator.language.split("-")[0]
		|| navigator.userLanguage.split("-")[0];
var dictionary = "";
$.ajax({
	url : "command/core/load-language?",
	type : "POST",
	async : false,
	data : {
	  module : "commons",
//		lang : lang
	},
	success : function(data) {
		dictionary = data['dictionary'];
                lang = data['lang'];
	}
});
$.i18n().load(dictionary, lang);
// End internationalization

Refine.CommonsImportingController = function(createProjectUI) {
  this._createProjectUI = createProjectUI;
  
  this._parsingPanel = createProjectUI.addCustomPanel();

  createProjectUI.addSourceSelectionUI({
    label: "Google Data",
    id: "gdata-source",
    ui: new Refine.GDataSourceUI(this)
  });
  
  $('#gdata-authorize').text($.i18n('gdata-auth/authorize-label')); 
  $('#gdata-authorized').text($.i18n('gdata-auth/authorized-label'));
};
Refine.CreateProjectUI.controllers.push(Refine.GDataImportingController);
