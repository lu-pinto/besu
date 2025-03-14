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
package org.hyperledger.besu.ethereum.trie;

import java.util.Map;
import java.util.Optional;

import org.apache.tuweni.bytes.v2.Bytes;

public class RangeStorageEntriesCollector extends StorageEntriesCollector<Bytes> {

  private int currentSize = 0;
  private final Optional<Bytes> endKeyHash;
  private final Integer maxResponseBytes;

  public RangeStorageEntriesCollector(
      final Bytes startKeyHash,
      final Optional<Bytes> endKeyHash,
      final int limit,
      final int maxResponseBytes) {
    super(startKeyHash, limit);
    this.endKeyHash = endKeyHash;
    this.maxResponseBytes = maxResponseBytes;
  }

  public static RangeStorageEntriesCollector createCollector(
      final Bytes startKeyHash,
      final Bytes endKeyHash,
      final int limit,
      final int maxResponseBytes) {
    return new RangeStorageEntriesCollector(
        startKeyHash, Optional.ofNullable(endKeyHash), limit, maxResponseBytes);
  }

  public static RangeStorageEntriesCollector createCollector(
      final Bytes startKeyHash, final int limit, final int maxResponseBytes) {
    return new RangeStorageEntriesCollector(
        startKeyHash, Optional.empty(), limit, maxResponseBytes);
  }

  public static TrieIterator<Bytes> createVisitor(final RangeStorageEntriesCollector collector) {
    return new TrieIterator<>(collector, false);
  }

  public static Map<Bytes, Bytes> collectEntries(
      final RangeStorageEntriesCollector collector,
      final TrieIterator<Bytes> visitor,
      final Node<Bytes> root,
      final Bytes startKeyHash) {
    root.accept(visitor, CompactEncoding.bytesToPath(startKeyHash));
    return collector.getValues();
  }

  @Override
  public TrieIterator.State onLeaf(final Bytes keyHash, final Node<Bytes> node) {
    if (keyHash.compareTo(startKeyHash) >= 0) {
      if (node.getValue().isPresent()) {
        final Bytes value = node.getValue().get();
        currentSize += 32 + value.size();
        if (currentSize > maxResponseBytes) {
          return TrieIterator.State.STOP;
        }
        if (endKeyHash.isPresent() && keyHash.compareTo(endKeyHash.get()) > 0) {
          return TrieIterator.State.STOP;
        }

        values.put(keyHash, value);
      }
    }
    return limitReached() ? TrieIterator.State.STOP : TrieIterator.State.CONTINUE;
  }
}
