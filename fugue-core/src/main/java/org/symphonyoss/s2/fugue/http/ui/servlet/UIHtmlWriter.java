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

import java.io.OutputStream;
import java.io.Writer;

import org.symphonyoss.s2.common.writer.HtmlWriter;

public class UIHtmlWriter extends HtmlWriter
{
  public UIHtmlWriter(OutputStream out)
  {
    super(out);
  }

  public UIHtmlWriter(Writer outputStream)
  {
    super(outputStream);
  }

  public void openError()
  {
    openElement("p", "class", "error");
  }

  public void printError(String error)
  {
    openError();
    println(error);
    closeElement();
  }

  public void printError(String error, Throwable cause)
  {
    openError();
    println(error);
    cause.printStackTrace(this);
    closeElement();
  }

  public void openForm(String action, String ... attributes)
  {
    startOpenElement("form", "action", action);
    finishElement(attributes);
  }
  
  public void printTextInput(String name, String value, String ... attributes)
  {
    startElement("input", "type", "text", "name", name, "value", value);
    finishEmptyElement(attributes);
  }
  
  public void printHiddenInput(String name, String value, String ... attributes)
  {
    startElement("input", "type", "hidden", "name", name, "value", value);
    finishEmptyElement(attributes);
  }
  
  public void printNumericInput(String name, Number value, String ... attributes)
  {
    startElement("input", "type", "text", "style", "text-align: right;", "name", name, "value", String.valueOf(value));
    finishEmptyElement(attributes);
  }
  
  public void printBooleanInput(String name, Boolean value, String ... attributes)
  {
    if(value)
      startElement("input", "type", "checkbox", "name", name, "checked");
    else
      startElement("input", "type", "checkbox", "name", name);
    
    finishEmptyElement(attributes);
  }
  
  public void printRadioInput(String name, String value, String ... attributes)
  {
    startElement("input", "type", "radio", "name", name, "value", value);
    finishEmptyElement(attributes);
  }
}
