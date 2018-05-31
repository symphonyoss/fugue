/*
 *
 *
 * Copyright 2017-2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The SSF licenses this file
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

package org.symphonyoss.s2.fugue.http.ui.servlet;

import java.util.Collection;

public class PanelListParameter<T> extends PanelParameter<T>
{
  private Collection<T> values_;

  public PanelListParameter(Class<T> type, String label, IGetter<T> getter, ISetter<T> setter,
      Collection<T> values)
  {
    super(type, label, getter, setter);
    values_ = values;
  }

  @Override
  public void handleContent(WizardRequest req, UIHtmlWriter out)
  {
    out.openElement("tr");
    out.printElement("td", getLabel());
    out.openElement("td");
    
    T value = getGetter().get();
    
    if(isEnabled())
    {
      out.openElement("select", "name", getId());
    }
    else
    {
      out.printHiddenInput(getId(), value.toString());
      out.openElement("select", DISABLED);
    }
   
    
    for(T v : values_)
    {
      if(v.equals(value))
        out.printElement("option", v.toString(), "selected");
      else
        out.printElement("option", v.toString());
    }
    out.closeElement();
  
    
    out.closeElement(); // td
    out.closeElement(); // tr
    
  }

}
