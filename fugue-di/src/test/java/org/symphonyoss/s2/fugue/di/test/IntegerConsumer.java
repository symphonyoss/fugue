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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.symphonyoss.s2.fugue.di.Cardinality;
import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.IComponent;

public class IntegerConsumer implements IComponent
{
  private final Cardinality cardinality_;

  private List<IIntegerProvider>  providers_ = new ArrayList<>();

  public IntegerConsumer(Cardinality cardinality)
  {
    cardinality_ = cardinality;
  }

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addDependency(IIntegerProvider.class, (v) -> providers_.add(v), cardinality_);
  }

  public void check(int n)
  {
    Set<Integer>  resultSet = new HashSet<>();
    Set<Integer>  unexpectedSet = new HashSet<>();
    
    while(n>0)
      resultSet.add(n--);
    
    for(IIntegerProvider p : providers_)
    {
      int v = p.getIntValue();
      
      if(!resultSet.remove(v))
        unexpectedSet.add(v);
    }
    
    if(!resultSet.isEmpty())
      throw new RuntimeException("Expected values " + resultSet + " were not received");
    
    if(!unexpectedSet.isEmpty())
      throw new RuntimeException("Unexpected values " + unexpectedSet + " were received");

  }
}
