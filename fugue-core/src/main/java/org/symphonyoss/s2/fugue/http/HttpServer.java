/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.http;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.ProgramFault;
import org.symphonyoss.s2.fugue.AbstractFugueComponent;
import org.symphonyoss.s2.fugue.FugueComponentState;

public class HttpServer extends AbstractFugueComponent
{
  private static final Logger log_ = LoggerFactory.getLogger(HttpServer.class);
  
  private final Server                jettyServer_;
  private final ServletContextHandler servletContextHandler_ = new ServletContextHandler();
  
  /* package */ HttpServer()
  {
    jettyServer_ = new Server();
  }
  
  /* package */ HttpServer(int httpPort)
  {
    jettyServer_ = new Server(httpPort);
  }
  
  public Server getJettyServer()
  {
    return jettyServer_;
  }

  public void start()
  {    
    try
    {
      log_.info("Starting S2HttpServer...");
      
      jettyServer_.start();
      setComponentStatusOK();
    } catch (Exception e) // Jetty throws Exception
    {
      setComponentStatus(FugueComponentState.Failed, "Jetty initialization failed (%s)", e);
      throw new ProgramFault(e);
    }
  }

  public void stop()
  {
    try
    {
      log_.info("Stopping S2HttpServer...");
      jettyServer_.stop();
      setComponentStatus(FugueComponentState.Warn, "Shutdown");
    } catch (Exception e) // Jetty throws Exception
    {
      setComponentStatus(FugueComponentState.Failed, "Jetty stop failed (%s)", e);
      throw new ProgramFault(e);
    }
  }

  public void join() throws InterruptedException
  {
    jettyServer_.join();
  }

  public ServletContextHandler getServletContextHandler()
  {
    return servletContextHandler_;
  }

  public int getLocalPort()
  {
    Connector[] c = getJettyServer().getConnectors();
    
    return ((ServerConnector)c[0]).getLocalPort();
  }
}
