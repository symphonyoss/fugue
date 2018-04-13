/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

//import static com.symphony.s2.legacy.message.forwarder.Constants.ENCRYPTION_KEY;
//import static com.symphony.s2.legacy.message.forwarder.Constants.INPUT_SUBSCRIPTION;
//import static com.symphony.s2.legacy.message.forwarder.Constants.INPUT_TOPICS;
//import static com.symphony.s2.legacy.message.forwarder.Constants.NAMESPACE;
//import static com.symphony.s2.legacy.message.forwarder.Constants.SOURCE_MSGBUS;
//import static com.symphony.s2.legacy.message.forwarder.Constants.USE_ENCRYPTION;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.codec.binary.Base64;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.symphonyoss.s2.fugue.di.ComponentDescriptor;
//
//import com.google.common.util.concurrent.MoreExecutors;
//import com.symphony.messaging.common.crypto.Cryptor;
//import com.symphony.messaging.common.exception.MessageBusCryptoException;
//import com.symphony.messaging.common.io.Deserializer;
//import com.symphony.messaging.pojo.ReceivedMessageBusMsg;
//import com.symphony.s2.fugue.config.IExtendedConfigurationStore;
//import com.symphony.s2.legacy.message.forwarder.ILegacyMessageHandler;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.AckReplyConsumer;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.MessageReceiver;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.PubsubMessage;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.S2MessageForwarderServer;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.Subscriber;
//import com.symphony.s2.legacy.message.forwarder.symlib.allinone.SubscriptionName;
//import com.symphony.s2.model.fundamental.canon.facade.IFundamental;
//import com.symphony.s2.model.fundamental.canon.facade.ITraceTransaction;
//import com.symphony.s2.model.fundamental.canon.facade.TraceTransaction;

public class PubSubSubscriber
{
//  private static final Logger         log_            = LoggerFactory.getLogger(PubSubSubscriber.class);
//
//  private String                      nameSpace_;
//  private String                      subscription_;
//
//  private Cryptor                     cryptor_        = new Cryptor();
//
//  private List<Subscriber>            subscriberList_ = new ArrayList<>();
//  private byte[]                      secretKey_;
//  private ILegacyMessageHandler       eventForwarder_;
//  private IExtendedConfigurationStore config_;
//  private boolean                     useMsgBusEncryption_;
//
//  private TraceTransaction.Factory traceFactory_;
//
//
//  @Override
//  public ComponentDescriptor getComponentDescriptor()
//  {
//    return new ComponentDescriptor()
//        .addDependency(IExtendedConfigurationStore.class, (v) -> config_ = v)
//        .addDependency(ILegacyMessageHandler.class, (v) -> eventForwarder_ = v)
//        .addDependency(IFundamental.class, (v) -> traceFactory_ = v.getTraceTransactionFactory())
//        .addStart(() -> startPubSub())
//        .addStop(() -> stopPubSub());
//  }
//
//  private void startPubSub()
//  {
//    log_.info("startPubSub");
//    
//    IExtendedConfigurationStore sourceMsgBus = config_.getConfiguration(SOURCE_MSGBUS);
//    
//    nameSpace_ = sourceMsgBus.getRequiredString(NAMESPACE);
//    
//    subscription_ = config_.getRequiredString(INPUT_SUBSCRIPTION);
//    useMsgBusEncryption_ = sourceMsgBus.getRequiredBoolean(USE_ENCRYPTION);
//    
//    if(useMsgBusEncryption_)
//    {
//      secretKey_ = Base64
//        .decodeBase64(sourceMsgBus.getRequiredString(ENCRYPTION_KEY));
//    }
//    
//    // Your Google Cloud Platform project ID
//    String projectId = ServiceOptions.getDefaultProjectId();
//    
//    for(String topicId : config_.getRequiredStringArray(INPUT_TOPICS))
//    {
//      startSubscriber(projectId, nameSpace_ + "-" + topicId + "-" + subscription_);
//    }
//  }
//
//  
//
//  private void startSubscriber(String projectId, String subscriptionId)
//  {
//    SubscriptionName subscriptionName = SubscriptionName.of(projectId, subscriptionId);
//    // Instantiate an asynchronous message receiver
//    MessageReceiver receiver = new MessageReceiver()
//    {
//      @Override
//      public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer)
//      {
//        try
//        {
//          ITraceTransaction trace = traceFactory_.createTransaction(null, null);
//          
//          ReceivedMessageBusMsg m = new Deserializer().deserialize( message.getData().toByteArray());
//          
//          byte[] rawMessage = cryptor_.decrypt(m.getPayload(), secretKey_);
//          
//          eventForwarder_.consume(trace, rawMessage);
//          
//          consumer.ack();
//          
//          trace.finished();
//        }
//        catch (IOException | MessageBusCryptoException e)
//        {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//        }
//      }
//    };
//
//    Subscriber subscriber = Subscriber.newBuilder(subscriptionName, receiver).build();
//    
//    subscriber.addListener(new Subscriber.Listener()
//    {
//      @Override
//      public void failed(Subscriber.State from, Throwable failure)
//      {
//        log_.error("Error for " + subscriptionName + " from " + from, failure);
//      }
//    }, MoreExecutors.directExecutor());
//    
//    subscriberList_.add(subscriber);
//    subscriber.startAsync();
//    log_.info("Subscribing to " + subscriptionName + "...");
//  }
//
//  private void stopPubSub()
//  {
//    for(Subscriber subscriber : subscriberList_)
//    {
//      subscriber.stopAsync();
//    }
//  }
}
