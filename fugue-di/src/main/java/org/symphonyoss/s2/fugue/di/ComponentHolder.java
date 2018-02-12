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
import java.util.List;

public class ComponentHolder
{
  private IComponent             component_;
  private List<ComponentHolder>  dependentComponents_ = new ArrayList<>();
  private List<DependencyHolder> dependencies_        = new ArrayList<>();
  private ComponentDescriptor    componentDescriptor_;

  public ComponentHolder(IComponent component)
  {
    component_ = component;
    componentDescriptor_ = component_.getComponentDescriptor();
  }
  
  public IComponent getComponent()
  {
    return component_;
  }
  
  public String getName()
  {
    return component_.getClass().getSimpleName();
  }

  public void addDependency(ComponentHolder providerHolder, Dependency<?> dependency)
  {
    dependentComponents_.add(providerHolder);
    dependencies_.add(new DependencyHolder(providerHolder, dependency));
  }

  public List<DependencyHolder> getDependencies()
  {
    return dependencies_;
  }

  public ComponentDescriptor getComponentDescriptor()
  {
    return componentDescriptor_;
  }
  
  @Override
  public String toString()
  {
    return "Holder(" + component_.getClass().getSimpleName() + ")";
  }
}

class DependencyHolder
{
  ComponentHolder providerHolder_;
  Dependency<?>   dependency_;
  
  public DependencyHolder(ComponentHolder componentHolder, Dependency<?> dependency)
  {
    providerHolder_ = componentHolder;
    dependency_ = dependency;
  }
  
  @Override
  public String toString()
  {
    return dependency_ + " from " + providerHolder_;
  }
}
