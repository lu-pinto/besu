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
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.v2.Bytes;
import org.apache.tuweni.bytes.v2.Bytes32;
import org.apache.tuweni.bytes.v2.MutableBytes;

/** The Sign extend operation. */
public class SignExtendOperation extends AbstractFixedCostOperation {

  private static final OperationResult signExtendSuccess = new OperationResult(5, null);

  /**
   * Instantiates a new Sign extend operation.
   *
   * @param gasCalculator the gas calculator
   */
  public SignExtendOperation(final GasCalculator gasCalculator) {
    super(0x0B, "SIGNEXTEND", 2, 1, gasCalculator, gasCalculator.getLowTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs Sign Extend operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    final Bytes value0 = frame.popStackItem().trimLeadingZeros();
    final Bytes stackItem = frame.popStackItem();
    final Bytes value1 =
        stackItem.size() >= 32 ? stackItem : frame.popStackItem().mutableCopy().leftPad(32);

    // Any value >= 31 imply an index <= 0, so no work to do (note that 0 itself is a valid index,
    // but copying the 0th byte to itself is only so useful).
    int value0size = value0.size();
    if (value0size > 1) {
      frame.pushStackItem(value1);
      return signExtendSuccess;
    }

    int value0Value = value0.toInt();
    if (value0Value >= 31) {
      frame.pushStackItem(value1);
      return signExtendSuccess;
    }

    final int byteIndex = 31 - value0Value;
    final MutableBytes result =
        value1.get(byteIndex) < 0 ? Bytes32.ZERO.mutableCopy().not() : Bytes32.ZERO.mutableCopy();
    result.set(byteIndex, value1.slice(byteIndex));
    frame.pushStackItem(result);

    return signExtendSuccess;
  }
}
