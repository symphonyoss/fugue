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

package org.symphonyoss.s2.fugue.aws.metrics;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.symphonyoss.s2.fugue.IFugueServer;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.metrics.IMetricManager;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.naming.NameFactory;

import com.amazonaws.metrics.AwsSdkMetrics;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class AwsMetricManager implements IMetricManager
{
  private final AmazonCloudWatch cwClient_;
  private String nameSpace_;
  private String tenantId_;
  
  public AwsMetricManager(IConfiguration config, INameFactory nameFactory)
  {
    cwClient_ =
        AmazonCloudWatchClientBuilder.defaultClient();
    
    nameSpace_ = nameFactory.getMultiTenantServiceName().toString();
    tenantId_ = config.getString("id/tenantId", null);
    
    AwsSdkMetrics.enableDefaultMetrics();

//  AwsSdkMetrics.setCredentialProvider(credentialsProvider);

    AwsSdkMetrics.setMetricNameSpace(nameSpace_);
  }

  @Override
  public void putMetric(long timestamp, int count)
  {
    List<Dimension> dimensions = new LinkedList<>();
    
    dimensions.add(new Dimension()
      .withName("MessageType")
      .withValue("PubSub"));
    
    if(tenantId_ != null)
      dimensions.add(new Dimension()
          .withName("Tenant")
          .withValue(tenantId_));
    
    dimensions.add(new Dimension()
      .withName("Instance")
      .withValue(IFugueServer.getInstanceId()));
    
    MetricDatum datum = new MetricDatum()
        .withMetricName("MessageCount")
        .withUnit(StandardUnit.Count)
        .withValue((double)count)
        .withDimensions(
            dimensions)
        .withTimestamp(new Date(timestamp))
        ;

    PutMetricDataRequest request = new PutMetricDataRequest()
        .withNamespace(nameSpace_)
        .withMetricData(datum);

    cwClient_.putMetricData(request);
  }
}
