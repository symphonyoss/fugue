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

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.symphonyoss.s2.fugue.http.ui.servlet.ICommandHandler;
import org.symphonyoss.s2.fugue.http.ui.servlet.IUIPanel;

/**
 * The main component for a Fugue process.
 * 
 * @author Bruce Skingle
 *
 */
public interface IFugueServer extends IFugeComponentContainer<IFugueServer>
{
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
  IFugueServer join() throws InterruptedException;

  /**
   * Put the server into a failed state.
   * 
   * @return this (fluent method) 
   */
  IFugueServer fail();

  /**
   * Create a new ExecutorService with a thread pool using the given name.
   * 
   * If tasks submitted to this service throw exceptions then they are handled the Fugue way.
   * 
   * The ExecutorService will be shut down when the server terminates.
   * 
   * @param name The name for threads in the executor.
   * 
   * @return A new ExecutorService.
   */
  ExecutorService newExecutor(String name);

  /**
   * Create a new ExecutorService using the given underlying service.
   * 
   * If tasks submitted to this service throw exceptions then they are handled the Fugue way.
   * 
   * The ExecutorService will be shut down when the server terminates.
   * 
   * @param exec  An executor to so the actual work.
   * 
   * @return A new ExecutorService.
   */
  ExecutorService newExecutor(ExecutorService exec);

  /**
   * Create a new ScheduledExecutorService with a thread pool using the given name.
   * 
   * If tasks submitted to this service throw exceptions then they are handled the Fugue way.
   * 
   * The ExecutorService will be shut down when the server terminates.
   * 
   * @param name The name for threads in the executor.
   * 
   * @return A new ScheduledExecutorService.
   */
  ScheduledExecutorService newScheduledExecutor(String name);

  /**
   * Create a new ScheduledExecutorService using the given underlying service.
   * 
   * If tasks submitted to this service throw exceptions then they are handled the Fugue way.
   * 
   * The ExecutorService will be shut down when the server terminates.
   * 
   * @param exec  An executor to so the actual work.
   * 
   * @return A new ScheduledExecutorService.
   */
  ScheduledExecutorService newScheduledExecutor(ScheduledExecutorService exec);

  /**
   * Return the instanceId for this process.
   * 
   * @return the instanceId.
   */
  static String getInstanceId()
  {
    String instanceId = System.getProperty(Fugue.FUGUE_INSTANCE);
    
    if(instanceId == null)
      instanceId = System.getenv(Fugue.FUGUE_INSTANCE);
    
    if(instanceId == null)
      return "UNKNOWN";
    else
      return instanceId;
  }

  /**
   * Add the current thread to the list of managed threads.
   * 
   * Managed threads are interrupted when the server shuts down.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withCurrentThread();

  /**
   * Add the given thread to the list of managed threads.
   * 
   * Managed threads are interrupted when the server shuts down.
   * 
   * @param thread The thread to be managed.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withThread(Thread thread);

  /**
   * Add the given UIPanel to this server.
   * 
   * @param panel A UIPanel.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withPanel(IUIPanel panel);

  /**
   * Add the given UIPanel to this server and make it the default panel.
   * 
   * @param panel A UIPanel.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withDefaultPanel(IUIPanel panel);

  /**
   * Add the given command to this server.
   * 
   * @param path            The servlet path for the command servlet.
   * @param name            The command name for the UI.
   * @param validStates     Lifecycle states from which this command can be invoked.
   * @param handler         The handler which implements the command.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withCommand(String path, String name, EnumSet<FugueLifecycleState> validStates, ICommandHandler handler);

  /**
   * Generate a random one time authentication token and invoke the local browser to connect to the running server.
   * 
   * @return this (Fluent method).
   */
  IFugueServer withLocalWebLogin();

  /**
   * 
   * @return The application name.
   */
  String getApplicationName();

  /**
   * 
   * @return the http port in use.
   */
  int getHttpPort();

}
