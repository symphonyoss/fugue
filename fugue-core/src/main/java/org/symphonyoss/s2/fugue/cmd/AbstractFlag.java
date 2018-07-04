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

package org.symphonyoss.s2.fugue.cmd;

import org.symphonyoss.s2.fugue.cmd.CommandLineHandler.ArrayIterator;
import org.symphonyoss.s2.fugue.http.ui.servlet.ISetter;

public abstract class AbstractFlag
{
  private final Character  shortFlag_;
  private final String     longFlag_;

  public AbstractFlag(Character shortFlag, String longFlag)
  {
    if(shortFlag == null && longFlag == null)
      throw new IllegalArgumentException("Short and long flags may not both be null");
    
    shortFlag_ = shortFlag;
    longFlag_ = longFlag;
  }

  public Character getShortFlag()
  {
    return shortFlag_;
  }

  public String getLongFlag()
  {
    return longFlag_;
  }

  public abstract boolean isMultiple();
  public abstract boolean isRequired();
  
  public abstract void process(ArrayIterator it, boolean boolVal);

  @Override
  public String toString()
  {
    if(longFlag_ == null)
    {
      if(shortFlag_ == null)
        return super.toString();
      
      return "-" + shortFlag_;
    }
    else if(shortFlag_ == null)
    {
      return "--" + longFlag_;
    }
    return "--" + longFlag_ + " (-" + shortFlag_ + ")";
  }
  
  
}
