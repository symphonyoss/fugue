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

package org.symphonyoss.s2.fugue.pubsub;

import org.symphonyoss.s2.common.fluent.IFluent;

/**
 * A subscriber manager of payload type P.
 * 
 * @author Bruce Skingle
 *
 * @param <P> The type of payload received.
 * @param <T> Type of concrete manager, needed for fluent methods.
 */
public interface ISubscriberManager<P,T extends ISubscriberManager<P,T>> extends IFluent<T>
{
  
}
