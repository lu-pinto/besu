/*
 * Copyright contributors to Hyperledger Besu.
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
package org.hyperledger.besu.evm.operation;

import static org.hyperledger.besu.evm.internal.Words.clampedToLong;

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes;

/** The Call data copy operation. */
public class CallDataCopyOperation extends AbstractOperation {

  /**
   * Instantiates a new Call data copy operation.
   *
   * @param gasCalculator the gas calculator
   */
  public CallDataCopyOperation(final GasCalculator gasCalculator) {
    super(0x37, "CALLDATACOPY", 3, 0, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long memOffset = clampedToLong(frame.popStackItem());
    final long sourceOffset = clampedToLong(frame.popStackItem());
    final long numBytes = clampedToLong(frame.popStackItem());

    final long cost = gasCalculator().dataCopyOperationGasCost(frame, memOffset, numBytes);
    if (frame.getRemainingGas() < cost) {
      return OperationResult.insufficientGas(cost);
    }

    final Bytes callData = frame.getInputData();

    frame.writeMemory(memOffset, sourceOffset, numBytes, callData, true);

    return new OperationResult(cost);
  }
}
