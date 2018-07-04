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

public class BooleanFlag extends AbstractFlag
{
  private final ISetter<Boolean> setter_;

  public BooleanFlag(Character shortFlag, String longFlag, ISetter<Boolean> setter)
  {
    super(shortFlag, longFlag);

    setter_ = setter;
  }

  public void set(Boolean value)
  {
    setter_.set(value);
  }

  @Override
  public boolean isMultiple()
  {
    return false;
  }

  @Override
  public boolean isRequired()
  {
    return false;
  }

  @Override
  public void process(ArrayIterator it, boolean boolVal)
  {
    set(boolVal);
  }
}
