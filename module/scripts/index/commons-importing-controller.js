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
    label: "Wikimedia Commons",
    id: "commons-source",
    ui: new Refine.CommonsSourceUI(this)
  });
  
};
Refine.CreateProjectUI.controllers.push(Refine.CommonsImportingController);

Refine.CommonsImportingController.prototype.startImportingDocument = function(doc) {
  var dismiss = DialogSystem.showBusy($.i18n('commons-import/preparing'));
  var self = this;
  Refine.postCSRF(
    "command/core/create-importing-job",
    null,
    function(data) {
      Refine.wrapCSRF(function(token) {
        $.post(
            "command/core/importing-controller?" + $.param({
            "controller": "commons/commons-importing-controller",
            "subCommand": "initialize-parser-ui",
            /* all the selected categories */
            "categoryJsonValue" : JSON.stringify(doc.categoryJsonObj),// this serializes the string
            "csrf_token": token
            }),
            null,
            function(data2) {
                dismiss();

                if (data2.status == 'ok') {
                    self._doc = doc;
                    self._jobID = data.jobID;
                    self._options = data2.options;

                    self._showParsingPanel();
                } else {
                    alert(data2.message);
                }
            },
            "json"
        );
      });
    },
    "json"
  );
};

Refine.CommonsImportingController.prototype.getOptions = function() {
  var options = {
    categoryJsonValue: this._doc.categoryJsonObj,
  };

  var parseIntDefault = function(s, def) {
    try {
      var n = parseInt(s);
      if (!isNaN(n)) {
        return n;
      }
    } catch (e) {
      // Ignore
    }
    return def;
  };

  if (this._parsingPanelElmts.skipCheckbox[0].checked) {
    options.skipDataLines = parseIntDefault(this._parsingPanelElmts.skipInput[0].value, 0);
  } else {
    options.skipDataLines = 0;
  }
  if (this._parsingPanelElmts.limitCheckbox[0].checked) {
    options.limit = parseIntDefault(this._parsingPanelElmts.limitInput[0].value, -1);
  } else {
    options.limit = -1;
  }

  options.disableAutoPreview = this._parsingPanelElmts.disableAutoPreviewCheckbox[0].checked;
  options.categoriesColumn = this._parsingPanelElmts.categoriesColumnCheckbox[0].checked;
  options.mIdsColumn = this._parsingPanelElmts.mIdsCheckbox[0].checked;
  console.log(options);

  return options;
};

Refine.CommonsImportingController.prototype._showParsingPanel = function() {
  var self = this;

  this._parsingPanel.unbind().empty().html(
      DOM.loadHTML("commons", 'scripts/index/commons-parsing-panel.html'));
  this._parsingPanelElmts = DOM.bind(this._parsingPanel);

  this._parsingPanelElmts.startOverButton.html($.i18n('commons-parsing/start-over'));
  this._parsingPanelElmts.commons_conf_pars.html($.i18n('commons-parsing/conf-pars'));
  this._parsingPanelElmts.commons_proj_name.html($.i18n('commons-parsing/proj-name'));
  this._parsingPanelElmts.createProjectButton.html($.i18n('commons-parsing/create-proj'));
  this._parsingPanelElmts.commons_options.html($.i18n('commons-parsing/option'));
  this._parsingPanelElmts.commons_discard_next.html($.i18n('commons-parsing/discard-next'));
  this._parsingPanelElmts.commons_discard.html($.i18n('commons-parsing/discard'));
  this._parsingPanelElmts.commons_limit_next.html($.i18n('commons-parsing/limit-next'));
  this._parsingPanelElmts.commons_limit.html($.i18n('commons-parsing/limit'));

  this._parsingPanelElmts.commons_wiki_options.html($.i18n('commons-parsing/wiki-option'));
  this._parsingPanelElmts.commons_categories_column.text($.i18n('commons-parsing/categories-column'));
  this._parsingPanelElmts.commons_mids_column.text($.i18n('commons-parsing/mids-column'));

  this._parsingPanelElmts.previewButton.html($.i18n('commons-parsing/preview-button'));
  this._parsingPanelElmts.commons_disable_auto_preview.text($.i18n('commons-parsing/disable-auto-preview'));
  this._parsingPanelElmts.commons_updating.html($.i18n('commons-parsing/updating-preview'));

  if (this._parsingPanelResizer) {
    $(window).unbind('resize', this._parsingPanelResizer);
  }

  this._parsingPanelResizer = function() {
    var elmts = self._parsingPanelElmts;
    var width = self._parsingPanel.width();
    var height = self._parsingPanel.height();
    var headerHeight = elmts.wizardHeader.outerHeight(true);
    var controlPanelHeight = 250;

    elmts.dataPanel
    .css("left", "0px")
    .css("top", headerHeight + "px")
    .css("width", (width - DOM.getHPaddings(elmts.dataPanel)) + "px")
    .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.dataPanel)) + "px");
    elmts.progressPanel
    .css("left", "0px")
    .css("top", headerHeight + "px")
    .css("width", (width - DOM.getHPaddings(elmts.progressPanel)) + "px")
    .css("height", (height - headerHeight - controlPanelHeight - DOM.getVPaddings(elmts.progressPanel)) + "px");

    elmts.controlPanel
    .css("left", "0px")
    .css("top", (height - controlPanelHeight) + "px")
    .css("width", (width - DOM.getHPaddings(elmts.controlPanel)) + "px")
    .css("height", (controlPanelHeight - DOM.getVPaddings(elmts.controlPanel)) + "px");
  };

  $(window).resize(this._parsingPanelResizer);
  this._parsingPanelResizer();

  this._parsingPanelElmts.startOverButton.click(function() {
    // explicitly cancel the import job
    Refine.CreateProjectUI.cancelImportingJob(self._jobID);

    delete self._doc;
    delete self._jobID;
    delete self._options;

    self._createProjectUI.showSourceSelectionPanel();
  });

  this._parsingPanelElmts.createProjectButton.click(function() { self._createProject(); });

  this._parsingPanelElmts.previewButton.click(function() { self._updatePreview(); });

  this._parsingPanelElmts.projectNameInput[0].value = this._doc.title;

  if (this._options.limit > 0) {
    this._parsingPanelElmts.limitCheckbox.prop("checked", true);
    this._parsingPanelElmts.limitInput[0].value = this._options.limit.toString();
  }
  if (this._options.skipDataLines > 0) {
    this._parsingPanelElmts.skipCheckbox.prop("checked", true);
    this._parsingPanelElmts.skipInput.value[0].value = this._options.skipDataLines.toString();
  }
  if (this._options.disableAutoPreview) {
    this._parsingPanelElmts.disableAutoPreviewCheckbox.prop('checked', true);
  }
  if (this._options.categoriesColumn) {
    this._parsingPanelElmts.categoriesColumnCheckbox.prop('checked', true);
  }
  if (this._options.mIdsColumn) {
    this._parsingPanelElmts.mIdsCheckbox.prop('checked', true);
  }

  // If disableAutoPreviewCheckbox is not checked, we will schedule an automatic update
  var onChange = function() {
    if (!self._parsingPanelElmts.disableAutoPreviewCheckbox[0].checked)
    {
        self._scheduleUpdatePreview();
    }
  };
  this._parsingPanel.find("input").bind("change", onChange);
  this._parsingPanel.find("select").bind("change", onChange);

  this._createProjectUI.showCustomPanel(this._parsingPanel);
  this._updatePreview();
};

Refine.CommonsImportingController.prototype._scheduleUpdatePreview = function() {
  if (this._timerID != null) {
    window.clearTimeout(this._timerID);
    this._timerID = null;
  }

  var self = this;
  this._timerID = window.setTimeout(function() {
    self._timerID = null;
    self._updatePreview();
  }, 500); // 0.5 second
};

Refine.CommonsImportingController.prototype._updatePreview = function() {
  var self = this;

  this._parsingPanelElmts.dataPanel.hide();
  this._parsingPanelElmts.progressPanel.show();

  Refine.wrapCSRF(function(token) {
    $.post(
        "command/core/importing-controller?" + $.param({
        "controller": "commons/commons-importing-controller",
        "jobID": self._jobID,
        "subCommand": "parse-preview",
        "csrf_token": token
        }),
        {
        "options" : JSON.stringify(self.getOptions())
        },
        function(result) {
        if (result.status == "ok") {
            self._getPreviewData(function(projectData) {
            self._parsingPanelElmts.progressPanel.hide();
            self._parsingPanelElmts.dataPanel.show();

            new Refine.PreviewTable(projectData, self._parsingPanelElmts.dataPanel.unbind().empty());
            });
        } else {
            self._parsingPanelElmts.progressPanel.hide();
            alert('Errors :\n' +
            (result.message) ? result.message : Refine.CreateProjectUI.composeErrorMessage(job));
        }
        },
        "json"
    );
  });
};

Refine.CommonsImportingController.prototype._getPreviewData = function(callback, numRows) {
  var self = this;
  var result = {};

  $.post(
    "command/core/get-models?" + $.param({ "importingJobID" : this._jobID }),
    null,
    function(data) {
      for (var n in data) {
        if (data.hasOwnProperty(n)) {
          result[n] = data[n];
        }
      }

      $.post(
        "command/core/get-rows?" + $.param({
          "importingJobID" : self._jobID,
          "start" : 0,
          "limit" : numRows || 100 // More than we parse for preview anyway
        }),
        null,
        function(data) {
          result.rowModel = data;
          callback(result);
        },
        "json"
      );
    },
    "json"
  );
};

Refine.CommonsImportingController.prototype._createProject = function() {
  var projectName = $.trim(this._parsingPanelElmts.projectNameInput[0].value);
  if (projectName.length == 0) {
    window.alert("Please name the project.");
    this._parsingPanelElmts.projectNameInput.focus();
    return;
  }

  var self = this;
  var options = this.getOptions();
  options.projectName = projectName;
  Refine.wrapCSRF(function(token) {
    $.post(
        "command/core/importing-controller?" + $.param({
        "controller": "commons/commons-importing-controller",
        "jobID": self._jobID,
        "subCommand": "create-project",
        "csrf_token": token
        }),
        {
        "options" : JSON.stringify(options)
        },
        function(o) {
        if (o.status == 'error') {
            alert(o.message);
        } else {
            var start = new Date();
            var timerID = window.setInterval(
            function() {
                self._createProjectUI.pollImportJob(
                    start,
                    self._jobID,
                    timerID,
                    function(job) {
                    return "projectID" in job.config;
                    },
                    function(jobID, job) {
                    window.clearInterval(timerID);
                    Refine.CreateProjectUI.cancelImportingJob(jobID);
                    document.location = "project?project=" + job.config.projectID;
                    },
                    function(job) {
                    alert(Refine.CreateProjectUI.composeErrorMessage(job));
                    }
                );
            },
            1000
            );
            self._createProjectUI.showImportProgressPanel($.i18n('commons-import/creating'), function() {
            // stop the timed polling
            window.clearInterval(timerID);

            // explicitly cancel the import job
            Refine.CreateProjectUI.cancelImportingJob(jobID);

            delete self._jobID;
            delete self._options;

            self._createProjectUI.showSourceSelectionPanel();
            });
        }
        },
        "json"
    );
  });
};
