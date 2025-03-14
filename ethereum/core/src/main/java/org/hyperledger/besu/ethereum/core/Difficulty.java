/*
 * Copyright ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.ethereum.core;

import org.hyperledger.besu.datatypes.Quantity;

import java.math.BigInteger;

import org.apache.tuweni.bytes.v2.Bytes;
import org.apache.tuweni.bytes.v2.Bytes32;
import org.apache.tuweni.bytes.v2.DelegatingBytes;
import org.apache.tuweni.units.bigints.UInt256;

public final class Difficulty extends DelegatingBytes implements Quantity {

  public static final Difficulty ZERO = of(0);

  public static final Difficulty ONE = of(1);

  public static final Difficulty MAX_VALUE = wrap(Bytes32.ZERO.mutableCopy().not());

  Difficulty(final UInt256 value) {
    super(value, 32);
  }

  private Difficulty(final long v) {
    this(UInt256.valueOf(v));
  }

  private Difficulty(final BigInteger v) {
    this(UInt256.valueOf(v));
  }

  private Difficulty(final String hexString) {
    this(UInt256.fromHexString(hexString));
  }

  public static Difficulty of(final long value) {
    return new Difficulty(value);
  }

  public static Difficulty of(final BigInteger value) {
    return new Difficulty(value);
  }

  public static Difficulty of(final UInt256 value) {
    return new Difficulty(value);
  }

  public static Difficulty wrap(final Bytes value) {
    return new Difficulty(UInt256.fromBytes(value));
  }

  public static Difficulty fromHexString(final String str) {
    return new Difficulty(str);
  }

  public static Difficulty fromHexOrDecimalString(final String str) {
    return str.startsWith("0x") ? new Difficulty(str) : new Difficulty(new BigInteger(str));
  }

  @Override
  public Number getValue() {
    return getAsBigInteger();
  }

  @Override
  public BigInteger getAsBigInteger() {
    return toBigInteger();
  }

  @Override
  public String toHexString() {
    return super.toHexString();
  }

  @Override
  public String toShortHexString() {
    return super.isZero() ? "0x0" : super.toShortHexString();
  }

  public Difficulty add(final Difficulty value) {
    return new Difficulty(
        UInt256.fromBytes(this.getImpl()).add(UInt256.fromBytes(value.getImpl())));
  }

  public boolean greaterOrEqualThan(final Difficulty value) {
    return UInt256.fromBytes(this.getImpl()).greaterOrEqualThan(UInt256.fromBytes(value));
  }

  public boolean lessThan(final Difficulty value) {
    return UInt256.fromBytes(this.getImpl()).lessThan(UInt256.fromBytes(value.getImpl()));
  }

  public Difficulty subtract(final Difficulty value) {
    return new Difficulty(UInt256.fromBytes(this.getImpl()).subtract(UInt256.fromBytes(value)));
  }

  public boolean greaterThan(final Difficulty value) {
    return UInt256.fromBytes(this.getImpl()).greaterThan(UInt256.fromBytes(value.getImpl()));
  }

  public boolean lessOrEqualThan(final Difficulty value) {
    return UInt256.fromBytes(value.getImpl()).lessOrEqualThan(UInt256.fromBytes(value));
  }

  public Bytes toMinimalBytes() {
    return UInt256.fromBytes(this.getImpl()).toMinimalBytes();
  }
}
