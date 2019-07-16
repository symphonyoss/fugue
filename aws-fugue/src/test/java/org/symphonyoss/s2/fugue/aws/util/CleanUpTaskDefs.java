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

package org.symphonyoss.s2.fugue.aws.util;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.DeregisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionFamiliesRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionFamiliesResult;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsResult;
import com.amazonaws.services.ecs.model.TaskDefinitionFamilyStatus;

public class CleanUpTaskDefs
{
  private final String        awsClientRegion_     = "us-east-1";
  private AmazonECS ecsClient_;
  
  public CleanUpTaskDefs()
  {
    ecsClient_ = AmazonECSClientBuilder.standard()
        .withRegion(awsClientRegion_)
        .build();
  }
  
  public void run(String familyPrefix) throws InterruptedException
  {
    
    ListTaskDefinitionFamiliesResult families = ecsClient_.listTaskDefinitionFamilies(new ListTaskDefinitionFamiliesRequest()
        .withStatus(TaskDefinitionFamilyStatus.ACTIVE)
        .withFamilyPrefix(familyPrefix));
    
    for(String family : families.getFamilies())
    {
      System.out.println("Family " + family);
      
      ListTaskDefinitionsResult taskDefinitions = ecsClient_.listTaskDefinitions(new ListTaskDefinitionsRequest()
          .withFamilyPrefix(family));
          
      for(String taskDef : taskDefinitions.getTaskDefinitionArns())
      {
        System.out.println("taskDef " + taskDef);
        boolean retry = true;
        
        while(retry)
        {
          try
          {
            ecsClient_.deregisterTaskDefinition(new DeregisterTaskDefinitionRequest()
              .withTaskDefinition(taskDef));
            
            retry = false;
          }
          catch(AmazonECSException e)
          {
            System.err.println("Sleep....");
            Thread.sleep(5000);
          }
        }
      }
    }
  }

  
  public static void main(String[] argv) throws InterruptedException
  {
    new CleanUpTaskDefs().run(argv[0]);
  }
}
