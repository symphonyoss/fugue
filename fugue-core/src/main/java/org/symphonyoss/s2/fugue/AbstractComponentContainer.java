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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

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
public class AbstractComponentContainer<T extends AbstractComponentContainer<T>> extends FugueLifecycleBase<T> implements IFugeComponentContainer<T>
{
  private static final Logger                  log_                 = LoggerFactory.getLogger(AbstractComponentContainer.class);

  private final List<IFugueComponent>          components_          = new ArrayList<>();
  private final List<IFugueLifecycleComponent> lifecycleComponents_ = new ArrayList<>();
  private final List<IResourceProvider>        resourceProviders_   = new ArrayList<>();
  private final List<IServletProvider>         servletProviders_    = new ArrayList<>();
  private final List<IUrlPathServlet>          servlets_            = new ArrayList<>();
  private final List<ICommand>                 commands_            = new ArrayList<>();

  private Stack<IFugueComponent>               stopStack_           = new Stack<>();
  
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
    setLifeCycleState(FugueLifecycleState.Starting);
    
    for(IFugueComponent component : components_)
    {
      stopStack_.push(component);
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
}
