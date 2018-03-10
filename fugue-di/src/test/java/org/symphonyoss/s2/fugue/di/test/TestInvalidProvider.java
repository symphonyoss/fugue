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

package org.symphonyoss.s2.fugue.di.test;

import java.security.InvalidParameterException;

import org.junit.Test;
import org.symphonyoss.s2.fugue.di.Cardinality;
import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.DIContext;
import org.symphonyoss.s2.fugue.di.IComponent;

@SuppressWarnings("javadoc")
public class TestInvalidProvider
{
  @Test(expected=InvalidParameterException.class)
  public void testInvalidProvider()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new InvalidProvider());
    
    context.resolveAndStart();
  }
  
  class InvalidProvider implements IComponent
  {

    @Override
    public ComponentDescriptor getComponentDescriptor()
    {
      return new ComponentDescriptor().addProvidedInterface(IIntegerProvider.class);
    }
  }
  
  @Test(expected=InvalidParameterException.class)
  public void testInvalidIndirectProvider()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new InvalidIndirectProvider());
    
    context.resolveAndStart();
  }
  
  class InvalidIndirectProvider implements IComponent
  {
    private IIntegerProvider provider;
    
    @Override
    public ComponentDescriptor getComponentDescriptor()
    {
      return new ComponentDescriptor().addProvidedInterface(IIntegerProvider.class, () -> provider);
    }
  }
  
  @Test
  public void testIndirectProvider()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new IndirectProvider());
    
    context.resolveAndStart();
  }
  
  class IndirectProvider implements IComponent
  {
    private IIntegerProvider provider = new IntegerProvider(8);
    
    @Override
    public ComponentDescriptor getComponentDescriptor()
    {
      return new ComponentDescriptor().addProvidedInterface(IIntegerProvider.class, () -> provider);
    }
  }
}
