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

/** The Dup operation. */
public class DupOperation extends AbstractOperation {

  /** The constant DUP_BASE. */
  private static final int DUP_BASE = 0x7F;
  /**
   * Instantiates a new Dup operation.
   *
   * @param index the index
   */
  public DupOperation(final int index) {
    super(
        0x80 + index - 1,
        "DUP" + index,
        index,
        index + 1,
      null);
  }

  @Override
  public Operation.OperationResult execute(
      final MessageFrame frame, final EVM evm) {
    final int index = frame.getCurrentOperation().getOpcode() - DUP_BASE;
    frame.pushStackItem(frame.getStackItem(index - 1));

    return new OperationResult();
  }
}
