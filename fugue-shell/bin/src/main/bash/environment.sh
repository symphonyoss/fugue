#!/usr/bin/env bash
# ----------------------------------------------------------------------------
# Copyright 2016-2018 Symphony Communication Services, LLC.
# 
# Licensed to The Symphony Software Foundation (SSF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The SSF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# -----------------------------------------------------------------------------

mainDir=`dirname $scriptDir`
srcDir=`dirname $mainDir`
baseDir=`dirname $srcDir`

FUGUE_DEFAULT=~/.fugue

function prompt()
{
  local  __resultvar=$1
  local  __default=$2
  local  __input
  
  if [[ "${!__resultvar}" != "" ]]
  then 
    __default="${!__resultvar}"
  fi
    
  echo -n "Enter $1 [$__default]:"
  read __input
  
  if [[ "${__input}" == "" ]]
  then
    eval $__resultvar="'$__default'"
  else
    eval $__resultvar="'$__input'"
  fi
}

function loadProperties()
{
  configFile=$1

  if [[ -f $configFile ]]
  then

      if [[ "$FUGUE_DEBUG" == true ]]
      then
        echo FUGUE_DEBUG is $FUGUE_DEBUG
        echo -------------------------------------------------------
        echo loading config from ${configFile}...
      fi


      while read line || [ -n "$line" ]
      do
          if ! [[ $line =~ ^([\t]*#) ]]; then
              if ! [[ $line =~ ^([\t]*)$ ]]; then


                  name=`echo $line | sed -e 's/ *=.*//' -e 's/\./_/g'`
                  eval value=\$$name

                  if [[ "$force" == true || "$value" == "" ]]
                  then
                      value=`echo $line | sed -e 's/[^=]*= *//' -e 's/\(\${[^.}]*\)\./\1_/g'`


                      while [[ $value =~ (\$\{[^.}]*\.) ]]
                      do
                          value=`echo $value | sed -e 's/\(\${[^.}]*\)\./\1_/g'`
                      done
                      eval "export $name=$value"

                      if [[ "${FUGUE_DEBUG}" == "true" ]]
                      then
                          echo $name = $value
                      fi
                  else
                      if [[ "${FUGUE_DEBUG}" == "true" ]]
                      then
                          echo $name ALREADY SET to $value
                      fi
                  fi
              fi
          fi

      done < ${configFile}
  fi
}

function showConfig()
{
  for n in CLOUD PROJECT
  do
    name=FUGUE_${n}
    echo ${name} = ${!name}
  done
}

if [[ "${FUGUE_HOME}" == "" ]]
then
    if [[ ! -e ${FUGUE_DEFAULT} ]]
    then
        if mkdir -p ${FUGUE_DEFAULT}
        then
          if [[ "$FUGUE_DEBUG" == true ]]
          then
              echo Created FUGUE_HOME as ${FUGUE_DEFAULT}
          fi
        else
              cat <<EOF
FUGUE_HOME is not set and I cannot create a directory ${FUGUE_DEFAULT}
Aborting.
EOF
            exit 1
        fi
    fi
    if [[ -d ${FUGUE_DEFAULT} ]]
    then
        export FUGUE_HOME=${FUGUE_DEFAULT}
        if [[ "$FUGUE_DEBUG" == true ]]
        then
            echo FUGUE_HOME defaults to ${FUGUE_HOME}
        fi
    else
        cat <<EOF
Cannot determine value for FUGUE_HOME, set environment variable \${FUGUE_HOME}
or create a directory ${FUGUE_DEFAULT}
Aborting.
EOF
            exit 1
    fi
fi

if ! [[ -d ${FUGUE_HOME} ]]
then
    echo FUGUE_HOME ${FUGUE_HOME} does not exist, aborting.
    exit 1
fi

if ! [[ -r ${FUGUE_HOME} ]]
then
    echo FUGUE_HOME ${FUGUE_HOME} is not readable, aborting.
    exit 1
fi

loadProperties ${FUGUE_HOME}/config.properties

if [[ "$requiredConfig" == "all" ]]
then
  requiredConfig="CLOUD PROJECT IMAGE IMAGE_VERSION ENV"
fi

for m in $requiredConfig
do
  n=FUGUE_$m
  case "$n" in
    FUGUE_CLOUD)
      prompt FUGUE_CLOUD google
      export FUGUE_CLOUD
      ;;
    
    FUGUE_PROJECT)
      prompt FUGUE_PROJECT myproject
      export FUGUE_PROJECT
      ;;
      
    *)
      prompt $n ""
      export $n
  esac
done



if [[ "${FUGUE_DEBUG}" == "true" ]]
then
    echo FUGUE_CLOUD is ${FUGUE_CLOUD}
    echo FUGUE_PROJECT is ${FUGUE_PROJECT}
    
    echo Environment =================================================================
    env | sort
    echo =============================================================================
fi