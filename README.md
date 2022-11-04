Commons extension (name not final)
==================================

This is an OpenRefine extension for Wikimedia Commons.
It works with OpenRefine 3.6+.


Building it
-----------

Run     
```
mvn package
```

This creates a zip file in the `target` folder, which can then be [installed in OpenRefine](https://docs.openrefine.org/manual/installing#installing-extensions).

Developing it
-------------

To avoid having to unzip the extension in the corresponding directory every time you want to test it, you can also use another set up: simply create a symbolic link from your extensions folder in OpenRefine to the local copy of this repository.
You will still need to restart OpenRefine every time you make changes.

Releasing it
------------

- Open `pom.xml` and set the version to the desired version number, such as `<version>0.1.0</version>`
- Commit and push those changes
- Add a corresponding git tag, with `git tag -a v0.1.0 -m "Version 0.1.0"`
- Push the tag to GitHub: `git push --tags`
- Create the zip file for the release: `mvn package`
- Create a new release on GitHub at https://github.com/OpenRefine/CommonsExtension/releases/new, providing a release title (such as "Commons extension 0.1.0") and a description of the features in this release. Upload the zip file you generated at the previous step as an attachment (it can be found in the `target` subfolder of your local copy of the repository).
- Open `pom.xml` and set the version to the expected next version number, followed by `-SNAPSHOT`. For instance, if you just released 0.1.0, you could set `<version>0.1.1-SNAPSHOT</version>`
- Commit and push those changes.
