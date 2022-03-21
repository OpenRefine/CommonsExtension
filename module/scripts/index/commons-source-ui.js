
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

    this._elmts.NextButton.click(function(evt) {
      var url = "https://en.wikipedia.org/w/api.php";
      var cmtitle = "Category:" + $.trim(self._elmts.categoryInput[0].value);
      // Add sugest service?
      var cmlimit = $.trim(self._elmts.nestedBox[0].value);
      if (cmtitle.length === 0) {
        //Gray out "Nested categories level" button
      }
      if (cmlimit.length === 0) {
        cmlimit = "1";
      }
      var params = {
        action: "query",
        list: "categorymembers",
        cmtitle,
        cmtype: "subcat",
        cmlimit,
        format: "json"
      };

      url = url + "?origin=*";
      Object.keys(params).forEach(function(key){url += "&" + key + "=" + params[key];});

      fetch(url)
          .then(function(response){return response.json();})
          .then(function(response) {
              var categories = response.query.categorymembers;
              for (var category in categories) {
                  console.log(categories[category].title);
                  /*var doc = {ans: categories[category].title};
                  self._controller.startImportingDocument(doc);*/
              }
          })
          .catch(function(error){console.log(error);});

      var doc = {};
      self._controller.startImportingDocument(doc);
    });

    this._body.find('.commons-page').hide();
  };

  Refine.CommonsSourceUI.prototype.focus = function() {
  };
