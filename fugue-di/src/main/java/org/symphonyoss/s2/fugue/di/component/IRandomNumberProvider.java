/*
 *
 *
 * Copyright 2017 Symphony Communication Services, LLC.
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

package org.symphonyoss.s2.fugue.di.component;

import javax.annotation.Nonnull;

import org.symphonyoss.s2.fugue.di.IComponent;

/**
 * A source of random data.
 * 
 * @author Bruce Skingle
 *
 */
public interface IRandomNumberProvider extends IComponent
{
  /**
   * Returns a pseudorandom, uniformly distributed {@code int}
   * value. The general
   * contract of {@code nextInt} is that one {@code int} value is
   * pseudorandomly generated and returned. All 2<sup>32</sup> possible
   * {@code int} values are produced with (approximately) equal probability.
   *
   * @return a pseudorandom, uniformly distributed {@code int}
   *         value
   */
  int nextInt();

  /**
   * Returns a pseudorandom, uniformly distributed {@code int} value
   * between 0 (inclusive) and the specified value (exclusive).
   *
   * @param bound the upper bound (exclusive).  Must be positive.
   * @return a pseudorandom, uniformly distributed {@code int}
   *         value between zero (inclusive) and {@code bound} (exclusive)
   * @throws IllegalArgumentException if bound is not positive
   */
  int nextInt(int bound);

  /**
   * Returns a pseudorandom, uniformly distributed {@code long}
   * value. The general
   * contract of {@code nextInt} is that one {@code long} value is
   * pseudorandomly generated and returned. All 2<sup>64</sup> possible
   * {@code long} values are produced with (approximately) equal probability.
   *
   * @return a pseudorandom, uniformly distributed {@code int}
   *         value
   */
  long nextLong();

  /**
   * Returns a pseudorandom, uniformly distributed {@code boolean}
   * value.
   *
   * @return a pseudorandom, uniformly distributed {@code boolean}
   *         value
   */
  boolean nextBoolean();

  /**
   * Generates random bytes and places them into a user-supplied
   * byte array.  The number of random bytes produced is equal to
   * the length of the byte array.
   *
   * @param  bytes the byte array to fill with random bytes
   * @throws NullPointerException if the byte array is null
   */
  void nextBytes(@Nonnull byte[] bytes);

  /**
   * Returns a pseudorandom, uniformly distributed {@code float}
   * value between {@code 0.0} and {@code 1.0}.
   *
   * @return a pseudorandom, uniformly distributed {@code float}
   *         value between {@code 0.0} and {@code 1.0}
   */
  float nextFloat();

  /**
   * Returns a pseudorandom, uniformly distributed {@code double}
   * value between {@code 0.0} and {@code 1.0}.
   *
   * @return a pseudorandom, uniformly distributed {@code double}
   *         value between {@code 0.0} and {@code 1.0}
   */
  double nextDouble();

  /**
   * Returns a pseudorandom, Gaussian ("normally") distributed
   * {@code double} value with mean {@code 0.0} and standard
   * deviation {@code 1.0}.
   *
   * @return a pseudorandom, Gaussian ("normally") distributed
   *         {@code double} value with mean {@code 0.0} and
   *         standard deviation {@code 1.0}
   */
  double nextGaussian();

}