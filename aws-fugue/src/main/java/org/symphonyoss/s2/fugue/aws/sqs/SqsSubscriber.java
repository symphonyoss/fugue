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
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.fugue.core.trace.ITraceContext;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransaction;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextTransactionFactory;
import org.symphonyoss.s2.fugue.counter.ICounter;
import org.symphonyoss.s2.fugue.deploy.IBatch;
import org.symphonyoss.s2.fugue.pipeline.FatalConsumerException;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeRetryableConsumer;
import org.symphonyoss.s2.fugue.pipeline.RetryableConsumerException;

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
  private final ITraceContextTransactionFactory                 traceFactory_;
  private final IThreadSafeRetryableConsumer<String> consumer_;
  private final NonIdleSubscriber                    nonIdleSubscriber_;
  private final ICounter                             counter_;
  private final String                               tenantId_;
  private int                                        messageBatchSize_ = 10;

  private boolean running_ = true;


  /* package */ SqsSubscriber(SqsSubscriberManager manager, AmazonSQS sqsClient, String queueUrl,
      ITraceContextTransactionFactory traceFactory,
      IThreadSafeRetryableConsumer<String> consumer, ICounter counter, String tenantId)
  {
    manager_ = manager;
    sqsClient_ = sqsClient;
    queueUrl_ = queueUrl;
    traceFactory_ = traceFactory;
    consumer_ = consumer;
    nonIdleSubscriber_ = new NonIdleSubscriber();
    counter_ = counter;
    tenantId_ = tenantId;
  }
  
  class NonIdleSubscriber implements Runnable
  {
    @Override
    public void run()
    {
      SqsSubscriber.this.run(false);
    }
  }

  public String getQueueUrl()
  {
    return queueUrl_;
  }

  @Override
  public void run()
  {
    run(true);
  }

  public void run(boolean runIfIdle)
  {
    if(isRunning())
    {
      if(runIfIdle)
      {
        try
        {
          while(isRunning())
          {
            getSomeMessages();
          }
        }
        finally
        {
          if(runIfIdle && isRunning())
          {
            // This "can't happen"
            log_.error("Main SQS thread returned, rescheduling...");
            
            manager_.submit(this, true);
          }
        }
      }
      else
      {
        if(isRunning())
        {
          getSomeMessages();
        }
      }
    }
  }
  
  private void getSomeMessages()
  {
    // receive messages from the queue
    
    ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl_)
        .withMaxNumberOfMessages(messageBatchSize_ )
        .withWaitTimeSeconds(20);
    try
    {    
      List<Message> messages = sqsClient_.receiveMessage(request).getMessages();
  
      log_.info("Read " + messages.size() + " for " + queueUrl_);
      
      switch(messages.size())
      {
        case 0:
          // Nothing to do...
          break;
          
        case 1:
          // Single message, just process in the current thread
          if(counter_ != null)
            counter_.increment(1);
          handleMessage(messages.get(0));
          break;
          
        default:
          if(counter_ != null)
            counter_.increment(messages.size());
          if(messages.size() > 2 && isRunning())
          {
            manager_.submit(nonIdleSubscriber_, false);

            log_.debug("Extra schedule " + queueUrl_);
          }
          
          // Fire off all but one envelopes in its own thread, do the final one in the current thread
          int     index = 0;
          IBatch  batch = manager_.newBatch();
          
          try
          {
            while(index < messages.size() - 1)
            {
              final int myIndex = index++;
              
              batch.submit(() -> 
              {
                handleMessage(messages.get(myIndex));
              });
            }
            handleMessage(messages.get(index));
            
            batch.waitForAllTasks();
          }
          catch(RuntimeException e)
          {
            Throwable cause = e.getCause();
            
            if(cause instanceof ExecutionException)
              cause = cause.getCause();
            
            if(cause instanceof RetryableConsumerException)
            {
              throw (RetryableConsumerException)cause;
            }            
            if(cause instanceof FatalConsumerException)
            {
              throw (FatalConsumerException)cause;
            }
            throw e;
          }
      }
    }
    catch(RuntimeException e)
    {
      log_.error("Error processing message", e);
    }
    catch (Throwable e)
    {
      /*
       * This method is called from an executor so I am catching Throwable because otherwise Errors will
       * be swallowed.
       * 
       * If we are catching an OutOfMemoryError then it may be futile to try to log this but on balance
       * I think it's worth trying.
       */
      
      try
      {
        log_.error("Error processing message", e);
      }
      finally
      {
        System.exit(1);
      }
    }
  }

  private void handleMessage(Message message)
  {
    try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("PubSub:SQS", message.getMessageId(), tenantId_))
    {
      ITraceContext trace = traceTransaction.open();
      
      long retryTime = manager_.handleMessage(consumer_, message.getBody(), trace, message.getMessageId());
      
      if(retryTime < 0)
      {
        trace.trace("ABOUT_TO_ACK");
        sqsClient_.deleteMessage(queueUrl_, message.getReceiptHandle());
        traceTransaction.finished();
      }
      else
      {
        trace.trace("ABOUT_TO_NACK");
        
        int visibilityTimout = (int) (retryTime / 1000);
        
        sqsClient_.changeMessageVisibility(queueUrl_, message.getReceiptHandle(), visibilityTimout);
        traceTransaction.aborted();
      }
    }
  }

  synchronized boolean isRunning()
  {
    return running_;
  }
  
  public synchronized void stop()
  {
    running_ = false;
  }
}
