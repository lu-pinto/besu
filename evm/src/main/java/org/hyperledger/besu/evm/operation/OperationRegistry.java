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

/** Encapsulates a group of {@link Operation}s used together. */
public class OperationRegistry {

  public static final int NUM_OPERATIONS = 256;

  private final Operation[] operations;
  private final int[] staticGas;
  private final int[] pcIncrements;

  /** Instantiates a new Operation registry. */
  public OperationRegistry() {
    this.operations = new Operation[NUM_OPERATIONS];
    this.staticGas = new int[NUM_OPERATIONS];
    this.pcIncrements = new int[NUM_OPERATIONS];
  }

  /**
   * Get operation.
   *
   * @param opcode the opcode
   * @return the operation
   */
  public Operation getOperation(final int opcode) {
    return operations[opcode];
  }

  /**
   * Put.
   *
   * @param operation the operation
   */
  public void put(final Operation operation, final int staticGas, final int pcIncrement) {
    operations[operation.getOpcode()] = operation;
    this.staticGas[operation.getOpcode()] = staticGas;
    pcIncrements[operation.getOpcode()] = pcIncrement;
  }

  public int getStaticGas(final int opcode) {
    return staticGas[opcode];
  }

  public int getPcIncrement(final int opcode) {
    return pcIncrements[opcode];
  }

}
