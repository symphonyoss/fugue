/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.kv;

/**
 * Implementation of IKvPagination
 * 
 * @author Bruce Skingle
 *
 */
public class KvPagination implements IKvPagination
{
  private final String before_;
  private final String after_;
  
  /**
   * Constructor.
   * 
   * @param before  before token
   * @param after   after token
   */
  public KvPagination(String before, String after)
  {
    before_ = before;
    after_ = after;
  }

  @Override
  public String getBefore()
  {
    return before_;
  }

  @Override
  public String getAfter()
  {
    return after_;
  }
}
