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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.symphonyoss.s2.fugue.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.FugueLifecycleState;
import org.symphonyoss.s2.fugue.IFugeComponentContainer;



public class StatusServlet extends UIServlet implements IUrlPathServlet, IUIPanelContainer
{
  private static final long       serialVersionUID   = 1L;

  private static final String     PLAIN_CONTENT_TYPE = "text/plain";

  private static final String BasePath = "/fugue";
  private static final String PathPrefix = BasePath + "/";

  private Map<String, ICommand>   commandMap_        = new TreeMap<>();
  private Map<String, IUIPanel>   panelMap_          = new HashMap<>();
  private List<IUIPanel>          panelList_         = new ArrayList<>();
  private IUIPanel                defaultPanel_;
  private IUIPanel                statusPanel_;

  private IFugeComponentContainer<?> app_;

  public StatusServlet(IResourceProvider provider, IFugeComponentContainer<?> app)
  {
    super(provider);

    app_ = app;
    statusPanel_        = new StatusPanel(app);

    panelMap_.put(statusPanel_.getId(), statusPanel_);
    addCommand(new Command("Refresh", "#", null));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    String pathInfo = req.getPathInfo();
    
    if (pathInfo==null)
    {
      super.doGet(req, resp);
    } else
    {
      String[] path = pathInfo.split("/");
      
      IUIPanel panel = panelMap_.get(path[1]);
      
      if(panel == null)
      {
        resp.setStatus(404);
        resp.setContentType(PLAIN_CONTENT_TYPE);
  
        resp.getWriter().println("Invalid URL");
        // resp.sendError(404);
      }
      else
      {
        req.setAttribute("panel", panel);
        super.doGet(req, resp);
      }
    }
  }

  @Override
  public void handleLeftNavContent(UIHtmlWriter out)
  {
    out.println("<ul>");

    if(defaultPanel_ != null)
    {
       printNav(out, defaultPanel_);
    }
    printNav(out, statusPanel_);

    for(IUIPanel p : panelList_)
    {
      if(p.getName() != null)
        printNav(out, p);
    }
    out.println("</ul>");
  }

  private void printNav(UIHtmlWriter out, IUIPanel p)
  {
    out.openElement("li", "class=", "w3-padding");
    out.openElement("a", "href", PathPrefix + p.getId());
    out.print(p.getName());
    out.closeElement(); // a
    out.closeElement(); // li
  }

  
  @Override
  public void handleContent(HttpServletRequest req, UIHtmlWriter out)
  {
    IUIPanel p = null;

    Object a = req.getAttribute("panel");
    
    if(a instanceof IUIPanel)
      p = (IUIPanel) a;
    
    if(p == null)
    {
      String panel = req.getParameter("panel");
      
          
      if(panel != null)
        p = panelMap_.get(panel);
    }
    
    if(p == null)
      p = defaultPanel_ == null ? statusPanel_ : defaultPanel_;
    
    p.handleContent(req, out);
  
    FugueLifecycleState lifecycleState = app_.getLifecycleComponents().get(0).getLifecycleState();

    out.openElement("p");

    for (ICommand c : commandMap_.values())
    {
      if (c.getValidStates().contains(lifecycleState))
      {
        printCommand(out, c);
      }
    }
    out.closeElement(); // p
  }

  private void printCommand(UIHtmlWriter out, ICommand command)
  {
    out.openElement("form", "method", "GET", "action", command.getPath(), "class", "commandForm");
    out.println(
        "<button class=\"w3-btn\">" + command.getName() + " &nbsp;<i class=\"fa fa-arrow-right\"></i></button>");
    out.closeElement(); // form
  }

  @Override
  public String getUrlPath()
  {
    return PathPrefix + "*" ;
  }

  public void addCommand(ICommand command)
  {
    commandMap_.put(command.getName(), command);
  }

  @Override
  public IUIPanelContainer addPanel(IUIPanel panel)
  {
    panelMap_.put(panel.getId(), panel);
    panelList_.add(panel);
    
    return this;
  }

  @Override
  public IUIPanelContainer setDefaultPanel(IUIPanel panel)
  {
    if (defaultPanel_ != null)
      throw new IllegalStateException("Default panel already set to \"" + defaultPanel_.getName() + "\"");

    defaultPanel_ = panel;
    panelMap_.put(panel.getId(), panel);
    
    return this;
  }
}
