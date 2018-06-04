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

package org.symphonyoss.s2.fugue;

/**
 * A manageable component of a FugeServer.
 *
 * @author Bruce Skingle
 */
public interface IFugueComponent
{
  /**
   * Start method called after all configuration is complete and the server is starting normal operation.
   * 
   * Components will be started in the order in which they are registered with the server.
   */
  void start();
  
  /**
   * Stop method called prior to server shutdown.
   * 
   * Components will be stopped in the reverse order to that in which they were started.
   */
  void stop();
  
  default FugueComponentState getComponentState()
  {
    return FugueComponentState.Warn;
  }
  
  default String getComponentStatusMessage()
  {
    return getClass().getSimpleName() + " does not implement IFugeComponent.getComponentStatusMessage()";
  }
  
  default String getComponentId()
  {
    return getClass().getSimpleName();
  }
}
