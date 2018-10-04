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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger log_ = LoggerFactory.getLogger(SqsSubscriber.class);
  
  private final SqsSubscriberManager                 manager_;
  private final AmazonSQS                            sqsClient_;
  private final String                               queueUrl_;
  private final ITraceContextFactory                 traceFactory_;
  private final IThreadSafeRetryableConsumer<String> consumer_;
  private final NonIdleSubscriber                    nonIdleSubscriber_;
  private int messageBatchSize_ = 10;

  /* package */ SqsSubscriber(SqsSubscriberManager manager, AmazonSQS sqsClient, String queueUrl,
      ITraceContextFactory traceFactory,
      IThreadSafeRetryableConsumer<String> consumer)
  {
    manager_ = manager;
    sqsClient_ = sqsClient;
    queueUrl_ = queueUrl;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
  }
  
  class NonIdleSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      SqsSubscriber.this.run(false);
    }
  }

  @Override
  public void run()
  {
    run(true);
  }

  public void run(boolean runIfIdle)
  {
    // receive messages from the queue
    
    ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl_)
        .withMaxNumberOfMessages(messageBatchSize_ )
        .withWaitTimeSeconds(20);
    try
    {    
      List<Message> messages = sqsClient_.receiveMessage(request).getMessages();
  
      if(messages.isEmpty())
      {
        if(runIfIdle)
        {
          manager_.submit(this, runIfIdle);
          log_.info("Idle schedule " + queueUrl_);
          manager_.printQueueSize();
        }
      }
      else
      {
        log_.info("Read " + messages.size() + " for " + queueUrl_);
        
        // We need to do this before handling the message so that if there is an exception in thehandler code
        // we don't loose this topic.
        
        if(runIfIdle)
        {
          manager_.submit(this, runIfIdle);
          log_.debug("Idle re-schedule " + queueUrl_);
        }
        else
        {
          manager_.submit(nonIdleSubscriber_, runIfIdle);
          log_.debug("Extra re-schedule " + queueUrl_);
        }
        manager_.printQueueSize();
        
        if(messages.size() > 2 /*== messageBatchSize_*/)
        {
          manager_.submit(nonIdleSubscriber_, false);

          log_.debug("Extra schedule " + queueUrl_);
        }
        
        for (Message m : messages)
        {
          ITraceContext trace = traceFactory_.createTransaction("SQS_Message", m.getMessageId());
  
          long retryTime = manager_.handleMessage(consumer_, m.getBody(), trace, m.getMessageId());
          
          if(retryTime < 0)
          {
            trace.trace("ABOUT_TO_ACK");
            sqsClient_.deleteMessage(queueUrl_, m.getReceiptHandle());
          }
          else
          {
            trace.trace("ABOUT_TO_NACK");
            
            int visibilityTimout = (int) (retryTime / 1000);
            
            sqsClient_.changeMessageVisibility(queueUrl_, m.getReceiptHandle(), visibilityTimout);
          }
          trace.finished();
        }
        

      }
    }
    catch (Throwable e)
    {
      /*
       * This method is called from an executor so I am catching Throwable because otherwise Errors will
       * cause the process to fail silently.
       * 
       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
       * I think it's worth trying.
       */
      
      log_.error("Error processing message", e);
    }
   
    
    
  }
}
