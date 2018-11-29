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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.concurrent.FugueExecutorService;
import org.symphonyoss.s2.fugue.concurrent.FugueScheduledExecutorService;
import org.symphonyoss.s2.fugue.http.HttpServer;
import org.symphonyoss.s2.fugue.http.HttpServerBuilder;
import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.symphonyoss.s2.fugue.http.IServletProvider;
import org.symphonyoss.s2.fugue.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.http.RandomAuthFilter;
import org.symphonyoss.s2.fugue.http.ui.servlet.Command;
import org.symphonyoss.s2.fugue.http.ui.servlet.CommandServlet;
import org.symphonyoss.s2.fugue.http.ui.servlet.ICommand;
import org.symphonyoss.s2.fugue.http.ui.servlet.ICommandHandler;
import org.symphonyoss.s2.fugue.http.ui.servlet.IUIPanel;
import org.symphonyoss.s2.fugue.http.ui.servlet.StatusServlet;

/**
 * The main component for a Fugue process.
 * 
 * @author Bruce Skingle
 *
 */
public class FugueServer extends AbstractComponentContainer<FugueServer> implements IFugueServer
{
  private static final Logger                        log_              = LoggerFactory.getLogger(FugueServer.class);

  private static final String APP_SERVLET_ROOT = "/app/";
  private static final long MEGABYTE = 1024L * 1024L;
  
  private final String                               applicationName_;
  private final int                                  httpPort_;

  private HttpServer                                 server_;
  // private StatusServlet statusServlet_;

  private CopyOnWriteArrayList<FugueExecutorService> executors_        = new CopyOnWriteArrayList<>();
  private CopyOnWriteArrayList<Thread>               threads_          = new CopyOnWriteArrayList<>();

  // private IApplication application_;
  private boolean                                    started_;
  private boolean                                    running_;
  private FugueComponentState                        componentState_ = FugueComponentState.OK;
  private String                                     statusMessage_ = "Initializing...";
  private String                                     serverUrl_;

  private StatusServlet statusServlet_;

  private boolean localWebLogin_;

  private int maxMemory_;

  private String pid_;


  /**
   * Constructor.
   * 
   * @param application       The program name and local port on which to run an http server.
   */
  public FugueServer(IFugueApplication application)
  {
    this(application.getName(), application.getHttpPort());
  }
  
  /**
   * Constructor.
   * 
   * @param name              The program name.
   * @param httpPort          The local port on which to run an http server.
   */
  public FugueServer(String name, int httpPort)
  {
    super(FugueServer.class);

    String configFile = System.getProperty("log4j.configurationFile");
    
    if(configFile == null)
    {
      URL url = getClass().getClassLoader().getResource("log4j2.xml");
      System.out.println("log4j2.xml from resources = " + url);
    }
    else
    {
      System.out.println("log4j2.xml from system property = " + configFile);
    }
    
    applicationName_  = name;
    httpPort_         = httpPort;
    
    register(new IFugueLifecycleComponent()
    {
      
      @Override
      public void start()
      {
//        startFugueServer();
      }
      
      @Override
      public void quiesce()
      {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void stop()
      {
        stopFugueServer();
      }

      @Override
      public FugueLifecycleState getLifecycleState()
      {
        return FugueServer.this.getLifecycleState();
      }

      @Override
      public FugueComponentState getComponentState()
      {
        return componentState_;
      }

      @Override
      public String getComponentStatusMessage()
      {
        return statusMessage_;
      }
    });
    
    withCommand(APP_SERVLET_ROOT, "shutdown", 
        EnumSet.of(FugueLifecycleState.Running,
            FugueLifecycleState.Initializing,
            FugueLifecycleState.Starting),
        () -> 
        {
          if(started_)
            stopFugueServer();
        });
    
    register(new HealthCheckServlet());
  }

  @Override
  public String getApplicationName()
  {
    return applicationName_;
  }

  @Override
  public int getHttpPort()
  {
    return httpPort_;
  }

  @Override
  public FugueServer start()
  {
    super.start();
    
    startFugueServer();
    
    return this;
  }

  @Override
  public synchronized FugueServer join() throws InterruptedException
  {
    return mainLoop(0L);
//    while(running_)
//      wait();
//    
//    return this;
  }
  
  @Override
  public IFugueServer withLocalWebLogin()
  {
    localWebLogin_ = true;
    
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

  @Override
  public IFugueServer withPanel(IUIPanel panel)
  {
    statusServlet_.addPanel(panel);
    
    return this;
  }

  @Override
  public IFugueServer withDefaultPanel(IUIPanel panel)
  {
    statusServlet_.setDefaultPanel(panel);
    
    return this;
  }
  
  public IFugueServer withCommand(String path, String name, 
      EnumSet<FugueLifecycleState> validStates,
      ICommandHandler handler)
  {
    path = path + name;
    name = name.substring(0,1).toUpperCase() + name.substring(1);
    
    ICommand command = new Command(name, path, validStates, handler);
    
    register(command);
    
    return this;
  }

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

  public synchronized boolean setRunning(boolean running)
  {
    boolean v = running_;
    running_ = running;
    
    if(!running)
      notifyAll();
    
    return v;
  }

  private final void startFugueServer()
  {
    if(started_)
      return;
    
    started_ = true;
    setRunning(true);
    
    log_.info("FugueServer Started");
    
    try
    {
      HttpServerBuilder httpServerBuilder = new HttpServerBuilder();
      RandomAuthFilter filter = null;
      
      if(localWebLogin_)
      {
        filter = new RandomAuthFilter();
        httpServerBuilder.withFilter(filter);
      }
      for(IResourceProvider provider : getResourceProviders())
        httpServerBuilder.withResources(provider);
      
      httpServerBuilder
        .withHttpPort(httpPort_);

      List<IResourceProvider> resourceProviders = getResourceProviders();
      
      if(!resourceProviders.isEmpty())
      {
        statusServlet_ = new StatusServlet(resourceProviders.get(0), this);
        httpServerBuilder.withServlet(statusServlet_);
        
        for(ICommand command : getCommands())
        {
          httpServerBuilder.withServlet(command.getPath(),  new CommandServlet(command.getHandler()));
          statusServlet_.addCommand(command);
        }
      }
      
      synchronized(this)
      {
        for(IServletProvider servletProvider : getServletProviders())
        {
          servletProvider.registerServlets(httpServerBuilder);
        }
        
        for(IUrlPathServlet servlet : getServlets())
        {
          httpServerBuilder.withServlet(servlet);
        }

        server_ = httpServerBuilder.build();
      
        server_.start();
      }
      
      int port = server_.getLocalPort();
      
      serverUrl_ = "http://127.0.0.1:" + port;
      
      log_.info("server started on " + serverUrl_);
      log_.info("you can also point your browser to http://" + 
          InetAddress.getLocalHost().getHostName() + ":" + port);
      log_.info("you can also point your browser to http://" + 
          InetAddress.getLocalHost().getHostAddress() + ":" + port);
      
      if(filter != null)
      {
        openBrowser(RandomAuthFilter.LOGIN_TOKEN + "=" + filter.getAuthToken());
      }
      
      setLifeCycleState(FugueLifecycleState.Running);
      statusMessage_ = "";
    }
    catch(IOException e)
    {
      setLifeCycleState(FugueLifecycleState.Failed);
      componentState_ = FugueComponentState.Failed;
      statusMessage_ = e.toString();
      log_.error("Start failed", e);
    }
  }

  private final void stopFugueServer()
  {
    setRunning(false);
    
    if(!started_)
    {
      log_.info("Not started, no need to stop");
      return;
    }

    setLifeCycleState(FugueLifecycleState.Stopping);
    statusMessage_ = "Shutting down...";
    
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

    setLifeCycleState(FugueLifecycleState.Stopped);
    statusMessage_ = "Stopped cleanly.";
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
   * 
   * @param queryString An optional query string.
   */
  public void openBrowser(String queryString)
  {
    try
    {
      if(serverUrl_ != null)
      {
        String url = serverUrl_ + "/fugue";
        
        if(queryString != null)
          url = url + "?" + queryString;
        
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
  
  @Override
  public FugueServer mainLoop(long timeout) throws InterruptedException
  {
    long endTime = timeout <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + timeout;
    Runtime runtime = Runtime.getRuntime();
    pid_ = getPid();
    ProcessBuilder builder = new ProcessBuilder()
        .command("ps", "-o", "pid,rss,vsz,time");
    
    while(isRunning() && System.currentTimeMillis() < endTime)
    {
      log_.info(String.format("JVM Memory: %4d used, %4d free, %4d total, %3d processors", runtime.freeMemory() / MEGABYTE, runtime.totalMemory() / MEGABYTE, runtime.maxMemory() / MEGABYTE, runtime.availableProcessors()));
      run(builder);
      log_.info("pid " + pid_ + " max memory " + maxMemory_);
      
      long bedtime = endTime - System.currentTimeMillis();
      
      if(bedtime > 60000)
        bedtime = 60000;
      
      if(bedtime>0)
      {
        synchronized(this)
        {
          wait(bedtime);
        }
      }
    }
    
    return this;
  }

  private void run(ProcessBuilder builder)
  {
    try
    {
      Process process = builder.start();
      
      try(BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream())))
      {
        String line = in.readLine();
        log_.info(line);
        while((line = in.readLine()) != null)
        {
          String[] words = line.trim().split(" +");
          
          if(pid_.equals(words[0]))
          {
            log_.info(line);
            try
            {
              String word = words[1];
              int mem = 0;
              
              if(word.endsWith("m"))
                mem = Integer.parseInt(word.substring(0, word.length()-1));
              else if(word.endsWith("g"))
                  mem = (int)(1000 * Double.parseDouble(word.substring(0, word.length()-1)));
              else
                mem = Integer.parseInt(word);
              
              if(mem > maxMemory_)
                maxMemory_ = mem;
            }
            catch(NumberFormatException e)
            {
              log_.error("Failed to parse memory", e);
            }
          }
        }
      }
      
      try(BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream())))
      {
        String line;
        while((line = in.readLine()) != null)
          log_.warn(line);
      }
    }
    catch (IOException e)
    {
      log_.error("Unable to run command", e);
    }
  }

  private String getPid()
  {
    String processName =
        java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      
    return processName.split("@")[0];
  }
}
