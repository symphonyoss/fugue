/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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

package org.symphonyoss.s2.fugue.core;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.http.HttpServer;
import org.symphonyoss.s2.common.http.HttpServerBuilder;
import org.symphonyoss.s2.common.http.IServletProvider;
import org.symphonyoss.s2.common.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.di.Cardinality;
import org.symphonyoss.s2.fugue.di.IComponent;
import org.symphonyoss.s2.fugue.di.IDIContext;
import org.symphonyoss.s2.fugue.di.impl.ComponentDescriptor;

public class FugueServer implements IComponent
{
  private static Logger                          log_ = LoggerFactory.getLogger(FugueServer.class);

  private final IDIContext                       diContext_;
  private final String                           name_;
  private final int                              httpPort_;
  private final ScheduledExecutorService         exec_;
  
//  private IResourcesService                      resourcesService_;
//  private IFundamentalService                    fundamentalService_;
//  private ISystemService                         systemModelService_;
//  private ISessionService                        sessionService_;

  private HttpServer                             server_;
//  private StatusServlet                          statusServlet_;
  private CopyOnWriteArrayList<IServletProvider> servletProviders_ = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<IUrlPathServlet>  servlets_         = new CopyOnWriteArrayList<>();

//  private IApplication                           application_;
  private boolean                                started_;
  private String                                 serverUrl_;

  
  public FugueServer(IDIContext diContext, String name, int httpPort)
  {
    diContext_ = diContext;
    name_ = name;
    httpPort_ = httpPort;
    exec_ = Executors.newScheduledThreadPool(0, new LocalThreadFactory());
  }
  
  public void shutdown()
  {
    log_.info("Shutting down...");
    diContext_.stop();
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
//        .addProvidedInterface(IUIPanelContainer.class)
//        .addDependency(IResourcesService.class,         (v) -> resourcesService_ = v)
//        .addDependency(IFundamentalService.class,       (v) -> fundamentalService_ = v)
//        .addDependency(ISystemService.class,            (v) -> systemModelService_ = v)
//        .addDependency(ISessionService.class,           (v) -> sessionService_ = v)
        .addDependency(IUrlPathServlet.class,           (v) -> bind(v), Cardinality.zeroOrMore)
        .addDependency(IServletProvider.class,          (v) -> bind(v), Cardinality.zeroOrMore)
        .addStart(() -> startFugueServer())
        .addStop(() -> stopFugueServer());
  }

  public synchronized void bind(IServletProvider servletProvider)
  {
    servletProviders_.addIfAbsent(servletProvider);
//    if(servletProviders_.addIfAbsent(servletProvider))
//    {
//      if(server_ != null)
//        servletProvider.registerServlets(server_);
//    }
  }
  
  public synchronized void bind(IUrlPathServlet servlet)
  {
    servlets_.addIfAbsent(servlet);
//    if(servlets_.addIfAbsent(servlet))
//    {
//      if(server_ != null)
//        server_.addServlet(servlet);
//    }
  }
  
//  public IResourcesService getResourcesService()
//  {
//    return resourcesService_;
//  }
//
//  public IFundamentalService getFundamentalModelService()
//  {
//    return fundamentalService_;
//  }
//  
//  public ISystemService getSystemModelService()
//  {
//    return systemModelService_;
//  }
//
//  public ISessionService getSessionService()
//  {
//    return sessionService_;
//  }
//
//  
//
//  @Override
//  public IUIPanelContainer getUIPanelContainer()
//  {
//    return statusServlet_;
//  }
//
//  @Override
//  public IUIPanelContainer addPanel(IUIPanel panel)
//  {
//    return statusServlet_.addPanel(panel);
//  }
//
//  @Override
//  public IUIPanelContainer setDefaultPanel(IUIPanel panel)
//  {
//    return statusServlet_.setDefaultPanel(panel);
//  }

  public void startFugueServer()
  {
    if(started_)
      return;
    
    started_ = true;
//    application_.setLifeCycleState(ComponentLifeCycleState.Starting);
//    application_.setState(ComponentState.OK);
    
    log_.info("FugueServer Started");
    
    try
    {
      
//      int   httpPort = configureHttpPort();
//      int   grpcPort = configureGrcpPort();
//      
//      UIServletResources    res         = new UIServletResources(getResourcesService());
//
//      statusServlet_ = new StatusServlet(res, application_);
//  
//      configure(statusServlet_);
      
      HttpServerBuilder httpServerBuilder = new HttpServerBuilder();
//      (getResourcesService())
//          .addResource("/html/examplePage.html")
//          .addResource("/html/datafeed.html")
//          .addResource("/css/main.css")
//          .addResource("/images/s2avatar.png")
//          .addServlet(statusServlet_)
//          .addServlet(APP_SERVLET_ROOT + "status",    new AppStatusServlet(application_));
//      
//      
//      String keyStore = systemModelService_.getServerKeystorePath();
//      
//      if(keyStore == null || keyStore.trim().length()==0)
//          ssl=true;
      
      boolean ssl = false;
      
      httpServerBuilder
        .setHttpPort(httpPort_);

//      if(ssl)
//      {
//        httpServerBuilder
//          .setHttpsPort(httpPort)
//          .setKeyStorePath(keyStore)
//          .setKeyStorePassword(systemModelService_.getServerKeystorePassword());
//      }
//      
//      configure(httpServerBuilder);
      
      
      synchronized(this)
      {
        for(IServletProvider servletProvider : servletProviders_)
        {
          servletProvider.registerServlets(httpServerBuilder);
        }
        
        for(IUrlPathServlet servlet : servlets_)
        {
          httpServerBuilder.addServlet(servlet);
        }

        server_ = httpServerBuilder.build();
      
        server_.start();
      }
      
      int port = server_.getLocalPort();
      
      if(ssl)
      {
        serverUrl_ = "https://localhost.symphony.com:" + port;
      }
      else
      {
        serverUrl_ = "http://127.0.0.1:" + port;
      }
      
      log_.info("server started on " + serverUrl_);
      log_.info("you can also point your browser to http://" + 
          InetAddress.getLocalHost().getHostName() + ":" + port);
      log_.info("you can also point your browser to http://" + 
          InetAddress.getLocalHost().getHostAddress() + ":" + port);
      
//      addCommand(APP_SERVLET_ROOT, "shutdown", 
//          EnumSet.of(ComponentLifeCycleState.Running,
//              ComponentLifeCycleState.Initializing,
//              ComponentLifeCycleState.Starting),
//          () -> 
//          {
//            diContext_.stop();
//            
//            if(started_)
//              stopFugueServer();
//          });

      
      
//      application_.setLifeCycleState(ComponentLifeCycleState.Running);
//      application_.setState(ComponentState.OK);
    }
    catch(IOException e)
    {
      log_.error("Start failed", e);
      
//      application_.setLifeCycleState(ComponentLifeCycleState.Stopped);
//      application_.setState(ComponentState.Failed);
    }
  }

  public final void stopFugueServer()
  {
    exec_.shutdown();
    server_.stop();
    log_.info("FugueServer Stopped");
    
    try
    {
      exec_.awaitTermination(3, TimeUnit.SECONDS);
    }
    catch(InterruptedException e)
    {
      // Ignored, we are shutting down
    }
    exec_.shutdownNow();
    started_ = false;
  }
  
  public void openBrowser()
  {
    try
    {
      if(serverUrl_ != null)
      {
        String url = serverUrl_ + "/fugue";
        
        log_.info("opening browser on " + url);
        
        Runtime.getRuntime().exec("open " + url);
      }
    }
    catch(IOException e)
    {
      log_.error("Failed to open browser", e);
    }
  }

//  protected void configure(StatusServlet statusServlet)
//  {}
//
//  protected void configure(ServerBuilder<?> serverBuilder) throws S2Exception
//  {}
//  
//  protected void configure(S2HttpServerBuilder httpServerBuilder) throws S2Exception
//  {}
//
//  public void addCommand(String path, String name, 
//      EnumSet<ComponentLifeCycleState> validStates,
//      ICommandHandler handler)
//  {
//    path = path + name;
//    name = name.substring(0,1).toUpperCase() + name.substring(1);
//    
//    ICommand command = new Command(name, path, validStates, handler);
//    server_.addCommand(command);
//    statusServlet_.addCommand(command);
//  }

  public Future<?> submit(Runnable task)
  {
    return exec_.submit(task);
  }

  public <T> Future<T> submit(Runnable task, T result)
  {
    return exec_.submit(task, result);
  }

  public <T> Future<T> submit(Callable<T> task)
  {
    return exec_.submit(task);
  }
  
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
  {
    return exec_.schedule(command, delay, unit);
  }

  class LocalThreadFactory implements ThreadFactory
  {
    private int   id=1;
    
    @Override
    public Thread newThread(Runnable r)
    {
      return new Thread(r, name_ + "-Thread-" + id++);
    }
    
  }
}
