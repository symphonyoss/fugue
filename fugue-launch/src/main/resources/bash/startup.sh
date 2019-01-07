#!/usr/bin/env bash

_term() { 
  echo "Terminating!" 
  kill "$child"
  (sleep 60 ; echo "Terminating with prejudice!" ; kill -9 $child) &
  
  echo waiting for child PID $child to terminate...
  wait "$child"
  echo Done.
}

trap _term SIGTERM
trap _term SIGINT
trap _term SIGQUIT



function start() {
    export GOOGLE_APPLICATION_CREDENTIALS=/tmp/google.json
    
    export FUGUE_INSTANCE=${HOSTNAME}
    
    echo ENVIRONMENT START-------------------------------------------------------------------------------------
    env
    echo ENVIRONMENT END---------------------------------------------------------------------------------------
    
    java_args=${FUGUE_JAVA_ARGS:--Xms256m -Xmx256m}
    echo "Starting services..."
    
    echo WORKSPACE is ${WORKSPACE}
    cd ${WORKSPACE}
    echo pwd
    pwd
    
    java -version
    
    echo java -cp "/maven/lib/*" -Dlog4j.configurationFile=/maven/log4j2.xml ${java_args} $1
    
    #java -cp "/maven/lib/*" "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005" -Dlog4j.configurationFile=/maven/log4j2.xml ${java_args} $1
    java -cp "/maven/lib/*" -Dlog4j.configurationFile=/maven/log4j2.xml ${java_args} $1 &

    child=$! 
    echo waiting for child PID $child
    wait "$child"
}

echo VERSION 1.2
start $*

# sleep 360
