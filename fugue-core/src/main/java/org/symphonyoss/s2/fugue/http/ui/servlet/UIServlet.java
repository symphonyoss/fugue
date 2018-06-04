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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.slf4j.Logger;

public class UIServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private static final Logger log_ = LoggerFactory.getLogger(UIServlet.class);
  
  private static final String HTML_CONTENT_TYPE = "text/html; charset=utf-8";
  private UIServletResources resources_;
  
  public UIServlet(IResourceProvider provider)
  {
    resources_ = new UIServletResources(provider);
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    resp.setContentType(HTML_CONTENT_TYPE);
    
    UIHtmlWriter  out = new UIHtmlWriter(resp.getWriter());
    
    handlePage(req, resp, out);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    doGet(req, resp);
  }



  public void handlePage(HttpServletRequest req, HttpServletResponse resp, UIHtmlWriter out)
  {
    try
    {
      handleAction(req, out);
      handleHeader(req, out);
      handleBanner(req, out);
      handleLeftNav(req, out);
      handleBody(req, out);
      handleFooter(req, out);
    }
    catch(Exception e)
    {
      log_.error("Failed to process servlet", e);
    }
  }

  public void handleAction(HttpServletRequest req, UIHtmlWriter out)
  {
  }

  public void handleHeader(HttpServletRequest req, UIHtmlWriter out)
  {
    out.println(resources_.getHeaderHtml());
  }
  
  public void handleFooter(HttpServletRequest req, UIHtmlWriter out)
  {
    out.println(resources_.getFooterHtml());
  }
  
  public void handleBanner(HttpServletRequest req, UIHtmlWriter out)
  {
    out.println(resources_.getBannerHtml());
  }
  
  public void handleLeftNav(HttpServletRequest req, UIHtmlWriter out)
  {
    out.println(resources_.getLeftNavHeaderHtml());
    
    handleLeftNavContent(out);
    
    out.println(resources_.getLeftNavFooterHtml());
  }
  
  public void handleLeftNavContent(UIHtmlWriter out)
  {}
  
  public void handleBody(HttpServletRequest req, UIHtmlWriter out)
  {
    try
    {
      handleContent(req, out);
    } catch (Exception e)
    {
      out.printStackTrace(e);
      log_.error("Failed to process response", e);
    }
  }

  public void handleContent(HttpServletRequest req, UIHtmlWriter out) throws Exception
  {
    out.printError("BODY");
  }
}
