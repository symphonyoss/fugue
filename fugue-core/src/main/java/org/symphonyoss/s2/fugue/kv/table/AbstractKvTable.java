/*
 *
 *
 * Copyright 2019 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.kv.table;

import org.symphonyoss.s2.common.fault.FaultAccumulator;
import org.symphonyoss.s2.common.fluent.BaseAbstractBuilder;
import org.symphonyoss.s2.fugue.config.IConfiguration;
import org.symphonyoss.s2.fugue.naming.INameFactory;

/**
 * Base implementation of IKvTable.
 * 
 * @author Bruce Skingle
 *
 * @param <T> Concrete type for fluent methods.
 */
public abstract class AbstractKvTable<T extends AbstractKvTable<T>> implements IKvTable
{
  protected static final String  Separator = "#";

  protected final IConfiguration config_;
  protected final INameFactory   nameFactory_;
  
  /** The serviceId forms part of the partition key for all values in this table. */
  protected final String         serviceId_;

  protected AbstractKvTable(AbstractBuilder<?,?> builder)
  {
    config_       = builder.config_;
    nameFactory_  = builder.nameFactory_;
    serviceId_    = builder.serviceId_;
  }

  protected static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AbstractKvTable<B>> extends BaseAbstractBuilder<T,B>
  {
    protected IConfiguration config_;
    protected INameFactory   nameFactory_;
    protected String         serviceId_;
    
    protected AbstractBuilder(Class<T> type)
    {
      super(type);
    }
    
    @Override
    public void validate(FaultAccumulator faultAccumulator)
    {
      super.validate(faultAccumulator);
      
      faultAccumulator.checkNotNull(config_,      "config");
      faultAccumulator.checkNotNull(nameFactory_, "nameFactory");
      faultAccumulator.checkNotNull(serviceId_,   "serviceId");
    }

    public T withConfig(IConfiguration config)
    {
      config_ = config;
      
      return self();
    }

    public T withNameFactory(INameFactory nameFactory)
    {
      nameFactory_ = nameFactory;
      
      return self();
    }

    /**
     * The serviceId forms part of the partition key for all values in this table.
     * 
     * @param serviceId The serviceId for this table.
     * 
     * @return This (fluent method).
     */
    public T withServiceId(String serviceId)
    {
      serviceId_ = serviceId;
      
      return self();
    }
  }
}
