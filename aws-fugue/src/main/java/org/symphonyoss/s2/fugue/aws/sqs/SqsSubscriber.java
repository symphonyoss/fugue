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

package org.symphonyoss.s2.fugue.aws.sqs;

import java.util.List;

import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

/**
 * An SWS SNS subscriber.
 * 
 * @author Bruce Skingle
 *
 */
/* package */ class SqsSubscriber implements Runnable
{
  private final SqsAbstractSubscriberManager<?>      manager_;
  private final AmazonSQS                            sqsClient_;
  private final String                               queueUrl_;
  private final ITraceContextFactory                 traceFactory_;
  private final IThreadSafeRetryableConsumer<String> consumer_;

  /* package */ SqsSubscriber(SqsAbstractSubscriberManager<?> manager, AmazonSQS sqsClient, String queueUrl,
      ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<String> consumer)
  {
    manager_ = manager;
    sqsClient_ = sqsClient;
    queueUrl_ = queueUrl;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
  }

  @Override
  public void run()
  {
    // receive messages from the queue
    
    ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl_)
        .withMaxNumberOfMessages(10)
        .withWaitTimeSeconds(20);
    try
    {    
      List<Message> messages = sqsClient_.receiveMessage(request).getMessages();
  
      for (Message m : messages)
      {

        ITraceContext trace = traceFactory_.createTransaction("SQS_Message", m.getMessageId());

        long retryTime = manager_.handleMessage(consumer_, m.getBody(), trace);
        
        if(retryTime < 0)
        {
          trace.trace("ABOUT_TO_ACK");
          sqsClient_.deleteMessage(queueUrl_, m.getReceiptHandle());
        }
        else
        {
          trace.trace("ABOUT_TO_NACK");
          // TODO: do we need to do something here ?
        }
        trace.finished();
      }
    }
    catch (RuntimeException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
   
    
    manager_.submit(this);
  }
}
