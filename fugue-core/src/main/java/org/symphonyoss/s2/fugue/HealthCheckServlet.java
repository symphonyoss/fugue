/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.symphonyoss.s2.fugue.http.IUrlPathServlet;

public class HealthCheckServlet extends HttpServlet implements IUrlPathServlet
{
  private static final long serialVersionUID = 1L;

  @Override
  public String getUrlPath()
  {
    return "/HealthCheck";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    resp.getOutputStream().println("<!DOCTYPE html>\n" + 
        "<html>\n" + 
        "<title>Healthcheck</title>\n" + 
        "<body>\n" + 
        "This is index.html\n" + 
        "All is well\n" + 
        "</body>\n" + 
        "</html>");
  }

  
}
