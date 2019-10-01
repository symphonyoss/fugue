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
import org.symphonyoss.s2.fugue.pubsub.IQueueMessageAck;
import org.symphonyoss.s2.fugue.pubsub.IQueueMessageNak;
import org.symphonyoss.s2.fugue.pubsub.IQueueReceiver;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
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
  public @Nonnull Collection<IQueueMessage> receiveMessages(int maxMessages, Set<? extends IQueueMessageAck> ackMessages, Set<? extends IQueueMessageNak> nakMessages)
  {
    try
    {
      List<IQueueMessage> messages = new ArrayList<>(maxMessages);
      
      for(IQueueMessageAck ack : ackMessages)
      {
        sqsClient_.deleteMessage(queueUrl_, ack.getReceiptHandle());
      }
      

      for(IQueueMessageNak nak : nakMessages)
      {
        if(nak.getNakDelay() != null) 
        {
          sqsClient_.changeMessageVisibility(queueUrl_, nak.getReceiptHandle(), nak.getNakDelay());
        }
      }
      
      for(Message receivedMessage : sqsClient_.receiveMessage(
          new ReceiveMessageRequest(queueUrl_)
            .withMaxNumberOfMessages(maxMessages)
            .withWaitTimeSeconds(20)
            )
          .getMessages())
      {
        messages.add(new SqsQueueMessage(receivedMessage));
      }
      
      return messages;
    }
    catch (RuntimeException e)
    {
      throw new TransactionFault(e);
    }
  }

  private static MessageAttributeValue getAttribute(Object value)
  {
    if(value instanceof Number)
    {
      return new MessageAttributeValue()
          .withDataType("Number")
          .withStringValue(value.toString());
    }
    
    return new MessageAttributeValue()
        .withDataType("String")
        .withStringValue(value.toString());
    
  }
  
  private class SqsQueueMessage implements IQueueMessage
  {
    private final Message receivedMessage_;
    

    public SqsQueueMessage(Message receivedMessage)
    {
      receivedMessage_ = receivedMessage;
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
