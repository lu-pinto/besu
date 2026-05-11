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
package org.hyperledger.besu.evm.operation;

import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.UInt256;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

/** The Div operation. */
public class DivOperationOptimized extends AbstractOperation {
  /**
   * Instantiates a new Div operation.
   *
   */
  public DivOperationOptimized() {
    super(0x04, "DIV", 2, 1, null);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    final Bytes value0 = frame.popStackItem();
    final Bytes value1 = frame.popStackItem();

    if (value1.isZero()) {
      frame.pushStackItem(Bytes32.ZERO);
    } else {
      UInt256 b0 = UInt256.fromBytesBE(value0.toArrayUnsafe());
      UInt256 b1 = UInt256.fromBytesBE(value1.toArrayUnsafe());
      frame.pushStackItem(Bytes.wrap(b0.div(b1).toBytesBE()));
    }
    return new OperationResult();
  }
}
