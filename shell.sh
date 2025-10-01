#!/bin/bash
set -e

BASE_DIR="/Users/maherkarim/Dev/qap-junit-5-plugin/src/main/java/com/mk/fx/qa/qap/junit"

# Create new package structure
mkdir -p "$BASE_DIR/core"
mkdir -p "$BASE_DIR/extension"
mkdir -p "$BASE_DIR/model"
mkdir -p "$BASE_DIR/store"

# Move files into the right directories
mv "$BASE_DIR/QAPUtils.java"              "$BASE_DIR/core/"
mv "$BASE_DIR/ExtensionUtil.java"         "$BASE_DIR/core/"
mv "$BASE_DIR/QAPLaunchIdGenerator.java"  "$BASE_DIR/core/"
mv "$BASE_DIR/TestCaseStatus.java"        "$BASE_DIR/core/"

mv "$BASE_DIR/QAPJunitExtension.java"            "$BASE_DIR/extension/"
mv "$BASE_DIR/IMethodInterceptor.java"           "$BASE_DIR/extension/"
mv "$BASE_DIR/QAPJunitMethodInterceptor.java"    "$BASE_DIR/extension/"
mv "$BASE_DIR/ILifeCycleEventCreator.java"       "$BASE_DIR/extension/"
mv "$BASE_DIR/QAPJunitLifeCycleEventCreator.java" "$BASE_DIR/extension/"
mv "$BASE_DIR/ITestEventCreator.java"            "$BASE_DIR/extension/"
mv "$BASE_DIR/QAPJunitTestEventsCreator.java"    "$BASE_DIR/extension/"
mv "$BASE_DIR/LifeCycleEvent.java"               "$BASE_DIR/extension/"

mv "$BASE_DIR/QAPBaseTestCase.java"       "$BASE_DIR/model/"
mv "$BASE_DIR/QAPTest.java"               "$BASE_DIR/model/"
mv "$BASE_DIR/QAPTestClass.java"          "$BASE_DIR/model/"
mv "$BASE_DIR/QAPHeader.java"             "$BASE_DIR/model/"
mv "$BASE_DIR/QAPJunitLaunch.java"        "$BASE_DIR/model/"
mv "$BASE_DIR/QAPJunitLifeCycleEvent.java" "$BASE_DIR/model/"
mv "$BASE_DIR/QAPTestParams.java"         "$BASE_DIR/model/"
mv "$BASE_DIR/QAPPropertiesLoader.java"   "$BASE_DIR/model/"

mv "$BASE_DIR/StoreManager.java"          "$BASE_DIR/store/"

# Update package declarations
find "$BASE_DIR/core"      -type f -name "*.java" -exec sed -i '' 's|package com.mk.fx.qa.qap.junit;|package com.mk.fx.qa.qap.junit.core;|' {} +
find "$BASE_DIR/extension" -type f -name "*.java" -exec sed -i '' 's|package com.mk.fx.qa.qap.junit;|package com.mk.fx.qa.qap.junit.extension;|' {} +
find "$BASE_DIR/model"     -type f -name "*.java" -exec sed -i '' 's|package com.mk.fx.qa.qap.junit;|package com.mk.fx.qa.qap.junit.model;|' {} +
find "$BASE_DIR/store"     -type f -name "*.java" -exec sed -i '' 's|package com.mk.fx.qa.qap.junit;|package com.mk.fx.qa.qap.junit.store;|' {} +

echo "✅ Classes moved and package declarations updated."
