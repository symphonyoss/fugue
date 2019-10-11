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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.s2.common.fault.TransactionFault;
import org.symphonyoss.s2.fugue.pubsub.IQueueMessage;
import org.symphonyoss.s2.fugue.pubsub.IQueueMessageDelete;
import org.symphonyoss.s2.fugue.pubsub.IQueueMessageExtend;
import org.symphonyoss.s2.fugue.pubsub.IQueueReceiver;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueDoesNotExistException;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

/**
 * AWS SQS implementation of QueueReceiver.
 * 
 * 
 * @author Bruce Skingle
 *
 */
public class SqsQueueReceiver implements IQueueReceiver
{
  private static final Logger log_ = LoggerFactory.getLogger(SqsQueueReceiver.class);

  private final AmazonSQS     sqsClient_;
  private final String        queueUrl_;

  /**
   * Constructor.
   * 
   * @param sqsClient An SQS client.
   * @param queueName The name of a queue
   * 
   * @throws QueueDoesNotExistException if the queue does not exist.
   */
  SqsQueueReceiver(AmazonSQS sqsClient, String queueName)
  {
    sqsClient_     = sqsClient;
    
    queueUrl_ = sqsClient_.getQueueUrl(queueName).getQueueUrl();

    log_.info("Queue " + queueName + " exists as " + queueUrl_);
  }
  
  @Override
  public @Nonnull Collection<IQueueMessage> receiveMessages(int maxMessages, int waitTimeSeconds, Set<? extends IQueueMessageDelete> deleteMessages, Set<? extends IQueueMessageExtend> extendMessages)
  {
    try
    {
      List<IQueueMessage> messages = new ArrayList<>(maxMessages);
      
      for(IQueueMessageDelete delete : deleteMessages)
      {
        try
        {
          sqsClient_.deleteMessage(queueUrl_, delete.getReceiptHandle());
          log_.debug("Deleteed message " + delete.getReceiptHandle());
        }
        catch(AmazonSQSException e)
        {
          log_.warn("Failed to delete message", e);
        }
      }
      

      for(IQueueMessageExtend extend : extendMessages)
      {
        if(extend.getVisibilityTimeout() != null) 
        {
          try
          {
            sqsClient_.changeMessageVisibility(queueUrl_, extend.getReceiptHandle(), extend.getVisibilityTimeout());
            log_.debug("Extended message " + extend.getReceiptHandle() + " with delay " +  extend.getVisibilityTimeout());
          }
          catch(AmazonSQSException e)
          {
            log_.warn("Failed to extend message", e);
          }
        }
        else
        {
          log_.debug("Extended message " + extend.getReceiptHandle() + " with default delay delay (actually did nothing)");
        }
      }
      
      if(maxMessages > 0)
      {
        log_.debug("About to receive messages...");
        for(Message receivedMessage : sqsClient_.receiveMessage(
            new ReceiveMessageRequest(queueUrl_)
              .withMaxNumberOfMessages(maxMessages)
              .withWaitTimeSeconds(waitTimeSeconds)
              )
            .getMessages())
        {
          messages.add(new SqsQueueMessage(receivedMessage));
        }
      }
      
      log_.debug("Returning " + messages.size() + " messages");
      return messages;
    }
    catch (RuntimeException e)
    {
      throw new TransactionFault(e);
    }
  }
  
  private class SqsQueueMessage implements IQueueMessage
  {
    private final Message receivedMessage_;
    
    public SqsQueueMessage(Message receivedMessage)
    {
      receivedMessage_ = receivedMessage;
    }

    @Override
    public String getMessageId()
    {
      return receivedMessage_.getMessageId();
    }

    @Override
    public String getReceiptHandle()
    {
      return receivedMessage_.getReceiptHandle();
    }

    @Override
    public String getPayload()
    {
      return receivedMessage_.getBody();
    }    
  }
}
