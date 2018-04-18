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
import javax.annotation.Nullable;

/**
 * Utility class for building namespaced names.
 * 
 * @author Bruce Skingle
 *
 */
public class NameSpace
{
  private static final String SEPARATOR = "-";

  /**
   * Build a namespaced name.
   * 
   * The namespace may be <code>null</code> the name may not. Any optional additional suffix components will be
   * appended to the final name each with the standard separator.
   * 
   * @param nameSpace   The nullable namespace
   * @param name        The name
   * @param additional  Zero or more optional suffix elements.
   * 
   * @return  A fully namespaced name for the given inputs.
   */
  public static String build(@Nullable String nameSpace, @Nonnull String name, String ...additional)
  {
    if(name == null)
      throw new NullPointerException("name may not be null");
    
    StringBuilder b;
    
    if(nameSpace == null)
    {
      b = new StringBuilder(name);
    }
    else
    {
      b = new StringBuilder(nameSpace);
      b.append(SEPARATOR);
      b.append(name);
    }
    
    for(String s : additional)
    {
      b.append(SEPARATOR);
      b.append(s);
    }
    
    return b.toString();
  }
}
