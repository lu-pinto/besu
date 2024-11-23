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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

/** The Eq operation. */
public class EqOperation extends AbstractFixedCostOperation {

  /** The Eq operation success result. */
  static final OperationResult eqSuccess = new OperationResult(3);

  /**
   * Instantiates a new Eq operation.
   *
   * @param gasCalculator the gas calculator
   */
  public EqOperation(final GasCalculator gasCalculator) {
    super(0x14, "EQ", 2, 1, gasCalculator, gasCalculator.getVeryLowTierGasCost());
  }

  @Override
  public OperationResult executeFixedCostOperation(final MessageFrame frame, final EVM evm) {
    return staticOperation(frame);
  }

  /**
   * Performs Eq operation.
   *
   * @param frame the frame
   * @return the operation result
   */
  public static OperationResult staticOperation(final MessageFrame frame) {
    final Bytes value0 = frame.popStackItem().trimLeadingZeros();
    final Bytes value1 = frame.popStackItem().trimLeadingZeros();

    final Bytes result = (value0.equals(value1) ? UInt256.ONE : UInt256.ZERO);

    frame.pushStackItem(result);

    return eqSuccess;
  }
}
