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

package org.symphonyoss.s2.fugue.aws.lambda;

import org.symphonyoss.s2.fugue.lambda.JsonLambdaResponse;
import org.symphonyoss.s2.fugue.lambda.LambdaRequest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A response from an Amazon API Gateway Lambda Authorizer.
 * 
 * @author Bruce Skingle
 *
 */
public class AwsLambdaAuthResponse extends JsonLambdaResponse
{
  private static final String STATUS_CODE = "statusCode";
  private static final String BODY = "body";

  public AwsLambdaAuthResponse(AwsLambdaRequest request, boolean allowRequest)
  {
    this(request, allowRequest, "user");
  }

  public AwsLambdaAuthResponse(AwsLambdaRequest request, boolean allowRequest, String principalId)
  {
    String methodArn = request.getString("methodArn"); // "arn:aws:execute-api:us-east-1:123456789012:s4x3opwd6i/test/GET/request"
    System.out.println("methodArn=" + methodArn);
    
//    String[] arnPartials = methodArn.split(":");
//    String region = arnPartials[3];
//    String awsAccountId = arnPartials[4];
//    String[] apiGatewayArnPartials = arnPartials[5].split("/");
//    String restApiId = apiGatewayArnPartials[0];
//    String stage = apiGatewayArnPartials[1];
//    String httpMethod = apiGatewayArnPartials[2];
//    String resource = ""; // root resource
//    if (apiGatewayArnPartials.length == 4) {
//      resource = apiGatewayArnPartials[3];
//    }
    
    put("principalId", principalId);
    
    ObjectNode policy = putObject("policyDocument");
    
    policy.put("Version", "2012-10-17");
    
    ArrayNode statementList = policy.putArray("Statement");
    
    ObjectNode statement =  statementList.addObject();
    
    statement.put("Action", "execute-api:Invoke");
    statement.put("Effect", allowRequest ? "Allow" : "Deny");
    statement.put("Resource", methodArn);
    
    System.out.println("AUTH=" + allowRequest + " arn=" + methodArn);
  }
}
