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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.symphonyoss.s2.fugue.IFugeComponentContainer;
import org.symphonyoss.s2.fugue.IFugueLifecycleComponent;


public class StatusPanel extends UIPanel implements IUIPanel
{
  private final IFugeComponentContainer<?> app_;

  public StatusPanel(IFugeComponentContainer<?> app)
  {
    super("Status");
    app_ = app;
  }
  

  @Override
  public void handleContent(HttpServletRequest req, UIHtmlWriter out)
  {
    out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
    
    out.openElement("tr");
    out.printElement("td", "Component ID");
    out.printElement("td", "Lifecycle State");
    out.printElement("td", "Status");
    out.printElement("td", "Message");
    out.closeElement(); //tr
    
    printComponents(out, app_.getLifecycleComponents());
    
    out.closeElement(); // table
  }

  private void printComponents(UIHtmlWriter out, List<IFugueLifecycleComponent> lifecycleComponents)
  {
    for(IFugueLifecycleComponent component : lifecycleComponents)
    {
      printComponent(out, component);
      
      if(component instanceof IFugeComponentContainer)
      {
        printComponents(out, ((IFugeComponentContainer) component).getLifecycleComponents());
      }
    }
  }


  private void printComponent(UIHtmlWriter out, IFugueLifecycleComponent component)
  {
    out.openElement("tr");
    out.printElement("td", component.getComponentId());
    out.printElement("td", component.getLifecycleState());
    out.printElement("td", component.getComponentState());
    out.printElement("td", component.getComponentStatusMessage());
    out.closeElement(); //tr
  }
}
