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
package org.hyperledger.besu.evm.internal;

import java.util.Arrays;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes;

/**
 * An operand stack for the Ethereum Virtual machine (EVM). The stack grows 32 entries at a time if
 * it expands past the top of the allocated stack, up to maxSize.
 *
 * <p>The operand stack is responsible for storing the current operands that the EVM can execute. It
 * is assumed to have a fixed maximum size but may have a smaller memory footprint.
 */
public class OperandStack {
  private final Bytes[] entries;
  private int top;

  /**
   * Instantiates a new Flex stack.
   */
  public OperandStack() {
    this.entries = new Bytes[1024];
    this.top = -1;
  }

  /**
   * Get operand.
   *
   * @param offset the offset
   * @return the operand
   */
  public Bytes get(final int offset) {
    return entries[top - offset];
  }

  /**
   * Pop operand.
   *
   * @return the operand
   */
  public Bytes pop() {
    return entries[top--];
  }

  /**
   * Peek and return type T.
   *
   * @return the T entry
   */
  public Bytes peek() {
    return entries[top];
  }

  /**
   * Pops the specified number of operands from the stack.
   *
   * @param items the number of operands to pop off the stack
   */
  public void bulkPop(final int items) {
    int prevTop = top;
    top -= items;
    Arrays.fill(entries, top + 1, prevTop + 1, null);
  }

  /**
   * Trims the "middle" section of items out of the stack. Items below the cutpoint remains, and of
   * the items above only the itemsToKeep items remain. All items in the middle are removed.
   *
   * @param cutPoint Point at which to start removing items
   * @param itemsToKeep itemsToKeep Number of items on top to place at the cutPoint
   * @throws IllegalArgumentException if the cutPoint or items to keep is negative.
   * @throws UnderflowException If there are less than itemsToKeep above the cutPoint
   */
  public void preserveTop(final int cutPoint, final int itemsToKeep) {
    if (itemsToKeep == 0) {
      if (cutPoint < size()) {
        bulkPop(top - cutPoint);
      }
    } else {
      int targetSize = cutPoint + itemsToKeep;
      int currentSize = size();
      top = targetSize - 1;
      System.arraycopy(entries, currentSize - itemsToKeep, entries, cutPoint, itemsToKeep);
      Arrays.fill(entries, targetSize, currentSize, null);
    }
  }

  /**
   * Push operand.
   *
   * @param operand the operand
   */
  public void push(final Bytes operand) {
    entries[++top] = operand;
  }

  /**
   * Set operand.
   *
   * @param offset the offset
   * @param operand the operand
   */
  public void set(final int offset, final Bytes operand) {
    entries[top - offset] = operand;
  }

  /**
   * Size of entries.
   *
   * @return the size
   */
  public int size() {
    return top + 1;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < top; ++i) {
      builder.append(String.format("%n0x%04X ", i)).append(entries[i]);
    }
    return builder.toString();
  }

  @Override
  public int hashCode() {
    int result = 1;

    for (int i = 0; i <= top; i++) {
      result = 31 * result + (entries[i] == null ? 0 : entries[i].hashCode());
    }

    return result;
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof OperandStack)) {
      return false;
    }

    final OperandStack that = (OperandStack) other;
    if (this.top != that.top) {
      return false;
    }
    for (int i = 0; i <= top; i++) {
      if (!Objects.deepEquals(this.entries[i], that.entries[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Is stack full.
   *
   * @return the boolean
   */
  public boolean isFull() {
    return top + 1 >= 1024;
  }

  /**
   * Is stack empty.
   *
   * @return the boolean
   */
  public boolean isEmpty() {
    return top < 0;
  }
}
