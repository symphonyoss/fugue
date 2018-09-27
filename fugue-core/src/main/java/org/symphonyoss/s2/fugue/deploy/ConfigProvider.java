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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.symphonyoss.s2.common.dom.json.MutableJsonObject;

/**
 * A configuration provider, the primary source of configuration.
 * 
 * @author Bruce Skingle
 *
 */
public abstract class ConfigProvider extends ConfigHelper
{
  /**
   * Fetch the list of files contained in the given folder (directory).
   * Sub-folders are ignored.
   * 
   * @param folderName  The name of a folder.
   * 
   * @return A list of the names of all files in the given folder.
   */
  public abstract List<String> fetchFiles(String folderName);
  
  /**
   * Fetch the list of sub-folders contained in the given folder (directory).
   * Files are ignored.
   * 
   * @param folderName  The name of a folder.
   * 
   * @return A list of the names of all sub-folders in the given folder.
   */
  public abstract List<String> fetchDirs(String folderName);

  /**
   * Fetch the given file as a JSON object.
   * 
   * @param folderName  The name of the folder containing the file.
   * @param fileName    The name of the file.
   * 
   * @return The contents of the given file parsed as a JSON object.
   * 
   * @throws FileNotFoundException  If the file does not exist.
   * @throws IOException            If the file cannot be read.
   */
  public abstract MutableJsonObject fetchConfig(String folderName, String fileName) throws IOException;
}
