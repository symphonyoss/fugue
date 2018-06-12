/*
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package org.symphonyoss.s2.fugue.google.pubsub;

import org.symphonyoss.s2.common.immutable.ImmutableByteArray;
import org.symphonyoss.s2.fugue.core.trace.ITraceContextFactory;
import org.symphonyoss.s2.fugue.naming.INameFactory;
import org.symphonyoss.s2.fugue.pipeline.IThreadSafeConsumer;

public class GoogleSubscriberManager extends GoogleAbstractSubscriberManager<GoogleSubscriberManager>
{
  /**
   * Normal constructor.
   * 
   * @param nameFactory                     A NameFactory.
   * @param projectId                       The Google project ID for the pubsub service.
   * @param traceFactory                    A trace context factory.
   * @param unprocessableMessageConsumer    Consumer for invalid messages.
   */
  public GoogleSubscriberManager(INameFactory nameFactory, String projectId,
      ITraceContextFactory traceFactory,
      IThreadSafeConsumer<ImmutableByteArray> unprocessableMessageConsumer)
  {
    super(GoogleSubscriberManager.class, nameFactory, projectId, traceFactory, unprocessableMessageConsumer);
  }
}
