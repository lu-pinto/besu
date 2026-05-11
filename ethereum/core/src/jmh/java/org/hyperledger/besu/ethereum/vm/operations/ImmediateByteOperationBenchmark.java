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

import org.hyperledger.besu.evm.Code;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.operation.Operation;

import java.util.concurrent.TimeUnit;

import org.apache.tuweni.bytes.Bytes;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Base benchmark class for EVM operations that use immediate byte operands.
 *
 * <p>This class provides common setup for operations like DUPN, SWAPN, and EXCHANGE (EIP-8024)
 * which read an immediate byte following the opcode to determine their behavior.
 */
@State(Scope.Thread)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(value = TimeUnit.NANOSECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
public abstract class ImmediateByteOperationBenchmark {

  protected static final int SAMPLE_SIZE = 30_000;
  protected static final int STACK_DEPTH = 50;

  protected Bytes[] valuePool;
  protected int index;
  protected MessageFrame frame;
  protected byte[] code;

  @Setup
  public void setUp() {
    code = new byte[] {(byte) getOpcode(), getImmediate()};
    frame = BenchmarkHelper.createMessageCallFrameWithCode(new Code(Bytes.wrap(code)));
    frame.setCurrentOperation(getOperation());
    frame.setPC(0);
    valuePool = new Bytes[SAMPLE_SIZE];
    BenchmarkHelper.fillPool(valuePool);
    index = 0;

    for (int i = 0; i < STACK_DEPTH; i++) {
      frame.pushStackItem(valuePool[i]);
    }
  }

  /**
   * Returns the operation instance used by this benchmark.
   *
   * @return the operation
   */
  protected abstract Operation getOperation();

  /**
   * Returns the opcode for this operation.
   *
   * @return the opcode value
   */
  protected abstract int getOpcode();

  /**
   * Returns the immediate byte operand for this operation.
   *
   * @return the immediate byte value
   */
  protected abstract byte getImmediate();

  /**
   * Invokes the operation. The frame is pre-configured with the test code, current operation, and
   * PC=0.
   *
   * @param frame the message frame
   * @return the operation result
   */
  protected abstract Operation.OperationResult invoke(MessageFrame frame);

  /**
   * Returns the stack size change after operation execution. Positive means items were added,
   * negative means items were removed.
   *
   * @return the stack delta
   */
  protected abstract int getStackDelta();

  @Benchmark
  public void executeOperation(final Blackhole blackhole) {
    frame.setPC(0);
    blackhole.consume(invoke(frame));

    int delta = getStackDelta();
    if (delta > 0) {
      for (int i = 0; i < delta; i++) {
        frame.popStackItem();
      }
    } else if (delta < 0) {
      for (int i = 0; i < -delta; i++) {
        frame.pushStackItem(valuePool[index]);
        index = (index + 1) % SAMPLE_SIZE;
      }
    }
  }
}
