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

package org.symphonyoss.s2.fugue.di.component.impl;

import java.security.SecureRandom;

import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
import org.symphonyoss.s2.fugue.di.component.IRandomNumberProvider;

/**
 * A component which provides an instance of a SecureRandom.
 * 
 * @author bruce.skingle
 *
 */
public class RandomNumberProvider implements IRandomNumberProvider
{
  private SecureRandom  random_ = new SecureRandom();

  @Override
  public ComponentDescriptor getComponentDescriptor()
  {
    return new ComponentDescriptor()
        .addProvidedInterface(IRandomNumberProvider.class);
  }

  @Override
  public int nextInt()
  {
    return random_.nextInt();
  }

  @Override
  public int nextInt(int bound)
  {
    return random_.nextInt(bound);
  }

  @Override
  public long nextLong()
  {
    return random_.nextLong();
  }

  @Override
  public boolean nextBoolean()
  {
    return random_.nextBoolean();
  }

  @Override
  public void nextBytes(byte[] bytes)
  {
    random_.nextBytes(bytes);
  }

  @Override
  public float nextFloat()
  {
    return random_.nextFloat();
  }

  @Override
  public double nextDouble()
  {
    return random_.nextDouble();
  }

  @Override
  public double nextGaussian()
  {
    return random_.nextGaussian();
  }
}
