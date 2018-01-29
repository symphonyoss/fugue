#!/usr/bin/env bash
# distribution environment setup, replaces src/main/bash/environment.sh
# in distribution package

baseDir=`dirname $scriptDir`

#java_classpath="$baseDir/lib/*"

gcpProject=${gcp.project}
projectVersion=${project.version}
projectName=${project.name}
