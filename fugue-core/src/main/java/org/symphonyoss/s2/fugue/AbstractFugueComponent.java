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
 * An abstract super-class for FugueComponents, which handles the status parts of IFugueComponent.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class AbstractFugueComponent implements IFugueComponent
{
  private final String id_;
  
  private FugueComponentState componentState_;
  private String statusMessage_;
  
  /**
   * Create a component with the default status and component ID as the name of the class. 
   */
  public AbstractFugueComponent()
  {
    id_ = getClass().getSimpleName();
    componentState_ = FugueComponentState.OK;
    statusMessage_ = "";
  }
  
  /**
   * Create a component with the default status and the given component ID.
   * 
   * @param id The component ID.
   */
  public AbstractFugueComponent(String id)
  {
    id_ = id;
    componentState_ = FugueComponentState.OK;
    statusMessage_ = "";
  }
  
  /**
   * Create a component with the given ID and status.
   *  
   * @param id                The component ID.
   * @param componentState    The initial componentState.
   * @param statusMessage     The initial status message.
   */
  public AbstractFugueComponent(String id, FugueComponentState componentState, String statusMessage)
  {
    id_ = id;
    componentState_ = componentState;
    statusMessage_ = statusMessage;
  }

  @Override
  public FugueComponentState getComponentState()
  {
    return componentState_;
  }

  @Override
  public String getComponentStatusMessage()
  {
    return statusMessage_;
  }

  @Override
  public String getComponentId()
  {
    return id_;
  }

  /**
   * Set the component status.
   * 
   * @param state   The component state.
   * @param message A status message.
   * @param args    Optional varargs parameters for the status message, if present then the message is passed as the
   *                format string to String.format()
   */
  protected void setComponentStatus(FugueComponentState state, String message, Object ...args)
  {
    componentState_ = state;
    
    if(args.length==0)
      statusMessage_ = message;
    else
      statusMessage_ = String.format(message, args);
  }
  
  /**
   * Set the component status to OK and clear the status message.
   */
  protected void setComponentStatusOK()
  {
    componentState_ = FugueComponentState.OK;
    statusMessage_ = "";
  }
}
