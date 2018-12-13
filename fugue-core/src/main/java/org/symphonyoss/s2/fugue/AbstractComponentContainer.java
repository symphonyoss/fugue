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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.http.IResourceProvider;
import org.symphonyoss.s2.fugue.http.IServletProvider;
import org.symphonyoss.s2.fugue.http.IUrlPathServlet;
import org.symphonyoss.s2.fugue.http.ui.servlet.ICommand;

/**
 * An abstract fluent container of Fugue components.
 * 
 * @author Bruce Skingle
 *
 * @param <T> The concrete type to be returned by fluent methods.
 */
public class AbstractComponentContainer<T extends IFugeComponentContainer<T>> extends FugueLifecycleBase<T> implements IFugeComponentContainer<T>
{
  private static final long                    MEGABYTE             = 1024L * 1024L;
  
  private static final Logger                  log_                 = LoggerFactory.getLogger(AbstractComponentContainer.class);

  private final List<IFugueComponent>          components_          = new ArrayList<>();
  private final List<IFugueLifecycleComponent> lifecycleComponents_ = new ArrayList<>();
  private final List<IResourceProvider>        resourceProviders_   = new ArrayList<>();
  private final List<IServletProvider>         servletProviders_    = new ArrayList<>();
  private final List<IUrlPathServlet>          servlets_            = new ArrayList<>();
  private final List<ICommand>                 commands_            = new ArrayList<>();

  private ArrayDeque<IFugueComponent>          stopStack_           = new ArrayDeque<>();
  private int                                  maxMemory_;
  private String                               pid_;
  private boolean                              running_;
  
  /**
   * Constructor.
   * 
   * @param type The concrete type returned by fluent methods.
   */
  public AbstractComponentContainer(Class<T> type)
  {
    super(type);
  }
  
  @Override
  public T withComponents(Object ...components)
  {
    assertConfigurable();
    
    for(Object o : components)
    {
      if(o instanceof IFugueComponent)
      {
        components_.add((IFugueComponent)o);
      }
      if(o instanceof IFugueLifecycleComponent)
      {
        lifecycleComponents_.add((IFugueLifecycleComponent)o);
      }
      if(o instanceof IResourceProvider)
      {
        resourceProviders_.add((IResourceProvider)o);
      }
      if(o instanceof IServletProvider)
      {
        servletProviders_.add((IServletProvider)o);
      }
      if(o instanceof IUrlPathServlet)
      {
        servlets_.add((IUrlPathServlet)o);
      }
      if(o instanceof ICommand)
      {
        commands_.add((ICommand)o);
      }
    }
    
    return self();
  }
  
  @Override
  public List<IFugueComponent> getComponents()
  {
    return components_;
  }

  @Override
  public List<IFugueLifecycleComponent> getLifecycleComponents()
  {
    return lifecycleComponents_;
  }

  @Override
  public List<IResourceProvider> getResourceProviders()
  {
    return resourceProviders_;
  }

  @Override
  public List<IServletProvider> getServletProviders()
  {
    return servletProviders_;
  }

  @Override
  public List<IUrlPathServlet> getServlets()
  {
    return servlets_;
  }

  @Override
  public List<ICommand> getCommands()
  {
    return commands_;
  }

  @Override
  public <C> C register(C component)
  {
    withComponents(component);
    
    return component;
  }
  
  @Override
  public T start()
  {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
    {
      @Override
      public void run()
      {
        FugueLifecycleState state = getLifecycleState();
        
        System.err.println("Shutdown hook called from state " + state);
        
        switch(state)
        {
          case Initializing:
          case Running:
          case Starting:
            System.err.println("Attempting to quiesce...");
            quiesce();
            // fall through
            
          case Quiescing:
          case Quiescent:
            System.err.println("Attempting clean shutdown...");
            setLifeCycleState(FugueLifecycleState.Stopping);
            
            if(doStop())
            {
              System.err.println("Faild to stop cleanly");
            }
            else
            {
              setLifeCycleState(FugueLifecycleState.Stopped);
              System.err.println("Attempting clean shutdown...DONE");
            }
            break;
            
          case Stopped:
            break;
              
          default:
            try
            {
              // Sleep for 5 seconds in the hope that threads have time to finish....
              System.err.println("Sleep for 5 seconds...");
              Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {
              System.err.println("Sleep for 5 seconds...INTERRUPTED");
              e.printStackTrace();
            }
            System.err.println("Sleep for 5 seconds...DONE");
        }
      }
    }));
    
    setLifeCycleState(FugueLifecycleState.Starting);
    
    for(IFugueComponent component : components_)
    {
      try
      {
        
        log_.debug("Start " + component);
        component.start(); 
        stopStack_.push(component);
      }
      catch(RuntimeException ex)
      {
        log_.error("Unable to start component " + 
            component, ex);
        
        setLifeCycleState(FugueLifecycleState.Failed);
        
        doStop();
        
        log_.error("Faild to start cleanly : CALLING System.exit()");
        System.exit(1);
      }
    }
    setLifeCycleState(FugueLifecycleState.Running);
    setRunning(true);
    return self();
  }
  
  @Override
  public T quiesce()
  {
    RuntimeException error = null;
    
    setLifeCycleState(FugueLifecycleState.Quiescing);
    
    log_.info("Quiescing...");
    
    for(IFugueComponent component : stopStack_)
    {
      try
      {
        log_.debug("Quiesce " + component);
        component.quiesce();
      }
      catch(RuntimeException ex)
      {
        log_.error("Unable to quiesce component " + 
            component, ex);
        // Don't re-throw because we want other components to have a chance to stop
        
        error = ex;
      }
    }
    
    if(error == null)
    {
      setLifeCycleState(FugueLifecycleState.Quiescent);
    }
    else
    {
      setLifeCycleState(FugueLifecycleState.Failed);
      throw error;
    }
    return self();
  }

  @Override
  public T stop()
  {
    setLifeCycleState(FugueLifecycleState.Stopping);
    
    if(doStop())
    {
      log_.error("Faild to stop cleanly : CALLING System.exit()");
      System.exit(1);
    }
    setLifeCycleState(FugueLifecycleState.Stopped);
    
    return self();
  }
  
  private boolean doStop()
  {
    boolean terminate = false;
    
    log_.info("Stopping...");
    
    while(!stopStack_.isEmpty())
    {
      IFugueComponent component = stopStack_.pop();
      try
      {
        log_.debug("Stop " + component);
        component.stop();
      }
      catch(RuntimeException ex)
      {
        log_.error("Unable to stop component " + 
            component, ex);
        // Don't re-throw because we want other components to have a chance to stop
        
        terminate = true;
        setLifeCycleState(FugueLifecycleState.Failed);
      }
    }
    
    return terminate;
  }

  
  @Override
  public synchronized boolean isRunning()
  {
    return running_;
  }

  @Override
  public synchronized boolean setRunning(boolean running)
  {
    boolean v = running_;
    running_ = running;
    
    if(!running)
      notifyAll();
    
    return v;
  }
  
  @Override
  public T mainLoop(long timeout) throws InterruptedException
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
    
    return self();
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
