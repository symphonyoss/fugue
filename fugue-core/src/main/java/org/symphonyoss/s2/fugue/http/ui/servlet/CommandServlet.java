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
import java.io.PrintWriter;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.dom.json.JsonDom;

public class CommandServlet extends HttpServlet
{
  private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
  private static final long serialVersionUID = 1L;

  private static final Logger log_ = LoggerFactory.getLogger(CommandServlet.class);
  
  private final ICommandHandler handler_;

  public CommandServlet(ICommandHandler handler)
  {
    handler_ = handler;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    try
    {
      handler_.handle();
      respondOK(resp);
    }
    catch(Exception e)
    {
      respondInternalError(resp, "Command execution failed", e);
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException
  {
    doGet(req, resp);
  }
  
  protected void  respondOK(HttpServletResponse resp, JsonDom<?>... results) throws IOException
  {
    if(results.length==0)
    {
      resp.setStatus(HttpStatus.NO_CONTENT_204);
    }
    else
    {
      try(PrintWriter out = resp.getWriter())
      {
        for(JsonDom<?> result : results)
        {
          out.println(result.toString());
        }
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setStatus(HttpStatus.OK_200);
      }
    }
  }
  
  protected void  respondInternalError(HttpServletResponse resp, String message, Throwable cause) throws IOException
  {
    UUID logId = UUID.randomUUID();
    
    log_.error(message + " - " + logId, cause);
    
    try(PrintWriter out = resp.getWriter())
    {
      out.println("{\"errorId\": " + logId + ", \"message\": " + message);
  
      resp.setContentType(JSON_CONTENT_TYPE);
      resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }
  }
}
