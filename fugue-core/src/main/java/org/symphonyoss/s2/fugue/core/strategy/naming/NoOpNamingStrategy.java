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

package org.symphonyoss.s2.fugue.core.strategy.naming;

import javax.annotation.Nonnull;

/**
 * A naming strategy which leaves all names unchanged.
 * 
 * This is used for the symlib implementations since that library manages namespacing.
 * 
 * @author Bruce Skingle
 *
 */
public class NoOpNamingStrategy extends AbstractNamingStrategy
{
  /**
   * Constructor.
   */
  public NoOpNamingStrategy()
  {
    super(null);
  }

  @Override
  public String getName(@Nonnull String name, String ...additional)
  {
    return NameSpace.build(null, name, additional);
  }

  @Override
  public String getSubscriptionName(String topic, String subscription)
  {
    return subscription;
  }

}
