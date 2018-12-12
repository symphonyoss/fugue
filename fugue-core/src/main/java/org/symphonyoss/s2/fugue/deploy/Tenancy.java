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

package org.symphonyoss.s2.fugue.deploy;

import javax.annotation.Nullable;

/**
 * Tenancy for a service, either single or multi.
 * 
 * @author Bruce Skingle
 *
 */
public enum Tenancy
{
  /** A single tenant service */
  SINGLE,
  
  /** A multi-tenant service */
  MULTI;
  
  /**
   * Parse the given string as the name of a value of this enum.
   * 
   * Null or empty values are interpreted as MULTI and leading and trailing white space is ignored.
   * 
   * @param s A string containing the name of some element of this enum.
   * 
   * @return The enum represented by the given String.
   * 
   * @throws IllegalArgumentException if the given value is invalid.
   */
  public static Tenancy parse(@Nullable String s)
  {
    if(s==null)
      return MULTI;
    
    s = s.trim();
    
    if(s.length()==0)
      return MULTI;
    
    for(Tenancy t : values())
      if(s.equalsIgnoreCase(t.toString()))
        return t;
        
    throw new IllegalArgumentException("\"${s}\" is not a valid Tenancy");
  }
}
