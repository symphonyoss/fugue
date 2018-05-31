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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.CodingFault;
import org.symphonyoss.s2.common.fault.TransactionFault;

public class PanelParameter<T> implements IPanelParameter<T>
{
  protected static final String DISABLED = "disabled";

  protected static final String DisabledField = "disabledField";

  private static Logger                 log_ = LoggerFactory.getLogger(PanelParameter.class);

  private Class<T>                      type_;
  private final String                  label_;
  private final IGetter<T>              getter_;
  private final ISetter<T>              setter_;
  private final String                  id_;
  private Constructor<? extends Object> constructor_;
  private Method                        staticConstructor_;
  private T[]                           enumValues_;
  private boolean                       enabled_ = true;

  @SuppressWarnings("unchecked")
  public PanelParameter(Class<T> type, String label, IGetter<T> getter, ISetter<T> setter)
  {
    type_ = type;
    label_ = label;
    getter_ = getter;
    setter_ = setter;
    id_ = label_.replaceAll(" ", "");

    if (Enum.class.isAssignableFrom(type))
    {
      try
      {
        staticConstructor_ = type.getMethod("valueOf", String.class);

      }
      catch (NoSuchMethodException | SecurityException e)
      {
        throw new CodingFault("Enum type " + type + " has no valueOf method, which \"can't happen\"", e);
      }

      try
      {
        enumValues_ = (T[]) type.getMethod("values").invoke(null);
      }
      catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
          | SecurityException e)
      {
        throw new CodingFault("Failed to enumerate Enum type " + type + ", which \"can't happen\"", e);
      }
    }
    else
    {
      try
      {
        constructor_ = type.getConstructor(String.class);
      }
      catch (NoSuchMethodException | SecurityException e)
      {
        throw new TransactionFault("PanelParameters must be of a type with a string constructor (" + type + ").", e);
      }
    }
  }

//  public static <T> PanelParameter<T> create(GeneralConfigurationItem<T> conf)
//  {
//    return new Wrapper<T>(conf).getPanelParameter();
//  }
//
//  public static <T> PanelParameter<T> create(GeneralConfigurationItem<T> conf, GeneralConfigurationItem<?> run)
//  {
//    return new Wrapper<T>(conf, (GeneralConfigurationItem<T>) run).getPanelParameter();
//  }

  @Override
  public String getId()
  {
    return id_;
  }

  public String getLabel()
  {
    return label_;
  }

  public IGetter<T> getGetter()
  {
    return getter_;
  }

  public ISetter<T> getSetter()
  {
    return setter_;
  }

  public boolean isEnabled()
  {
    return enabled_;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void set(WizardRequest req, String value)
  {
    try
    {
      setter_.set((staticConstructor_ == null ? (T) constructor_.newInstance(value)
          : (T) staticConstructor_.invoke(null, value)));
    }
    catch (Exception e) // Yes, really, think NumberFormatException etc....
    {
      req.addError("Failed to set value of " + id_ + " from \"" + value + "\" (" + e + ")");
      log_.error("Failed to set value of " + id_ + " from \"" + value + "\"", e);
    }
  }

  @Override
  public void handleResponse(WizardRequest req)
  {
    String value = req.getHttpServletRequest().getParameter(getId());

    if (type_.isAssignableFrom(Boolean.class))
    {
      set(req, "on".equals(value) || "true".equals(value) ? "TRUE" : "FALSE");
    }
    else
    {
      if (value == null)
        req.addError("No value for " + getId() + " found.");
      else
      {
        set(req, value);
      }
    }
  }

  @Override
  public void handleContent(WizardRequest req, UIHtmlWriter out)
  {
    out.openElement("tr");
    out.printElement("td", getLabel());
    out.openElement("td");

    T value = getGetter().get();

    if(!isEnabled())
      out.printHiddenInput(getId(), value.toString());
    
    if (enumValues_ != null)
    {
      if(isEnabled())
        out.openElement("select", "name", getId());
      else
        out.openElement("select", DISABLED);

      for (T v : enumValues_)
      {
        if (v == value)
          out.printElement("option", v.toString(), "selected");
        else
          out.printElement("option", v.toString());
      }
      out.closeElement();
    }
    else if (type_.isAssignableFrom(Boolean.class))
    {
      if(isEnabled())
        out.printBooleanInput(getId(), (Boolean) value);
      else
        out.printBooleanInput(DisabledField, (Boolean) value, DISABLED);
    }
    else if (type_.isAssignableFrom(Number.class))
    {
      if(isEnabled())
        out.printNumericInput(getId(), (Number) value);
      else
        out.printNumericInput(DisabledField, (Number) value, DISABLED);
    }
    else
    {
      if(isEnabled())
        out.printTextInput(getId(), value.toString());
      else
        out.printTextInput(DisabledField, value.toString(), DISABLED);
    }
    
    out.closeElement(); // td
    out.closeElement(); // tr

  }

  public void setEnabled(boolean enabled)
  {
    enabled_ = enabled;
  }
}

//class Wrapper<TT>
//{
//  private TT                 value_;
//  private PanelParameter<TT> panelParameter_;
//
//  public Wrapper(GeneralConfigurationItem<TT> conf)
//  {
//    value_ = conf.get();
//
//    String id = conf.getId();
//
//    while (id.endsWith("."))
//      id = id.substring(0, id.length() - 2);
//
//    int i = id.lastIndexOf('.');
//
//    if (i != -1)
//      id = id.substring(i + 1);
//
//    panelParameter_ = new PanelParameter<TT>(conf.getType(), id, () ->
//    {
//      return value_;
//    }, (v) ->
//    {
//      conf.set(v);
//      value_ = v;
//    });
//  }
//
//  public Wrapper(GeneralConfigurationItem<TT> conf, GeneralConfigurationItem<TT> run)
//  {
//    value_ = conf.get();
//
//    String id = conf.getId();
//
//    while (id.endsWith("."))
//      id = id.substring(0, id.length() - 2);
//
//    int i = id.lastIndexOf('.');
//
//    if (i != -1)
//      id = id.substring(i + 1);
//
//    panelParameter_ = new PanelParameter<TT>(conf.getType(), id, () ->
//    {
//      return value_;
//    }, (v) ->
//    {
//      conf.set(v);
//
//      if (run != null)
//        run.set(v);
//
//      value_ = v;
//    });
//  }
//
//  public PanelParameter<TT> getPanelParameter()
//  {
//    return panelParameter_;
//  }
//
//}
