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

public class Flag<T> extends AbstractFlag
{
  private final Class<T>            type_;
  private final boolean             multiple_;
  private final boolean             required_;
  private final ISetter<T>          setter_;
  private final IStringConverter<T> converter_;

  public Flag(Character shortFlag, String longFlag, String envName, Class<T> type, boolean multiple, boolean required, ISetter<T> setter)
  {
    super(shortFlag, longFlag, envName);
    
    type_ = type;
    multiple_ = multiple;
    required_ = required;
    setter_ = setter;
    converter_ = StringConverter.get(type_);
    
    if(converter_ == null)
      throw new IllegalArgumentException("Unknown type \"" + type + "\"");
  }

  public Class<T> getType()
  {
    return type_;
  }

  public boolean isMultiple()
  {
    return multiple_;
  }

  public boolean isRequired()
  {
    return required_;
  }

  public void set(T value)
  {
    setter_.set(value);
  }

  public void process(ArrayIterator it, boolean boolVal)
  {
    if(it.hasNext())
      set(converter_.convert(it.next()));
    else
      throw new IllegalArgumentException(this + ": value required.");
  }

  @Override
  public void process(String value)
  {
    set(converter_.convert(value));
  }
}
