
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

  var categoryDepth = $.trim(self._elmts.nestedBox[0].value);
  self._elmts.categoryInput.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
      self._elmts.categoryInput.data("jsonValue", {
        id: data.id,
        depth: categoryDepth
    });
    categoryJsonObj.push({category : self._elmts.categoryInput.data("jsonValue").id,
    depth : self._elmts.categoryInput.data("jsonValue").depth});
  });

  var deleteLink = $('<a></a>')
  .addClass("remove-category")
  .attr("title",$.i18n('commons-import/remove-category'))
  .attr("href","")
  .html("<img src='images/close.png' />")
  .click(function() {
    categoryJsonObj.pop();
    return false;
  }).appendTo(
    (self._elmts.categoryRow)
  );

  // on addCategoryButton click
  this._elmts.addCategoryButton.click(function(evt) {
    // add text fields
    var addCategory = $("<input size='72'  width='70%'>").insertBefore(self._elmts.addCategoryRow);
    var addDepth = $("<input size='1'  width='10%' style='text-align: center;'>").insertAfter(addCategory);
    addCategory.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
      addCategory.data("jsonValue", {
        id: data.id
      });
      categoryJsonObj.push({category : addCategory.data("jsonValue").id});
    });
    var deleteLink = $('<a></a>')
    .addClass("remove-category")
    .attr("title",$.i18n('commons-import/remove-category'))
    .attr("href","")
    .html("<img src='images/close.png' />")
    .click(function() {
      categoryJsonObj.pop();
      return false;
    }).insertAfter(
      (addDepth)
    );
  });

  this._elmts.NextButton.click(function(evt) {
    var doc = {};
    if (categoryJsonObj.length === 0) {
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
