# Wikimedia Commons Extension for OpenRefine
<img align="right" width="160" src="https://upload.wikimedia.org/wikipedia/commons/4/4a/Commons-logo.svg">
This extension provides several helpful functionalities for OpenRefine users who want to edit (structured data of) **media files** (images, videos, PDFs...) on **[Wikimedia Commons](https://commons.wikimedia.org)**. For more info, documentation and how-tos about OpenRefine for Wikimedia Commons, see **https://commons.wikimedia.org/wiki/Commons:OpenRefine**.

Features included in this extension:
* Start an OpenRefine project by loading file names from one or more **Wikimedia Commons categories** (including category depth)
* Add **columns** with Commons categories and/or M-ids of each file name
* File names will already be **reconciled** when starting the project
* A few dedicated **GREL commands** allow basic processing and extraction of Wikitext: `extractFromTemplate` and `value.extractCategories`

It works with **OpenRefine 3.6.x and later versions of OpenRefine**. It is not compatible with OpenRefine 3.5.x or earlier. *(OpenRefine supports editing Wikimedia Commons from version 3.6; this is not possible in earlier versions.)*

*This extension was first released in October 2022. It has been funded by a [Wikimedia project grant](https://meta.wikimedia.org/wiki/Grants:Project/CS%26S/Structured_Data_on_Wikimedia_Commons_functionalities_in_OpenRefine).*

## How to use this extension

### Install this extension in OpenRefine

Download the .zip file of the [latest release of this extension](https://github.com/OpenRefine/CommonsExtension/releases).
Unzip this file and place the unzipped folder in your OpenRefine extensions folder. [Read more about installing extensions in OpenRefine's user manual](https://docs.openrefine.org/manual/installing#installing-extensions).

<img width="600" src="https://upload.wikimedia.org/wikipedia/commons/2/26/OpenRefine_-_Commons_Extension_-_location_to_install.png">

When this extension is installed correctly, you will now see the additional option 'Wikimedia Commons' when starting a new project in OpenRefine. 

### Start an OpenRefine project from one or more Wikimedia Commons categories

After installing this extension, click the 'Wikimedia Commons' option to start a new project in OpenRefine. You will be prompted to add one or more [Wikimedia Commons categories](https://commons.wikimedia.org/wiki/Commons:Categories). 

<img src="https://upload.wikimedia.org/wikipedia/commons/5/53/OpenRefine_-_Commons_Extension_-_start_project_from_categories.png">

There's no need to type the Category: prefix. 

You can specify category depth by typing or selecting a number in the input field after each category. Depth `0` means only files from the current category level; depth `1` will retrieve files from one sub-category level down, etc.

Next, in the project preview screen (`Configure parsing options`), you can choose to also include a column with each file's M-id (unique [MediaInfo identifier](https://www.mediawiki.org/wiki/Extension:WikibaseMediaInfo#MediaInfo_Entity)) and/or Commons categories.

File names will already be reconciled when your project starts.

When you load larger categories (thousands of files) in a new project, OpenRefine will start slowly and will give you a memory warning. [This is a known issue](https://github.com/OpenRefine/CommonsExtension/issues/72). Wait for a bit; the project will eventually start. The Commons Extension has been tested with a project of more than 450,000 files.

### GREL commands to extract data from Wikitext

The Wikimedia Commons Extension also enables two dedicated GREL commands, which help to extract specific information from the Wikitext of Wikimedia Commons files. *(GREL, General Refine Expression Language, is a dedicated scripting language used in OpenRefine for many flexible data operations. For a general reference on using GREL in OpenRefine, see https://docs.openrefine.org/manual/grelfunctions.)*
 
Firstly, retrieve the Wikitext from a list of Commons files in your project. In the column menu of the reconciled file names' column, select `Edit column` > `Add column from reconciled values...` and select `Wikitext` in the resulting dialog window.

From this new column with Wikitext, you can now extract values and categories as described below. Start by selecting `Edit column` > `Add column based on this column...` in the column menu. In the next dialog window, you can use various specific GREL commands:

#### Extract values from template parameters: `extractFromTemplate`

<img width="600" src="https://upload.wikimedia.org/wikipedia/commons/b/be/OpenRefine_-_Commons_Extension_-_GREL_extractFromTemplate.png">

Use the following syntax:

```
extractFromTemplate(value, "BHL", "source")[0]
```

where you replace `BHL` with the name of the template (without curly brackets) and `source` with the parameter from which you want to extract the value. This GREL syntax will return the first (and usually the only) value of said parameter, e.g. `https://www.flickr.com/photos/biodivlibrary/10329116385`.

#### Extract Wikimedia Commons categories: `value.extractCategories`

<img width="600" src="https://upload.wikimedia.org/wikipedia/commons/0/0d/OpenRefine_-_Commons_Extension_-_GREL_value.extractCategories.png">

Use the following syntax:

```
value.extractCategories().join('#')
```

This GREL syntax will return all categories mentioned in the Wikitext, separated by the `#` character, which you can then use to split the resulting cell further as needed.

## Development

### Building from source

Run     
```
mvn package
```

This creates a zip file in the `target` folder, which can then be [installed in OpenRefine](https://docs.openrefine.org/manual/installing#installing-extensions).

### Developing it

To avoid having to unzip the extension in the corresponding directory every time you want to test it, you can also use another set up: simply create a symbolic link from your extensions folder in OpenRefine to the local copy of this repository. With this setup, you do not need to run `mvn package` when making changes to the extension, but you will still to compile it with `mvn compile` if you are making changes to Java files, and restart OpenRefine if you make changes to any files.

### Releasing it

- Make sure you are on the `master` branch and it is up to date (`git pull`)
- Open `pom.xml` and set the version to the desired version number, such as `<version>0.1.0</version>`
- Commit and push those changes
- Add a corresponding git tag, with `git tag -a v0.1.0 -m "Version 0.1.0"` (when working from GitHub Desktop, you can follow [this process](https://docs.github.com/en/desktop/contributing-and-collaborating-using-github-desktop/managing-commits/managing-tags) and manually add the `v0.1.0` and `Version 0.1.0` tags)
- Push the tag to GitHub: `git push --tags` (in GitHub Desktop, just push again)
- Create the zip file for the release: `mvn package`
- Create a new release on GitHub at https://github.com/OpenRefine/CommonsExtension/releases/new, providing a release title (such as "Commons extension 0.1.0") and a description of the features in this release. Upload the zip file you generated at the previous step as an attachment (it can be found in the `target` subfolder of your local copy of the repository).
- Open `pom.xml` and set the version to the expected next version number, followed by `-SNAPSHOT`. For instance, if you just released 0.1.0, you could set `<version>0.1.1-SNAPSHOT</version>`
- Commit and push those changes.
