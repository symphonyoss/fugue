/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.di.component.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.component.ILogComponent;

/**
 * An SLF4J implementation of ILogComponent.
 * 
 * @author Bruce Skingle
 *
 */
public class Slf4jLogComponent implements ILogComponent
{
  private static Logger log_ = LoggerFactory.getLogger(Slf4jLogComponent.class);
  
  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addProvidedInterface(ILogComponent.class);
  }

  @Override
  public void debug(Object message, Throwable cause)
  {
    log_.debug(message.toString(), cause);
  }

  @Override
  public void debug(Object message)
  {
    log_.debug(message.toString());
  }

  @Override
  public void info(Object message, Throwable cause)
  {
    log_.info(message.toString(), cause);
  }

  @Override
  public void info(Object message)
  {
    log_.info(message.toString());
  }

  @Override
  public void warn(Object message, Throwable cause)
  {
    log_.warn(message.toString(), cause);
  }

  @Override
  public void warn(Object message)
  {
    log_.warn(message.toString());
  }

  @Override
  public void error(Object message, Throwable cause)
  {
    log_.error(message.toString(), cause);
  }

  @Override
  public void error(Object message)
  {
    log_.error(message.toString());
  }
}
