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

import java.util.ArrayList;
import java.util.List;

public class WizardPanel extends UIPanel implements IWizardPanel
{
  private List<IPanelElement>   panelParameterList_ = new ArrayList<>();
  
  public WizardPanel(String id, String name, IPanelElement ...params)
  {
    super(id, name);
    
    for(IPanelElement panelParameter : params)
      panelParameterList_.add(panelParameter);
  }

  public WizardPanel(String name, PanelParameter<?> ...params)
  {
    super(name);
    
    for(IPanelElement panelParameter : params)
      panelParameterList_.add(panelParameter);
  }

//  public WizardPanel(IComponent configComponent)
//  {
//    this(getId(configComponent), getName(configComponent));
//  }
//
//  private static String getName(IComponent configComponent)
//  {
//    String id = getId(configComponent);
//    
//    return id.startsWith("Boot") ? id.substring(4) : id;
//  }
//
//  private static String getId(IComponent configComponent)
//  {
//    return configComponent.getClass().getSimpleName();
//  }

  public WizardPanel add(IPanelElement panelParameter)
  {
    panelParameterList_.add(panelParameter);
    
    return this;
  }

  @Override
  public void handleResponse(WizardRequest req)
  {
    if(panelParameterList_.isEmpty())
      req.addError("No Panel Parameters - did you mean to implement void handleResponse(WizardRequest req)?");
    else
    {
      for(IPanelElement panelParameter : panelParameterList_)
      {
        panelParameter.handleResponse(req);
      }
    }
  }

  @Override
  public void handleContent(WizardRequest req, UIHtmlWriter out)
  {
    if(panelParameterList_.isEmpty())
      req.addError("No Panel Parameters - did you mean to implement void handleContent(WizardRequest req, UIHtmlWriter out)?");
    else
    {
      out.printElement("h1", getName());
      out.openElement("table");
      
      for(IPanelElement p : panelParameterList_)
      {
        p.handleContent(req, out);
      }
      out.closeElement(); // table
    }
  }
}
