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

public class WizardRequest
{
  private final HttpServletRequest httpServletRequest_;
  private IWizardPanel             responsePanel_;
  private IWizardPanel             contentPanel_;
  private List<String>             errors_ = new ArrayList<>();
  private int                      panelId_;
  private List<IWizardPanel>       panelList_;
  private UIWizardPanelAction      action_;

  public WizardRequest(HttpServletRequest httpServletRequest, List<IWizardPanel> panelList) throws S2Exception
  {
    httpServletRequest_ = httpServletRequest;
    panelId_ = 0;
    panelList_ = panelList;
    
    String info = httpServletRequest_.getPathInfo();
    String[] path = info == null ? new String[0] : info.split("/");
    
    try
    {
      int responsePanelId = Integer.parseInt(httpServletRequest.getParameter(UIWizard.RESPONSE_PANEL_ID));
      
      if(responsePanelId>=0 && responsePanelId<panelList.size())
        responsePanel_ = panelList.get(responsePanelId);
    }
    catch(NumberFormatException e)
    {}
    
    String a = httpServletRequest.getParameter(UIWizard.ACTION);
    
    if(a!=null)
      action_ = UIWizardPanelAction.valueOf(a);
    
    if(action_ == null)
      action_= UIWizardPanelAction.NoOp;
    
    if(path.length > 2)
    {
      if(UIWizardPanelAction.Finish.toString().equals(path[2]))
      {
        action_ = UIWizardPanelAction.Finish;
        return;
      }
      try
      {
        panelId_ = Integer.parseInt(path[2]);
      }
      catch(NumberFormatException e)
      {
        throw new S2Exception("\"" + path[2] + "\" is not a valid panel number");
      }
      
      if(panelId_ < 0 || panelId_ >= panelList.size())
      {
        throw new S2Exception("Panel ID out of range (" + panelId_ + " passed, max=" + panelList.size() + ")");
      }
    }
    contentPanel_ = panelList.get(panelId_);
  }

  public HttpServletRequest getHttpServletRequest()
  {
    return httpServletRequest_;
  }

  public IWizardPanel getResponsePanel()
  {
    return responsePanel_;
  }

  public IWizardPanel getContentPanel()
  {
    return contentPanel_;
  }

  public List<String> getErrors()
  {
    return errors_;
  }

  public int getPanelId()
  {
    return panelId_;
  }
  
  public WizardRequest addError(String error)
  {
    errors_.add(error);
    return this;
  }

  public void insertPanel(IWizardPanel next)
  {
    panelList_.add(panelId_, next);
    contentPanel_ = next;
  }
  
  public IWizardPanel getNextPanel()
  {
    if(panelId_ < panelList_.size() - 1)
      return panelList_.get(panelId_ + 1);
    
    return null;
  }

  public int getPanelIdOf(IWizardPanel panel)
  {
    int panelId = panelList_.indexOf(panel);
    
    if(panelId == -1)
     return 0;
    
    return panelId;
  }

  public UIWizardPanelAction getAction()
  {
    return action_;
  }

//  public void insertPanel(IWizardPanel before, IWizardPanel next)
//  {
//    for(int i=0 ; i<panelList_.size() ; i++)
//    {
//      if(panelList_.get(i) == before)
//        panelList_.add(i, next);
//    }
//  }
}
