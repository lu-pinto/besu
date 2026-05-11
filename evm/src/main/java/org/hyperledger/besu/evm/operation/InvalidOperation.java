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
import org.hyperledger.besu.evm.frame.ExceptionalHaltReason;
import org.hyperledger.besu.evm.frame.MessageFrame;

/** The Invalid operation. */
public class InvalidOperation extends AbstractOperation {

  /** The constant OPCODE. */
  public static final int OPCODE = 0xFE;

  /**
   * Instantiates a new Invalid operation.
   */
  public InvalidOperation() {
    this(OPCODE);
  }

  /**
   * Instantiates a new Invalid operation.
   *
   * @param opcode the opcode
   */
  public InvalidOperation(final int opcode) {
    super(opcode, "INVALID", -1, -1, null);
  }

  @Override
  public OperationResult execute(final MessageFrame frame, final EVM evm) {
    return invalidOperationResult(getOpcode());
  }

  /**
   * Creates an {@link OperationResult} for an invalid opcode.
   *
   * @param opcode the invalid opcode encountered
   * @return an {@link OperationResult} with zero gas cost and a description of the invalid opcode
   */
  public static OperationResult invalidOperationResult(final int opcode) {
    return new OperationResult(0, ExceptionalHaltReason.newInvalidOperation(opcode));
  }
}
