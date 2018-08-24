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

package org.symphonyoss.s2.fugue.naming;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

public class Base62
{

  private static final BigInteger SIXTY_TWO = BigInteger.valueOf(62L);
  private static final byte[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();

  public static String encodeToString(byte[] bytes)
  {
    byte[]      byteBuf = new byte[bytes.length + 1];
    
    byteBuf[0] = 1;
    
    for(int i=0 ; i<bytes.length ; i++)
      byteBuf[i+1] = bytes[i];
    
    BigInteger bigInt = new BigInteger(1, byteBuf);
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    
    while(bigInt.compareTo(BigInteger.ZERO) > 0)
    {
      int idx = bigInt.mod(SIXTY_TWO).intValue();
      
      bos.write(ALPHABET[idx]);
      bigInt = bigInt.divide(SIXTY_TWO);
    }
    
    byte[] resultBytes = bos.toByteArray();
    StringBuilder  s = new StringBuilder();
    
    for(int i=resultBytes.length-1 ; i>=0 ; i--)
      s.append((char)resultBytes[i]);
    
    return s.toString();
  }

}
