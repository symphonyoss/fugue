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

package org.symphonyoss.s2.fugue.di.test;

import org.junit.Test;
import org.symphonyoss.s2.fugue.di.Cardinality;
import org.symphonyoss.s2.fugue.di.ConfigurationError;
import org.symphonyoss.s2.fugue.di.impl.DIContext;

public class TestDIContext
{

  @Test
  public void testOne()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1));
    
    context.resolveAndStart();
    
    consumer.check(1);
  }
  
  @Test(expected=ConfigurationError.class)
  public void testMissing()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer);
    
    context.resolveAndStart();
  }
  
  @Test
  public void testMissingZeroOrMore()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.zeroOrMore);
    
    DIContext context = new DIContext()
        .register(consumer);
    
    context.resolveAndStart();
  }
  
  @Test
  public void testMissingZeroOrOne()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.zeroOrOne);
    
    DIContext context = new DIContext()
        .register(consumer);
    
    context.resolveAndStart();
  }
  
  @Test(expected=ConfigurationError.class)
  public void testInvalidMultiple()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1))
        .register(new IntegerProvider(99));
    
    context.resolveAndStart();
  }
  
  @Test(expected=ConfigurationError.class)
  public void testInvalidMultipleZeroOrOne()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.zeroOrOne);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1))
        .register(new IntegerProvider(99));
    
    context.resolveAndStart();
  }
  
  @Test(expected=ConfigurationError.class)
  public void testInvalidNone()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.one);
    
    DIContext context = new DIContext()
        .register(consumer);
    
    context.resolveAndStart();
  }
  
  @Test(expected=ConfigurationError.class)
  public void testInvalidNoneOneOrMore()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.oneOrMore);
    
    DIContext context = new DIContext()
        .register(consumer);
    
    context.resolveAndStart();
  }
  
  @Test
  public void testOneOrMore()
  {
    IntegerConsumer consumer = new IntegerConsumer(Cardinality.oneOrMore);
    
    DIContext context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1));
    
    context.resolveAndStart();
    
    consumer.check(1);
    
    consumer = new IntegerConsumer(Cardinality.oneOrMore);
    context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1))
        .register(new IntegerProvider(2));
    
    context.resolveAndStart();
    consumer.check(2);
    
    consumer = new IntegerConsumer(Cardinality.oneOrMore);
    context = new DIContext()
        .register(consumer)
        .register(new IntegerProvider(1))
        .register(new IntegerProvider(2))
        .register(new IntegerProvider(3));
    
    context.resolveAndStart();
    consumer.check(3);
  }

}
