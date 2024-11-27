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

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;

/** Implements the TLOAD operation defined in EIP-1153 */
public class TLoadOperation extends AbstractOperation {

  /**
   * TLoad operation
   *
   * @param gasCalculator gas calculator for costing
   */
  public TLoadOperation(final GasCalculator gasCalculator) {
    super(0x5C, "TLOAD", 1, 1, gasCalculator);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final long cost = gasCalculator().getTransientLoadOperationGasCost();
    final Bytes32 slot = UInt256.fromBytes(frame.popStackItem());
    if (frame.getRemainingGas() < cost) {
      return OperationResult.insufficientGas(cost);
    } else {
      frame.pushStackItem(frame.getTransientStorageValue(frame.getRecipientAddress(), slot));

      return new OperationResult(cost);
    }
  }
}
