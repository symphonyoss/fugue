#!/usr/bin/env bash

function start() {
    export GOOGLE_APPLICATION_CREDENTIALS=/tmp/google.json
    
    export FUGUE_INSTANCE=${HOSTNAME}
    
    echo ENVIRONMENT START-------------------------------------------------------------------------------------
    env
    echo ENVIRONMENT END---------------------------------------------------------------------------------------
#     echo FUGUE_CONFIG START-------------------------------------------------------------------------------------
#     cat ${FUGUE_CONFIG}
#     echo FUGUE_CONFIG END---------------------------------------------------------------------------------------
    echo "Starting services..."
    
    echo WORKSPACE is ${WORKSPACE}
    cd ${WORKSPACE}
    echo pwd
    pwd
    
    echo java -cp "/maven/lib/*" -Dlog4j.configurationFile=/maven/log4j2.xml -Xms256m -Xmx256m $1
    
    #java -cp "/maven/lib/*" "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" -Dlog4j.configurationFile=/maven/log4j2.xml $1
    java -cp "/maven/lib/*" -Dlog4j.configurationFile=/maven/log4j2.xml -Xms256m -Xmx256m $1
}

echo VERSION 1.1
start $*

# sleep 360
