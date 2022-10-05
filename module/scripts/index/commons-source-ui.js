
Refine.CommonsSourceUI = function(controller) {
    this._controller = controller;
};

Refine.CommonsSourceUI.prototype.addRow = function() {

  var tr = $(`<tr id="categoryRow">
  <td><input size="72" class="category-input-box"/></td>
  <td><input size="1" class="depth-input-box"/></td>
  <td><a class="x-button" href><img src='images/close.png'></a></td></tr>`);
  $("#categoriesTable").append(tr);

  tr.find('a.x-button').attr('title',$.i18n('commons-import/remove-category'));

  var endpoint = "https://commons.wikimedia.org/w/api.php"
  // FIXME: twik configuration to not use Freebase
  var suggestConfig = {
    commons_endpoint: endpoint,
    language: $.i18n("core-recon/wd-recon-lang")
  };

  tr.find('input.category-input-box').suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
    tr.find('input.category-input-box').data("jsonValue", {
        id: data.id
    });
  });

  var xButton = tr.find('.x-button');
  xButton.on( "click", function(event) {
    tr.remove();
    event.preventDefault();
  });
}

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
  self.addRow();

  this._elmts.addCategoryButton.click(function(evt) {
    self.addRow();
  });

  this._elmts.NextButton.click(function(evt) {
    var categoryJsonObj = [];
    var doc = {};
    var trs = $('#categoriesTable').find('tr');
    trs.each(function( index, tr ) {
      if (index > 0) {
        categoryJsonObj.push({category : $(tr).find('input.category-input-box').val(),
        depth: $(tr).find('input.depth-input-box').val()});
      }
    });
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
