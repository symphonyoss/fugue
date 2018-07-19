#!/usr/bin/env bash

function start() {
    export GOOGLE_APPLICATION_CREDENTIALS=/tmp/google.json
    export S3_GOOGLE_CREDENTIALS="https://s3.${AWS_REGION}.amazonaws.com/bruce-${AWS_REGION}/config/symphony-gce-dev-c8cfc40a5f41.json"
    
    export FUGUE_INSTANCE=${HOSTNAME}
    
    echo ENVIRONMENT START-------------------------------------------------------------------------------------
    env
    echo ENVIRONMENT END---------------------------------------------------------------------------------------
    echo FUGUE_CONFIG START-------------------------------------------------------------------------------------
    cat ${FUGUE_CONFIG}
    echo FUGUE_CONFIG END---------------------------------------------------------------------------------------
    echo "Starting services..."
    
    echo WORKSPACE is ${WORKSPACE}
    cd ${WORKSPACE}
    echo pwd
    pwd
    
    echo java -cp "/maven/lib/*" "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" -Dlog4j.configurationFile=/maven/log4j2.xml $1
    
#     echo sleep...
#     sleep 360
#     echo wakeup
    
    java -cp "/maven/lib/*" "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" -Dlog4j.configurationFile=/maven/log4j2.xml $1
}

#echo "Initializing environment..."
#load_config_data
#generate_config_json

echo VERSION 1.1
start $*

# sleep 360
