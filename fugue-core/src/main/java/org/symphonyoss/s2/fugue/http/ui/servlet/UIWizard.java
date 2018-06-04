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

import javax.servlet.http.HttpServletRequest;

import org.symphonyoss.s2.common.exception.S2Exception;

public abstract class UIWizard extends UIPanel implements IUIPanel
{
  public static final String RESPONSE_PANEL_ID = "responsePanelId";

  public static final String ACTION = "action";
  
  private List<IWizardPanel>  panelList_ = new ArrayList<>();
  
  public UIWizard(String name)
  {
    super(name);
  }

  public UIWizard(String id, String name)
  {
    super(id, name);
  }

  @Override
  public void handleContent(HttpServletRequest httpServletRequest, UIHtmlWriter out)
  {
    try
    {
      WizardRequest req = new WizardRequest(httpServletRequest, panelList_);
      
      if(req.getResponsePanel() != null)
        req.getResponsePanel().handleResponse(req);
      
      
      if(req.getErrors().isEmpty())
      {
        if(req.getContentPanel() == null)
          finish(req, out);
        else
        {
          handleContent(req, out, req.getContentPanel());
        }
      }
      else
      {
        for(String error : req.getErrors())
        {
          out.printError(error);
        }
        handleContent(req, out, req.getResponsePanel());
      }
    }
    catch(S2Exception e)
    {
      out.printError(e.getMessage());
    }
  }

  private void handleContent(WizardRequest req, UIHtmlWriter out, IWizardPanel panel)
  {
    try
    {
      out.openForm(getPath());
      
      panel.handleContent(req, out);
    }
    finally
    {
      int panelId = req.getPanelIdOf(panel);
      
      out.printHiddenInput(RESPONSE_PANEL_ID, String.valueOf(panelId));
      
      printButton(req, out, UIWizardPanelAction.Cancel, getPath());
      printButton(req, out, UIWizardPanelAction.Prev,
          (panelId>0 ? getPath() + "/" + (panelId - 1) : null));
      printButton(req, out, UIWizardPanelAction.Next,
          (panelId<panelList_.size()-1 ? getPath() + "/" + (panelId + 1) : null));
      printButton(req, out, UIWizardPanelAction.Finish,
          (getPath() + "/" + UIWizardPanelAction.Finish));
      
      out.closeElement(); // form
    }
  }

  public abstract void finish(WizardRequest req, UIHtmlWriter out);

  private void printButton(WizardRequest req, UIHtmlWriter out, UIWizardPanelAction action, String path)
  {
    if(path == null)
      out.openElement("button", "class", "w3-btn", "disabled", "disabled");
    else
      out.openElement("button", "name", ACTION, "value", action.toString(), "class", "w3-btn", "formaction",
          req.getHttpServletRequest().getServletPath() + path);
    
    out.println(action.getLabel());
    out.closeElement();
  }

  public void addPanel(IWizardPanel panel)
  {
    panelList_.add(panel);
  }
}