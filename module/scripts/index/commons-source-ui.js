
Refine.CommonsSourceUI = function(controller) {
    this._controller = controller;
};
var categoryJsonObj = [];

Refine.CommonsSourceUI.prototype.attachUI = function(body) {

  this._body = body;
  this._body.html(DOM.loadHTML("commons", "scripts/index/import-from-commons-form.html"));
  this._elmts = DOM.bind(this._body);

  $('#category-name').text($.i18n('commons-import/category'));
  $('#commons-import').html($.i18n('commons-import/import-by-category'));
  $('#commons-import-nested').html($.i18n('commons-import/import-nested-category'));
  $('#commons-add').html($.i18n('commons-import/add'));
  $('#commons-next').html($.i18n('commons-import/next->'));
  $('#commons-retrieving').text($.i18n('commons-import/retrieving'));

  var self = this;

  var endpoint = "https://commons.wikimedia.org/w/api.php"
  // FIXME: twik configuration to not use Freebase
  var suggestConfig = {
    commons_endpoint: endpoint,
    language: $.i18n("core-recon/wd-recon-lang")
  };

  self._elmts.categoryInput.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
      self._elmts.categoryInput.data("jsonValue", {
        id: data.id
    });
    categoryJsonObj.push({category : self._elmts.categoryInput.data("jsonValue").id});
  });

  // on addCategoryButton click
  this._elmts.addCategoryButton.click(function(evt) {
    // add text fields
    var addCategory = $("<input size='72'>").insertBefore(self._elmts.addCategoryRow)
    addCategory.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
      addCategory.data("jsonValue", {
        id: data.id
      });
      categoryJsonObj.push({category : addCategory.data("jsonValue").id});
    });
  });

  this._elmts.NextButton.click(function(evt) {
    var doc = {};
    // FIXME: check at least one of the text boxes is not empty, pass the non-empty field's data to backend
    if (self._elmts.categoryInput.data("jsonValue").id.length === 0) {
      window.alert($.i18n('commons-source/alert-retrieve'));
    } else {
      doc.categoryJsonObj = categoryJsonObj;
      self._controller.startImportingDocument(doc);
    }
  });

  this._body.find('.commons-page').hide();
};

Refine.CommonsSourceUI.prototype.focus = function() {
};
