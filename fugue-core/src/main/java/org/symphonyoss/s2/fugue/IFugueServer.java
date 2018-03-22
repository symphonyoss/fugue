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

package org.symphonyoss.s2.fugue;

/**
 * The main component for a Fugue process.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueServer
{
  /**
   * Start the server and return.
   * 
   * Unless some component starts a non-daemon thread the process will terminate. If no thread
   * exists to keep the application alive then call join() after this method returns since
   * this method is fluent you can call <code>start().join()</code>.
   * 
   * @return this (fluent method) 
   */
  FugueServer start();

  /**
   * Join the calling thread to the server process.
   * 
   * This method will block until the server exits. This is useful if the main thread of the process has nothing
   * to do apart from to start the server.
   * 
   * @return this (fluent method)
   * 
   * @throws InterruptedException If the thread is interrupted.
   */
  FugueServer join() throws InterruptedException;

  /**
   * Stop the server.
   * 
   * @return this (fluent method) 
   */
  FugueServer stop();

}
