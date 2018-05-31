/*
 *
 *
 * Copyright 2017-2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The SSF licenses this file
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

package org.symphonyoss.s2.fugue.http.ui.servlet;

public abstract class UIPanel implements IUIBasePanel
{
  private final String id_;
  private final String name_;
  private IUIPanel  parent_;
  
  public UIPanel(String id, String name)
  {
    id_ = id;
    name_ = name;
  }
  
  public UIPanel(String name)
  {
    id_ = name;
    name_ = name;
  }

  @Override
  public String getName()
  {
    return name_;
  }

  @Override
  public String getId()
  {
    return id_;
  }

  @Override
  public IUIPanel getParent()
  {
    return parent_;
  }

  @Override
  public void setParent(IUIPanel parent)
  {
    parent_ = parent;
  }
  
  @Override
  public String getPath()
  {
    if(parent_ == null)
      return "/" + id_;
    else
      return parent_.getPath() + "/" + id_;
  }

}
