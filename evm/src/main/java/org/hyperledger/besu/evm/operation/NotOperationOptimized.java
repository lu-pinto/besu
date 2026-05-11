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
import org.hyperledger.besu.evm.UInt256;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes;

/** The Not operation. */
public class NotOperationOptimized extends AbstractOperation {

  /**
   * Instantiates a new Not operation.
   *
   */
  public NotOperationOptimized() {
    super(0x19, "NOT", 1, 1, null);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final Bytes value = frame.popStackItem();
    UInt256 uint256 = UInt256.fromBytesBE(value.toArrayUnsafe());

    final UInt256 result = uint256.not();
    byte[] resultArray = result.toBytesBE();
    frame.pushStackItem(Bytes.wrap(resultArray));
    return new OperationResult();
  }
}
