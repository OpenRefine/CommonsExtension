#!/bin/bash

# This runs the Cypress tests in headless mode, for the CI

set -e

EXTENSION_NAME="commons"
CYPRESS_BROWSER="chrome"
CYPRESS_SPECS="cypress/e2e/**/*.cy.js"
OPENREFINE_DIR="/tmp/openrefine_CommonsExtension"

# Detect target OpenRefine version
cd "$(dirname "$0")/.."
EXTENSION_DIR=`pwd`
OPENREFINE_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=openrefine.version -q -DforceStdout)

echo "Setting up OpenRefine $OPENREFINE_VERSION"
cd "$(dirname "$0")"
rm -rf $OPENREFINE_DIR
echo "Downloading OpenRefine"
wget -N --quiet https://github.com/OpenRefine/OpenRefine/releases/download/$OPENREFINE_VERSION/openrefine-linux-$OPENREFINE_VERSION.tar.gz
echo "Decompressing archive"
tar -zxf openrefine-linux-$OPENREFINE_VERSION.tar.gz
mv openrefine-$OPENREFINE_VERSION $OPENREFINE_DIR
mkdir -p $OPENREFINE_DIR/webapp/extensions
cd $OPENREFINE_DIR/webapp/extensions
ln -s $EXTENSION_DIR $EXTENSION_NAME
cd $OPENREFINE_DIR

# Start OpenRefine
echo "Starting OpenRefine"
./refine -x refine.headless=true -x refine.autoreload=false -x butterfly.autoreload=false -d /tmp/openrefine_commons_cypress > /dev/null &
REFINE_PID="$!"

# Wait for OpenRefine to start
sleep 5
if curl -s http://127.0.0.1:3333/ | grep "<title>OpenRefine</title>" > /dev/null 2>&1; then
   echo "OpenRefine is running"
else
   echo "Waiting for OpenRefine to start..."
   sleep 10
fi

# Launch Cypress
cd $EXTENSION_DIR/cypress
if yarn run cypress run --browser $CYPRESS_BROWSER --spec "$CYPRESS_SPECS" --headless --quiet; then
   echo "All Cypress tests passed"
   RETURN_VALUE=0
else
   echo "Cypress tests failed"
   RETURN_VALUE=1
fi

# Kill OpenRefine
kill $REFINE_PID

exit $RETURN_VALUE
