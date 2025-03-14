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
package org.hyperledger.besu.ethereum.eth.sync.snapsync;

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.ethereum.core.TrieGenerator;
import org.hyperledger.besu.ethereum.proof.WorldStateProofProvider;
import org.hyperledger.besu.ethereum.trie.MerkleTrie;
import org.hyperledger.besu.ethereum.trie.RangeManager;
import org.hyperledger.besu.ethereum.trie.RangeStorageEntriesCollector;
import org.hyperledger.besu.ethereum.trie.TrieIterator;
import org.hyperledger.besu.ethereum.trie.forest.storage.ForestWorldStateKeyValueStorage;
import org.hyperledger.besu.ethereum.trie.patricia.StoredMerklePatriciaTrie;
import org.hyperledger.besu.ethereum.worldstate.WorldStateStorageCoordinator;
import org.hyperledger.besu.services.kvstore.InMemoryKeyValueStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.tuweni.bytes.v2.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class StackTrieTest {

  final Bytes lastAccount = RangeManager.MIN_RANGE;

  @Test
  public void shouldNotSaveTheRootWhenIncomplete() {

    final int nbAccounts = 15;

    final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    final ForestWorldStateKeyValueStorage recreatedWorldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());

    final MerkleTrie<Bytes, Bytes> accountStateTrie =
        TrieGenerator.generateTrie(worldStateStorageCoordinator, nbAccounts);

    final StackTrie stackTrie =
        new StackTrie(Hash.wrap(accountStateTrie.getRootHash()), 0, 256, lastAccount);
    stackTrie.addSegment();

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(
            lastAccount, RangeManager.MAX_RANGE, 5, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes, Bytes> accounts =
        (TreeMap<Bytes, Bytes>)
            accountStateTrie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, lastAccount));

    final WorldStateProofProvider worldStateProofProvider =
        new WorldStateProofProvider(worldStateStorageCoordinator);

    // generate the proof
    final List<Bytes> proofs =
        worldStateProofProvider.getAccountProofRelatedNodes(
            Hash.wrap(accountStateTrie.getRootHash()), lastAccount);
    proofs.addAll(
        worldStateProofProvider.getAccountProofRelatedNodes(
            Hash.wrap(accountStateTrie.getRootHash()), accounts.lastKey()));

    stackTrie.addElement(Bytes.random(32), proofs, accounts);

    final ForestWorldStateKeyValueStorage.Updater updater = recreatedWorldStateStorage.updater();
    stackTrie.commit(((location, hash, value) -> updater.putAccountStateTrieNode(hash, value)));
    updater.commit();

    Assertions.assertThat(
            recreatedWorldStateStorage.getAccountStateTrieNode(accountStateTrie.getRootHash()))
        .isEmpty();
  }

  @Test
  public void shouldSaveTheRootWhenComplete() {

    final int nbAccounts = 15;

    final ForestWorldStateKeyValueStorage worldStateKeyValueStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateKeyValueStorage);

    final ForestWorldStateKeyValueStorage recreatedWorldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());

    final MerkleTrie<Bytes, Bytes> accountStateTrie =
        TrieGenerator.generateTrie(worldStateStorageCoordinator, nbAccounts);

    final StackTrie stackTrie =
        new StackTrie(Hash.wrap(accountStateTrie.getRootHash()), 0, 256, lastAccount);

    for (int i = 0; i < nbAccounts; i += 5) {
      stackTrie.addSegment();
      final RangeStorageEntriesCollector collector =
          RangeStorageEntriesCollector.createCollector(
              lastAccount, RangeManager.MAX_RANGE, 5, Integer.MAX_VALUE);
      final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
      final TreeMap<Bytes, Bytes> accounts =
          (TreeMap<Bytes, Bytes>)
              accountStateTrie.entriesFrom(
                  root ->
                      RangeStorageEntriesCollector.collectEntries(
                          collector, visitor, root, lastAccount));

      final WorldStateProofProvider worldStateProofProvider =
          new WorldStateProofProvider(worldStateStorageCoordinator);

      // generate the proof
      final List<Bytes> proofs =
          worldStateProofProvider.getAccountProofRelatedNodes(
              Hash.wrap(accountStateTrie.getRootHash()), lastAccount);
      proofs.addAll(
          worldStateProofProvider.getAccountProofRelatedNodes(
              Hash.wrap(accountStateTrie.getRootHash()), accounts.lastKey()));

      stackTrie.addElement(Bytes.random(32), proofs, accounts);

      final ForestWorldStateKeyValueStorage.Updater updater = recreatedWorldStateStorage.updater();
      stackTrie.commit((location, hash, value) -> updater.putAccountStateTrieNode(hash, value));
      updater.commit();
    }

    Assertions.assertThat(
            worldStateKeyValueStorage.getAccountStateTrieNode(accountStateTrie.getRootHash()))
        .isPresent();
  }

  @Test
  public void shouldNotSaveNodeWithChildNotInTheRange() {
    final ForestWorldStateKeyValueStorage worldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final WorldStateStorageCoordinator worldStateStorageCoordinator =
        new WorldStateStorageCoordinator(worldStateStorage);

    final MerkleTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            (location, hash) -> worldStateStorage.getAccountStateTrieNode(hash).map(Bytes::wrap),
            b -> b,
            b -> b);

    trie.put(Bytes.of(0x10).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x11).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x20).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x21).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x01).mutableCopy().rightPad(32), Bytes.of(0x02));
    trie.put(Bytes.of(0x02).mutableCopy().rightPad(32), Bytes.of(0x03));
    trie.put(Bytes.of(0x03).mutableCopy().rightPad(32), Bytes.of(0x04));

    final ForestWorldStateKeyValueStorage.Updater updater = worldStateStorage.updater();
    trie.commit((location, hash, value) -> updater.putAccountStateTrieNode(hash, value));
    updater.commit();

    final Bytes startRange = Bytes.of(0x02).mutableCopy().rightPad(32);

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(
            startRange, RangeManager.MAX_RANGE, 15, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes, Bytes> entries =
        (TreeMap<Bytes, Bytes>)
            trie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, startRange));

    final WorldStateProofProvider worldStateProofProvider =
        new WorldStateProofProvider(worldStateStorageCoordinator);

    // generate the proof
    final List<Bytes> proofs =
        worldStateProofProvider.getAccountProofRelatedNodes(
            Hash.wrap(trie.getRootHash()), startRange);
    proofs.addAll(
        worldStateProofProvider.getAccountProofRelatedNodes(
            Hash.wrap(trie.getRootHash()), entries.lastKey()));

    // try to commit with stack trie
    final ForestWorldStateKeyValueStorage recreatedWorldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final StackTrie stackTrie = new StackTrie(Hash.wrap(trie.getRootHash()), 0, 256, startRange);
    stackTrie.addSegment();
    stackTrie.addElement(Bytes.random(32), proofs, entries);
    final ForestWorldStateKeyValueStorage.Updater updaterStackTrie =
        recreatedWorldStateStorage.updater();
    stackTrie.commit(
        (location, hash, value) -> updaterStackTrie.putAccountStateTrieNode(hash, value));
    updaterStackTrie.commit();

    // verify the state of the db
    Assertions.assertThat(worldStateStorage.getAccountStateTrieNode(trie.getRootHash()))
        .isPresent();
    Assertions.assertThat(recreatedWorldStateStorage.getAccountStateTrieNode(trie.getRootHash()))
        .isNotPresent();
  }

  @Test
  public void shouldSaveNodeWithAllChildsInTheRange() {
    final ForestWorldStateKeyValueStorage worldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());

    final MerkleTrie<Bytes, Bytes> trie =
        new StoredMerklePatriciaTrie<>(
            (location, hash) -> worldStateStorage.getAccountStateTrieNode(hash).map(Bytes::wrap),
            b -> b,
            b -> b);

    trie.put(Bytes.of(0x10).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x11).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x20).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x21).mutableCopy().rightPad(32), Bytes.of(0x01));
    trie.put(Bytes.of(0x01).mutableCopy().rightPad(32), Bytes.of(0x02));
    trie.put(Bytes.of(0x02).mutableCopy().rightPad(32), Bytes.of(0x03));
    trie.put(Bytes.of(0x03).mutableCopy().rightPad(32), Bytes.of(0x04));

    final ForestWorldStateKeyValueStorage.Updater updater = worldStateStorage.updater();
    trie.commit((location, hash, value) -> updater.putAccountStateTrieNode(hash, value));
    updater.commit();

    final Bytes startRange = Bytes.of(0x00).mutableCopy().rightPad(32);

    final RangeStorageEntriesCollector collector =
        RangeStorageEntriesCollector.createCollector(
            startRange, RangeManager.MAX_RANGE, 15, Integer.MAX_VALUE);
    final TrieIterator<Bytes> visitor = RangeStorageEntriesCollector.createVisitor(collector);
    final TreeMap<Bytes, Bytes> entries =
        (TreeMap<Bytes, Bytes>)
            trie.entriesFrom(
                root ->
                    RangeStorageEntriesCollector.collectEntries(
                        collector, visitor, root, startRange));

    // try to commit with stack trie
    final ForestWorldStateKeyValueStorage recreatedWorldStateStorage =
        new ForestWorldStateKeyValueStorage(new InMemoryKeyValueStorage());
    final StackTrie stackTrie = new StackTrie(Hash.wrap(trie.getRootHash()), 0, 256, startRange);
    stackTrie.addSegment();
    stackTrie.addElement(Bytes.random(32), new ArrayList<>(), entries);
    final ForestWorldStateKeyValueStorage.Updater updaterStackTrie =
        recreatedWorldStateStorage.updater();
    stackTrie.commit(
        (location, hash, value) -> updaterStackTrie.putAccountStateTrieNode(hash, value));
    updaterStackTrie.commit();

    // verify the state of the db
    Assertions.assertThat(worldStateStorage.getAccountStateTrieNode(trie.getRootHash()))
        .isPresent();
    Assertions.assertThat(recreatedWorldStateStorage.getAccountStateTrieNode(trie.getRootHash()))
        .isPresent();
  }
}
