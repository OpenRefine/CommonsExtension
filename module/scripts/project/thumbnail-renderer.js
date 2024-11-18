
/**
 Renders a thumbnail alongside a matched cell when the reconcilition service 
 is associated with the MediaInfo entities of a Wikibase instance.
 */
class ThumbnailReconRenderer extends ReconCellRenderer {

  constructor() {
    super();
    this.supportedExtensions = [
        'jpg', 'jpeg', 'png', 'gif', 'svg', 'tiff', 'ogv', 'pdf', 'djvu', 'webm'
    ];
    this.siteIriToMediaWikiRootUrl = new Map();
    var self = this;
    $.ajax({
       url: 'command/core/get-preference?' + $.param({ name: 'wikibase.manifests' }),
       success: function (data) {
         let wikibases = JSON.parse(data.value || '[]');
         for (let manifest of wikibases) {
           try {
            let api = manifest.mediawiki.api;
            let siteIri = null;
            if (manifest.entity_types && manifest.entity_types.mediainfo) {
              if (manifest.entity_types.mediainfo.site_iri) {
                siteIri = manifest.entity_types.mediainfo.site_iri;
              } else {
                siteIri = manifest.wikibase.site_iri;
              }
            }
            if (siteIri) {
              self.siteIriToMediaWikiRootUrl.set(siteIri, api.substr(0, api.length - 'w/api.php'.length));
            }
          } catch(error) {
            console.warn('Unsupported manifest format for Wikibase instance ' + manifest.mediawiki.name);
          }
        }
    }});
  }

  render(rowIndex, cellIndex, cell, cellUI) {
    var self = this;
    var divContent = document.createElement('div');
    var divContentRecon = $(divContent);
    var r = cell.r;
    if ( !r.service ) {
       return undefined;
    }

    var service = ReconciliationManager.getServiceFromUrl(r.service);
    var mediaWikiRootUrl = self.siteIriToMediaWikiRootUrl.get(service.identifierSpace);
    // if the reconciliation service is not associated with a Wikibase, defer to recon renderer
    if (!mediaWikiRootUrl) {
      return undefined;
    }

    // only display thumbnails for matched cells
    if (cell && 'r' in cell && cell.r.j === 'matched') {
      var match = cell.r.m;
      var a = $('<a></a>')
        .text(match.name)
        .attr("target", "_blank")
        .appendTo(divContentRecon);

      var bareFileName = match.name.includes('File:') ? match.name.substr('File:'.length).replaceAll(' ', '_') : match.name.replaceAll(' ', '_');
      var fileNameParts = bareFileName.split('.');
      var extension = fileNameParts[fileNameParts.length - 1].toLowerCase();
      if (!self.supportedExtensions.includes(extension)) {
        // defer to the standard recon renderer
        return undefined;
      }

      var imageUrl = self.getThumbnailUrl(mediaWikiRootUrl, bareFileName, 320);

      if (service && (service.view) && (service.view.url)) {
        a.attr("href", encodeURI(service.view.url.replace("{{id}}", match.id)));
      }

      $('<span></span>').appendTo(divContentRecon);
      var thumbnailDiv = $('<div></div>')
        .addClass('media-file-thumbnail-in-cell')
        .appendTo(divContentRecon);
      var image = $('<img />')
        .attr('src', imageUrl)
        .appendTo(thumbnailDiv);
      image.on('click', function(evt) {
        self.showFullScreenPreview(mediaWikiRootUrl, bareFileName);
      });
      $('<a></a>')
        .text($.i18n('core-views/choose-match'))
        .addClass('data-table-recon-action')
        .appendTo(divContentRecon)
        .on('click', function(evt) {
          self.doRematch(rowIndex, cellIndex, cell, cellUI);
      }); 

      return divContent;
    }
  
  }

  showFullScreenPreview(mediaWikiRootUrl, bareFileName) {
    var self = this;
    var imageUrl = self.getThumbnailUrl(mediaWikiRootUrl, bareFileName);
    let div = $('<div></div>')
        .addClass('media-file-full-screen-preview')
        .appendTo($('body'));
    let img = $('<img />')
        .attr('src', self.getThumbnailUrl(mediaWikiRootUrl, bareFileName, 1920))
        .appendTo(div);
    div.on('click', function(evt) {
       div.remove();
    });
  }

  /*
   Important: thumbnails take resources to generate, so it is worth sticking to the thumbnail sizes
   which are auto-generated at upload time:
   320, 640, 800, 1024, 1280, 1920, 2560, 2880
   
   See https://www.mediawiki.org/wiki/Requests_for_comment/Standardized_thumbnails_sizes
   */
  getThumbnailUrl(mediaWikiRootUrl, bareFileName, width) {
     return `${mediaWikiRootUrl}w/thumb.php?f=${encodeURIComponent(bareFileName)}&w=${width}&h=${width}`;
  }
}

CellRendererRegistry.addRenderer('thumbnail', new ThumbnailReconRenderer(), 'recon');
