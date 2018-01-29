#!/usr/bin/env bash
# source environment setup, used only during development

mainDir=`dirname $scriptDir`
srcDir=`dirname $mainDir`
baseDir=`dirname $srcDir`

#java_classpath="${baseDir}/target/classes:${baseDir}/target/lib/*"

gcp_project=sym-dev-arch
