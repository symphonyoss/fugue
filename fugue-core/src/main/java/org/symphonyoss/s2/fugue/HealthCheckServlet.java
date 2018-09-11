/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.http.IUrlPathServlet;

public class HealthCheckServlet extends HttpServlet implements IUrlPathServlet
{
  private static final long serialVersionUID = 1L;
  private static final Logger log_ = LoggerFactory.getLogger(HealthCheckServlet.class);

  @Override
  public String getUrlPath()
  {
    return "/HealthCheck";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
  {
    log_.info("Reply OK to HealthCheck request from " + req.getRemoteAddr());
    System.err.println(new Date() + " Reply OK to HealthCheck request from " + req.getRemoteAddr());
    
    resp.getOutputStream().println("<!DOCTYPE html>\n" + 
        "<html>\n" + 
        "<title>Healthcheck</title>\n" + 
        "<body>\n" + 
        "All is well\n" + 
        "</body>\n" + 
        "</html>");
  }

  
}
