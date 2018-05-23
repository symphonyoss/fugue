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

package org.symphonyoss.s2.fugue;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.http.HttpServer;
import org.symphonyoss.s2.common.http.HttpServerBuilder;
import org.symphonyoss.s2.common.http.IServletProvider;
import org.symphonyoss.s2.common.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.concurrent.FugueExecutorService;
import org.symphonyoss.s2.fugue.concurrent.FugueScheduledExecutorService;

/**
 * The main component for a Fugue process.
 * 
 * @author Bruce Skingle
 *
 */
public class FugueServer extends AbstractComponentContainer<FugueServer> implements IFugueServer
{
  private static final Logger                        log_              = LoggerFactory.getLogger(FugueServer.class);
  
  private final int                                  httpPort_;

  private HttpServer                                 server_;
  // private StatusServlet statusServlet_;
  private CopyOnWriteArrayList<IServletProvider>     servletProviders_ = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<IUrlPathServlet>      servlets_         = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<FugueExecutorService> executors_        = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<Thread>               threads_          = new CopyOnWriteArrayList<>();

  // private IApplication application_;
  private boolean                                    started_;
  private boolean                                    running_;
  private String                                     serverUrl_;

  public FugueServer(IFugueApplication application)
  {
    this(application.getName(), application.getHttpPort());
  }
  
  /**
   * Constructor.
   * 
   * @param name        The program name.
   * @param httpPort    The local port on which to run an http server.
   * @param components  Zero or  more components which will be managed by this server
   */
  public FugueServer(String name, int httpPort, Object ...components)
  {
    super(FugueServer.class);
    
    httpPort_   = httpPort;
    
    register(new IFugueComponent()
    {
      
      @Override
      public void start()
      {
        startFugueServer();
      }
      
      @Override
      public void stop()
      {
        stopFugueServer();
      }
    });
    
    withComponents(components);
    
    
  }
  
  
  
  /**
   * Add the given components.
   * 
   * @param components one or more components.
   * 
   * @return this (Fluent method).
   * 
   * @throws IllegalStateException If the server is not configurable (i.e. has been started)
   */
  public FugueServer withComponents(Object ...components)
  {
    super.withComponents(components);
    
    for(Object o : components)
    {
      if(o instanceof IUrlPathServlet)
      {
        servlets_.addIfAbsent((IUrlPathServlet)o);
      }
    }
    
    return this;
  }
  
  /**
   * Add the given ServletProviders.
   * 
   * @param servletProvider a provider of IURLPathServlets.
   * 
   * @return this (Fluent method).
   * 
   * @throws IllegalStateException If the server is not configurable (i.e. has been started)
   */
  public synchronized FugueServer withServletProvider(IServletProvider servletProvider)
  {
    assertConfigurable();
    
    servletProviders_.addIfAbsent(servletProvider);
//    if(servletProviders_.addIfAbsent(servletProvider))
//    {
//      if(server_ != null)
//        servletProvider.registerServlets(server_);
//    }
    
    return this;
  }

  /**
   * Add the given ServletProviders.
   * 
   * @param servlet an IURLPathServlet.
   * 
   * @return this (Fluent method).
   * 
   * @throws IllegalStateException If the server is not configurable (i.e. has been started)
   */
  public synchronized FugueServer registerServlet(IUrlPathServlet servlet)
  {
    assertConfigurable();
    
    servlets_.addIfAbsent(servlet);
//    if(servlets_.addIfAbsent(servlet))
//    {
//      if(server_ != null)
//        server_.addServlet(servlet);
//    }
    
    return this;
  }
  
//  @Override
//  public IFugueServer start()
//  {
//    transitionTo(FugueLifecycleState.Starting);
//    
//    for(IFugueComponent component : components_)
//    {
//      try
//      {
//        
//        log_.debug("Start " + component);
//        component.start(); 
//        stopStack_.push(component);
//      }
//      catch(RuntimeException ex)
//      {
//        log_.error("Unable to start component " + 
//            component, ex);
//        
//        setLifeCycleState(FugueLifecycleState.Failed);
//        
//        doStop();
//        
//        log_.error("Faild to start cleanly : CALLING System.exit()");
//        System.exit(1);
//      }
//    }
//    setLifeCycleState(FugueLifecycleState.Running);
//    return this;
//  }

  @Override
  public FugueServer join() throws InterruptedException
  {
    server_.join();
    return this;
  }
  
  

  @Override
  public IFugueServer fail()
  {
    log_.error("Server FAILED");
    return stop();
  }

  
  
  @Override
  public IFugueServer withCurrentThread()
  {
    return withThread(Thread.currentThread());
  }
  
 
  @Override
  public IFugueServer withThread(Thread thread)
  {
    threads_.add(thread);
    
    return this;
  }
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

  /**
   * Return true iff the server is running.
   * 
   * Threads may call this method in their main loop to determine if they should terminate.
   * 
   * @return true iff the server is running.
   */
  public synchronized boolean isRunning()
  {
    return running_;
  }

  private synchronized boolean setRunning(boolean running)
  {
    boolean v = running_;
    running_ = running;
    return v;
  }

  private final void startFugueServer()
  {
    if(started_)
      return;
    
    started_ = true;
    setRunning(true);
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

  private final void stopFugueServer()
  {
    if(!setRunning(false))
    {
      log_.info("Not running, no need to stop");
      return;
    }
    
    for(FugueExecutorService exec : executors_)
      exec.shutdown();
    
    for(Thread thread : threads_)
      thread.interrupt();
    
    server_.stop();
    log_.info("FugueServer Stopped");
    
    waitForAllExecutors(5000);
    
    for(FugueExecutorService exec : executors_)
    {
      if(!exec.isTerminated())
      {
        log_.warn("Executor " + exec + " did not terminate cleanly, calling shutdownNow...");
        exec.shutdownNow();
      }
    }
    
    for(Thread thread : threads_)
    {
      if(thread.isAlive())
        log_.error("Thread " + thread + " did not terminate cleanly");
    }
    
    waitForAllExecutors(5000);
    
    for(FugueExecutorService exec : executors_)
    {
      if(!exec.isTerminated())
      {
        log_.error("Executor " + exec + " did not terminate cleanly");
      }
    }
    
    started_ = false;
  }
  
  private void waitForAllExecutors(int delayMillis)
  {
    long timeout = System.currentTimeMillis() + delayMillis;
    
    for(FugueExecutorService exec : executors_)
    {
      long wait = timeout - System.currentTimeMillis();
      
      if(wait > 0)
      {
        try
        {
          exec.awaitTermination(wait, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
          log_.info("Interrupted waiting for executor termination");
        }
      }
    }
  }
  
  /**
   * Open the browser on the URL for this server.
   */
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

  
  @Override
  public ExecutorService newExecutor(String name)
  {
    FugueExecutorService fugueExec = new FugueExecutorService(this, name);
    
    executors_.add(fugueExec);
    
    return fugueExec;
  }
  
  @Override
  public ExecutorService newExecutor(ExecutorService exec)
  {
    FugueExecutorService fugueExec = new FugueExecutorService(this, exec);
    
    executors_.add(fugueExec);
    
    return fugueExec;
  }
  
  @Override
  public ScheduledExecutorService newScheduledExecutor(String name)
  {
    FugueScheduledExecutorService exec = new FugueScheduledExecutorService(this, name);
    
    executors_.add(exec);
    
    return exec;
  }
  
  @Override
  public ScheduledExecutorService newScheduledExecutor(ScheduledExecutorService exec)
  {
    FugueScheduledExecutorService fugueExec = new FugueScheduledExecutorService(this, exec);
    
    executors_.add(fugueExec);
    
    return fugueExec;
  }
}
