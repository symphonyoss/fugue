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

import org.symphonyoss.s2.fugue.di.component.ILogComponent;
import org.symphonyoss.s2.fugue.di.impl.ComponentDescriptor;

public class DefaultLogComponent implements ILogComponent
{
  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addProvidedInterface(ILogComponent.class)
        .addStart(() -> info("Default log component started."))
        .addStop(() -> info("Default log component stopped."));
  }

  private  void log(Object message, Throwable cause)
  {
    System.err.println(message);
    if(cause != null)
      cause.printStackTrace();
  }
  
  @Override
  public void error(Object message)
  {
    error(message, null);
  }
  
  @Override
  public void error(Object message, Throwable cause)
  {
    log("ERROR: " + message, cause);
  }

  @Override
  public void warn(Object message)
  {
    warn(message, null);
  }
  
  @Override
  public void warn(Object message, Throwable cause)
  {
    log("WARN:  " + message, cause);
  }

  @Override
  public void info(Object message)
  {
    info(message, null);
  }
  
  @Override
  public void info(Object message, Throwable cause)
  {
    log("INFO:  " + message, cause);
  }

  @Override
  public void debug(Object message)
  {
    debug(message, null);
  }
  
  @Override
  public void debug(Object message, Throwable cause)
  {
    log("DEBUG: " + message, cause);
  }

}
