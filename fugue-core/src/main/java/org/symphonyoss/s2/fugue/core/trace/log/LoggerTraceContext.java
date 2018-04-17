/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.symphonyoss.s2.fugue.core.trace.log;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.hash.Hash;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;

class LoggerTraceContext implements ITraceContext
{
  private static final Logger log_ = LoggerFactory.getLogger(LoggerTraceContext.class);

  private static final String LONG_FORMAT = "TRACE|%s|%s|%s|%s|%s|%s|%s";
  private static final String SHORT_FORMAT = "TRACE|%s|%s|%s|%s|%s";

  private final UUID          id_  = UUID.randomUUID();
  private final UUID          parentId_;
  private final String        subjectType_;
  private final String        subjectId_;

  public LoggerTraceContext(String subjectType, String subjectId)
  {
    parentId_ = null;
    subjectType_ = subjectType;
    subjectId_ = subjectId;
  }
  
  private LoggerTraceContext(LoggerTraceContext parent, String subjectType, String subjectId)
  {
    parentId_ = parent.id_;
    subjectType_ = subjectType;
    subjectId_ = subjectId;
  }

  @Override
  public void trace(String operationId)
  {
    log_.debug(String.format(SHORT_FORMAT, id_, parentId_, subjectType_, subjectId_, operationId));
  }

  @Override
  public void trace(String operationId, String subjectType, Hash subjectHash)
  {
    log_.debug(String.format(LONG_FORMAT, id_, parentId_, subjectType_, subjectId_, operationId, subjectType, subjectHash));
  }

  @Override
  public ITraceContext createSubContext(String externalSubjectType, String externalSubjectId)
  {
    return new LoggerTraceContext(this, externalSubjectType, externalSubjectId);
  }
}
