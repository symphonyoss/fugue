/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.di;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.symphonyoss.s2.fugue.di.component.ILogComponent;
import org.symphonyoss.s2.fugue.di.component.impl.DefaultLogComponent;

/**
 * A dependency injection context.
 * 
 * @author Bruce Skingle
 *
 */
public class DIContext implements IDIContext
{
  private static final String UNREACHABLE_CODE = "UNREACHABLE CODE - or did you change Cardinality?";
  
  private ILogComponent                        log_ = new DefaultLogComponent();
  private Map<Class<?>, List<ProvidedInterface<?>>> providedInterfaceMap_      = new HashMap<>();
  private List<ComponentHolder>                componentList_             = new ArrayList<>();
  private Object                               lifeCycleLock_             = new Object();
  private DIContextState                          lifeCycle_                 = DIContextState.Initializing;
  private List<IComponent>                     unresolvableComponentList_ = new ArrayList<>();
  private boolean                              logProvided_;
  private Stack<ComponentHolder>               stopStack_                 = new Stack<>();
  
  @Override
  public synchronized DIContext register(IComponent component)
  {
    if(getLifeCycle() != DIContextState.Initializing)
      throw new IllegalStateException("Components cannot be added once resolution has started");
    
    ComponentHolder holder = new ComponentHolder(component);
    ComponentDescriptor desc = holder.getComponentDescriptor();
        
    for(ProvidedInterface<?> provider : desc.getProvidedInterfaces())
    { 
      provider.bind(holder); // throws exception if there is a configuration problem
      Class<?> providedInterface = provider.getProvidedInterface();
      List<ProvidedInterface<?>> list = providedInterfaceMap_.get(providedInterface);
      
      if(list == null)
      {
        list = new ArrayList<>();
        providedInterfaceMap_.put(providedInterface, list);
      }
      
      list.add(provider);
      if(providedInterface == ILogComponent.class)
      {
        log_ = (ILogComponent) provider.getImplementation();
        logProvided_ = true;
      }
    }
    
    componentList_.add(holder);
    
    if(component instanceof IComponentProvider)
    {
      ((IComponentProvider) component).registerComponents(this);
    }
    
    return this;
  }
  
  @Override
  public void resolveAndStart()
  {
    resolve(true);
  }
  
  @Override
  public void resolve()
  {
    resolve(false);
  }
  
  private  synchronized void resolve(boolean start)
  {
    if(!logProvided_)
      register(log_);
    
    setLifeCycle(DIContextState.Resolving);
    
    for(ComponentHolder holder : componentList_)
    {
      resolve(holder);
      
    }
    if(unresolvableComponentList_.isEmpty())
    {
      if(start)
        start();
      else
        setLifeCycle(DIContextState.Resolved);
    }
    else
    {
      setLifeCycle(DIContextState.Failed);
      log_.error("The following components are unresolvable:");
      
      for(IComponent component : unresolvableComponentList_)
      {
        log_.error(component.getClass());
      }
      
      throw new ConfigurationFault("Unresolvable components");
    }
  }
  
  @Override
  public boolean resolveAdditionalComponent(IComponent component)
  {
    ComponentHolder holder = new ComponentHolder(component);
    
    if(resolve(holder))
    {
      start(holder);
      return true;
    }
    else
      return false;
  }
  
  private boolean resolve(ComponentHolder holder)
  {
    boolean result = true;
    
    IComponent component = holder.getComponent();
    
    for(Dependency<?> dependency : holder.getComponentDescriptor().getDependencies())
    {
      List<ProvidedInterface<?>> providers = providedInterfaceMap_.get(dependency.getRequiredInterface());
      
      if(providers == null || providers.size() == 0) // zero size "can't happen" but....
      {
        switch(dependency.getCardinality())
        {
          case zeroOrOne:
          case zeroOrMore:
            // OK
            break;
            
          case one:
          case oneOrMore:
            log_.error("Unsatisfied dependency from " + component.getClass() +
              " to " + dependency.getRequiredInterface());
          
            result = false;
            unresolvableComponentList_.add(component);
            break;
            
          default: 
            throw new IllegalStateException(UNREACHABLE_CODE);
        }
      }
      else
      {
        try
        {
          switch(dependency.getCardinality())
          {
            case one:
            case zeroOrOne:
              if(providers.size()==1)
              {
                doBind(providers.get(0), dependency, holder);
              }
              else
              {
                log_.error("Multiple providers of dependency " + 
                    dependency.getRequiredInterface() +
                    " for " + component.getClass());
                
                for(ProvidedInterface<?> provider : providers)
                {
                  log_.error("Provided by " + provider.getProvidingComponent());
                }
                
                result = false;
                unresolvableComponentList_.add(component);
              }
              break;
              
            case oneOrMore:
            case zeroOrMore:
              for(ProvidedInterface<?> provider : providers)
              {
                doBind(provider, dependency, holder);
              }
              break;
              
            default:
              throw new IllegalStateException(UNREACHABLE_CODE);
          }
        }
        catch (RuntimeException e) // yes, really, will re-throw it though
        {
          log_.error("Unable to bind dependency " + 
              dependency.getRequiredInterface() +
              " for " + component.getClass(), e);
          unresolvableComponentList_.add(component);
          result = false;
          
          throw e;
        }
        
      }
    }
    
    return result;
  }

  @Override
  public synchronized void start()
  {
//  TreeMap<Integer, List<ComponentHolder>>  startMap = new TreeMap<>();
    
  setLifeCycle(DIContextState.Starting);
//  for(ComponentHolder holder : componentList_)
//  {
//    System.err.println("DI get level for " + holder);
//    int level = holder.getLevel();
//    List<ComponentHolder> list = startMap.get(level);
//    
//    if(list == null)
//    {
//      list = new ArrayList<>();
//      startMap.put(level, list);
//    }
//    
//    list.add(holder);
//  }
//  
//  String  tab = "";
//  
//  for(Entry<Integer, List<ComponentHolder>> e : startMap.entrySet())
//  {
//    tab = tab + ">";
//    
//    for(ComponentHolder holder : e.getValue())
//    {
//      log_.debug("Order " + tab + e.getKey() + " " + 
//          holder.getName());
//      
//      for(DependencyHolder d : holder.getDependencies())
//      {
//        log_.debug("Order " + tab + e.getKey() + " +---->" + 
//            d.providerHolder_.getName() + 
//            "[" + d.dependency_.getRequiredInterface().getSimpleName() + "]");
//      }
//    }
//  }
//  
//  for(Entry<Integer, List<ComponentHolder>> e : startMap.entrySet())
//  {
//    stopStack_.push(e.getValue());
//    
//    for(ComponentHolder holder : e.getValue())
  for(ComponentHolder holder : componentList_)
  {
    try
    {
      start(holder);
      
    }
    catch(RuntimeException ex)
    {
      log_.error("Unable to start component " + 
          holder.getName(), ex);
      
      setLifeCycle(DIContextState.Failed);
      
      doStop();
      
      log_.error("Faild to start cleanly : CALLING System.exit()");
      System.exit(1);
    }
  }
//  }
  setLifeCycle(DIContextState.Running);
  }

  private void start(ComponentHolder holder)
  {
    log_.debug("Start " + holder.getName());
    for(Runnable handler : holder.getComponentDescriptor().getStartHandlers())
      handler.run();
    stopStack_.push(holder);
  }

  private void doBind(ProvidedInterface<?> providerHolder, 
      Dependency<?> dependency, ComponentHolder componentHolder)
  {
    log_.debug("Binding dependency " + 
        dependency.getRequiredInterface().getSimpleName() +
        " from " + providerHolder.getProvidingComponent() + 
        " for " + componentHolder.getName());
    
    dependency.bind(providerHolder.getImplementation());
    
    componentHolder.addDependency(providerHolder.getProvidingComponent(), dependency);
  }
  
  @Override
  public void stop()
  {
    setLifeCycle(DIContextState.Stopping);
    
    if(doStop())
    {
      log_.error("Faild to stop cleanly : CALLING System.exit()");
      System.exit(1);
    }
    setLifeCycle(DIContextState.Stopped);
  }
  
  private boolean doStop()
  {
    boolean terminate = false;
    
    while(!stopStack_.isEmpty())
    {
      ComponentHolder holder = stopStack_.pop();
      try
      {
        log_.debug("Stop " + holder.getName());
        for(Runnable handler : holder.getComponentDescriptor().getStopHandlers())
          handler.run();
      }
      catch(RuntimeException ex)
      {
        log_.error("Unable to stop component " + 
            holder.getName(), ex);
        // Don't re-throw because we want other components to have a chance to stop
        
        terminate = true;
        setLifeCycle(DIContextState.Failed);
      }
    }
    
    return terminate;
  }
  
  @Override
  public DIContextState  getLifeCycle()
  {
    synchronized(lifeCycleLock_)
    {
      return lifeCycle_;
    }
  }
  
  private void setLifeCycle(DIContextState lifeCycle)
  {
    synchronized(lifeCycleLock_)
    {
      lifeCycle_ = lifeCycle;
      lifeCycleLock_.notify();
    }
  }
  
  @Override
  public void waitForLifeCycle(DIContextState lifeCycle) throws InterruptedException
  {
    synchronized(lifeCycleLock_)
    {
      while(lifeCycle_ != lifeCycle)
      {
        lifeCycleLock_.wait();
      }
    }
  }
  
  @Override
  public void join() throws InterruptedException
  {
    waitForLifeCycle(DIContextState.Stopped);
  }
}
