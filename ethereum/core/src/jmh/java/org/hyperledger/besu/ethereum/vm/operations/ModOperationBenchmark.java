/*
 * Copyright contributors to Besu.
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
package org.hyperledger.besu.ethereum.vm.operations;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import org.apache.tuweni.bytes.Bytes;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.ModOperation;
import org.hyperledger.besu.evm.operation.Operation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;

public class ModOperationBenchmark extends BinaryOperationBenchmark {

  private static final Random RANDOM = new Random();

  // public because of jmh code generator
  public enum MODE {
    ZERO_MOD,
    LARGE_MOD_NUM_SMALLER_THAN_DENOM,
    LARGE_MOD_NUM_BIGGER_THAN_DENOM,
    SMALL_MOD_NUM_SMALLER_THAN_DENOM,
    SMALL_MOD_NUM_BIGGER_THAN_DENOM,
    INT_OPERATORS,
    LONG_OPERATORS,
    FIXED_SIZE_OPERATORS_NUM_SMALLER_THAN_DENOM,
    FIXED_SIZE_OPERATORS_NUM_BIGGER_THAN_DENOM,
    FULL_RANDOM
  }

  @Param
  private MODE mode;

  @Setup
  @Override
  public void setUp() {
    frame = BenchmarkHelper.createMessageCallFrame();

    switch (mode) {
      case ZERO_MOD:
        bPool = new Bytes[SAMPLE_SIZE];
        BenchmarkHelper.fillPool(bPool, () -> 1 + RANDOM.nextInt(10));
        aPool = Arrays.stream(bPool).map(bytes -> {
          BigInteger bigInt = new BigInteger(1, bytes.toArrayUnsafe());
          bigInt = bigInt.multiply(BigInteger.valueOf(RANDOM.nextInt()));
          return Bytes.wrap(bigInt.toByteArray());
        }).toArray(Bytes[]::new);
        break;
      case LARGE_MOD_NUM_SMALLER_THAN_DENOM:
        fillPools(
          () -> 24 + RANDOM.nextInt(9),
          () -> 24 + RANDOM.nextInt(9),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> a.compareTo(b) > 0);
        break;
      case LARGE_MOD_NUM_BIGGER_THAN_DENOM:
        fillPools(
          () -> 24 + RANDOM.nextInt(9),
          () -> 24 + RANDOM.nextInt(9),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> b.compareTo(a) > 0);
        break;
      case SMALL_MOD_NUM_SMALLER_THAN_DENOM:
        fillPools(
          () -> 1 + RANDOM.nextInt(9),
          () -> 24 + RANDOM.nextInt(9),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> a.compareTo(b) > 0);
        break;
      case SMALL_MOD_NUM_BIGGER_THAN_DENOM:
        fillPools(
          () -> 1 + RANDOM.nextInt(9),
          () -> 24 + RANDOM.nextInt(9),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> b.compareTo(a) > 0);
        break;
      case INT_OPERATORS:
        fillPools(
          () -> 5 + RANDOM.nextInt(4),
          () -> 1 + RANDOM.nextInt(4),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> a.compareTo(b) > 0);
        break;
      case LONG_OPERATORS:
        fillPools(
          () -> 9 + RANDOM.nextInt(5),
          () -> 4 + RANDOM.nextInt(5),
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> a.compareTo(b) > 0);
        break;
      case FIXED_SIZE_OPERATORS_NUM_SMALLER_THAN_DENOM:
        fillPools(
          () -> 16,
          () -> 16,
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> b.compareTo(a) > 0);
        break;
      case FIXED_SIZE_OPERATORS_NUM_BIGGER_THAN_DENOM:
        fillPools(
          () -> 16,
          () -> 16,
          byteArray -> new BigInteger(1, byteArray),
          (a, b) -> a.compareTo(b) > 0);
        break;
      case FULL_RANDOM:
        fillPools(
          () -> 1 + RANDOM.nextInt(32),
          () -> 1 + RANDOM.nextInt(32),
          __ -> 0,
          (__, ___) -> true);
        break;
    }
    index = 0;
  }

  @Override
  protected Operation.OperationResult invoke(final MessageFrame frame) {
    return ModOperation.staticOperation(frame);
  }
}
