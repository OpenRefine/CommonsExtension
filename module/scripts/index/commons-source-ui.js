
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

    var cmtitle = $.trim(self._elmts.categoryInput[0].value);
    var inputContainer = $('<div></div>').appendTo(cmtitle);
    var input = $('<input></input>').appendTo(inputContainer);

    var endpoint = "https://commons.wikimedia.org/w/api.php?action=opensearch&format=json&formatversion=2&search="
    + cmtitle + "&namespace=14&limit=10";
    var suggestConfig = {
      commons_endpoint: endpoint,
      /*entity_type: entityType,
      language: $.i18n("core-recon/wd-recon-lang"),
      view_url: WikibaseManager.getSelectedWikibaseSiteIriForEntityType(entityType)+'{{id}'*/
    };

    input.suggestCategory(suggestConfig).bind("fb-select", function(evt, data) {
        inputContainer.data("jsonValue", {
            //type : "jsonp",
            id : data.id,
            //label: data.name,
        });
        changedCallback();
    });
    this._elmts.NextButton.click(function(evt) {
      if (cmtitle.length === 0) {
        window.alert($.i18n('commons-source/alert-retrieve'));
      } else {
        var doc = {};
        doc.input = cmtitle;
        self._controller.startImportingDocument(doc);
      }
    });

    this._body.find('.commons-page').hide();
  };

  Refine.CommonsSourceUI.prototype.focus = function() {
  };
