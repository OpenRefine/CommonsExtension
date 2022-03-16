Commons extension
=================

This is a skeleton of OpenRefine extension for Wikimedia Commons, in the form of an independent Maven project.
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

To avoid having to unzip the extension in the corresponding directory every time you want to test it, you can also use another set up: simply create a symbolic link from your extensions folder in OpenRefine to the local copy of this repository. With this setup, you do not need to run `mvn package` when making changes to the extension, but you will still to compile it with `mvn compile` if you are making changes to Java files, and restart OpenRefine if you make changes to any files.
