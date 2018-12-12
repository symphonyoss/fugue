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

/**
 * The type of a container.
 * 
 * @author Bruce Skingle
 *
 */
public enum ContainerType
{
  /** Long lived runtime docker images. */
  SERVICE,
  /** Docker images executed at deploy time, expected to run to completion. */
  INIT,
  /** Docker images executed on a schedule, expected to run to completion. */
  SCHEDULED,
  /** Lambda executable jar images. */
  LAMBDA;
  
  /**
   * Parse the given string as the name of a value of this enum.
   * 
   * Null or empty values are interpreted as SERVICE and leading and trailing white space is ignored.
   * 
   * @param s A string containing the name of some element of this enum.
   * 
   * @return The enum represented by the given String.
   * 
   * @throws IllegalArgumentException if the given value is invalid.
   */
  public static ContainerType parse(String s)
  {
    if(s==null)
      return SERVICE;
    
    s = s.trim();
    
    if(s.length()==0)
      return SERVICE;
      
    for(ContainerType t : values())
      if(s.equalsIgnoreCase(t.toString()))
        return t;
        
    throw new IllegalArgumentException("\"${s}\" is not a valid ContainerType");
  }
}
