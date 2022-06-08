
Refine.CommonsSourceUI = function(controller) {
    this._controller = controller;
  };

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
    // TO-DO: twik configuration to not use Freebase
    var suggestConfig = {
      commons_endpoint: endpoint,
      language: $.i18n("core-recon/wd-recon-lang")
    };

    self._elmts.categoryInput.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
        self._elmts.categoryInput.data("jsonValue", {
          id: data.id
      });
    });

    this._elmts.NextButton.click(function(evt) {
      var doc = {};
      doc.input = self._elmts.categoryInput.data("jsonValue").id;
      self._controller.startImportingDocument(doc);
    });

    this._body.find('.commons-page').hide();
  };

  Refine.CommonsSourceUI.prototype.focus = function() {
  };
