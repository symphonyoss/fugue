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

import java.security.InvalidParameterException;

/* package */ class ProvidedInterface<T>
{
  private final Class<T>    providedInterface_;
  private IProvider<T>        provider_;
  private ComponentHolder providingComponent_;
  private T implementation_;
  
  /* package */ ProvidedInterface(Class<T> requiredInterface, IProvider<T> provider)
  {
    providedInterface_ = requiredInterface;
    provider_ = provider;
  }

  /* package */ Class<T> getProvidedInterface()
  {
    return providedInterface_;
  }
  
  @SuppressWarnings("unchecked")
  /* package */ void bind(ComponentHolder componentHolder)
  {
    if(provider_ == null)
    {
      IComponent component = componentHolder.getComponent();
      
      if(!providedInterface_.isInstance(component))
        throw new InvalidParameterException("Component " + component.getClass() +
            " does not implement the interface " + providedInterface_ +
            " which it declares to provide.");
      
      implementation_ = (T) component;
    }
    else
    {
      implementation_ = provider_.provide();
      
      if(implementation_ == null)
        throw new InvalidParameterException("Component " + componentHolder.getComponent().getClass() +
            " provides a null implementation of the interface " + providedInterface_);
    }
    
    providingComponent_ = componentHolder;
  }
  
  /* package */ T getImplementation()
  {
    return implementation_;
  }

  /* package */ ComponentHolder getProvidingComponent()
  {
    return providingComponent_;
  }

  @Override
  public String toString()
  {
    return "Provided interface " + providedInterface_.getSimpleName();
  }
}
