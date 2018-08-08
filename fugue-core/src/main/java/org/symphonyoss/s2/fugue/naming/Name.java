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

package org.symphonyoss.s2.fugue.naming;

import javax.annotation.Nonnull;

public class Name
{
  public static final String SEPARATOR = "-";
  
  private final String name_;

  /**
   * Base class for Names.
   * 
   * The name may not be <code>null</code>. Any optional additional non-null suffix components will be
   * appended to the final name each with the standard separator.
   * 
   * @param name        The name
   * @param additional  Zero or more optional suffix elements.
   */
  public Name(@Nonnull String name, String ...additional)
  {
    if(name == null)
      throw new NullPointerException("name may not be null");
    
    StringBuilder b = new StringBuilder(name);
    
    for(String s : additional)
    {
      if(s != null)
      {
        b.append(SEPARATOR);
        b.append(s);
      }
    }
    
    name_ = b.toString();
  }

  @Override
  public String toString()
  {
    return name_;
  }
}
