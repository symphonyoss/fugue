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

package org.symphonyoss.s2.fugue.aws.lambda.auth;

import org.symphonyoss.s2.fugue.lambda.LambdaRequest;

public class EnvVarAPIGatewayAuthorizer extends APIGatewayAuthorizer
{
  private static final String   ENV_TOKEN_NAME   = "API_KEY_NAME";
  private static final String   ENV_TOKEN_VALUES = "API_KEY_VALUES";

  private static final String   apiKeyName_      = System.getenv(ENV_TOKEN_NAME);
  private static final String[] apiKeyValues_    = System.getenv(ENV_TOKEN_VALUES).split(" *, *");
  
  @Override
  protected boolean allowRequest(LambdaRequest request)
  {
    String apiKey = request.getParameter(apiKeyName_);

    System.out.println("apiKeyName_ = " + apiKeyName_);
    System.out.println("apiKeyValues_ = " + apiKeyValues_[0]);
    System.out.println("apiKey = " + apiKey);
    if(apiKey != null)
    {
      for(String value : apiKeyValues_)
      {
        if(apiKey.equals(value))
        {
          System.out.println("Thats ok");
          return true;
        }
      }
    }
    System.out.println("Thats a NO THEN");
    return false;
  }

}
