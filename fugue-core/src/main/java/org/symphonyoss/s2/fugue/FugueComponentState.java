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

/**
 * The state of a FugueComponent.
 * 
 * @author Bruce Skingle
 *
 */
public enum FugueComponentState
{
  /** Everything is good. */
  OK,
  
  /** Something is wrong but all services are being provided. */
  Warn,
  
  /** Something is wrong and some services are being impacted but there is still some level of useful service provided. */
  Error,
  
  /** Something is wrong and no useful service is being provided. */
  Failed;
}
