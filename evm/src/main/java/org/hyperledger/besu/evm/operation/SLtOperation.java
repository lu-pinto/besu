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

import org.hyperledger.besu.datatypes.Bytes32Helper;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import java.math.BigInteger;

import org.apache.tuweni.bytes.Bytes32;

/** The SLT operation. */
public class SLtOperation extends AbstractFixedCostOperation {

  /** The Slt operation success result. */
  static final OperationResult sltSuccess = new OperationResult(3, null);

  /**
   * Instantiates a new SLt operation.
   *
   * @param gasCalculator the gas calculator
   */
  public SLtOperation(final GasCalculator gasCalculator) {
    super(0x12, "SLT", 2, 1, gasCalculator, gasCalculator.getVeryLowTierGasCost());
  }

  @Override
  public Operation.OperationResult executeFixedCostOperation(
      final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs SLt operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    final Bytes32 value0 = frame.popStackItem();
    final Bytes32 value1 = frame.popStackItem();

    final BigInteger b0 = new BigInteger(value0.toArrayUnsafe());
    final BigInteger b1 = new BigInteger(value1.toArrayUnsafe());

    final Bytes32 result = b0.compareTo(b1) < 0 ? BYTES_ONE : Bytes32Helper.ZERO_BYTES32;

    frame.pushStackItem(result);

    return sltSuccess;
  }
}
