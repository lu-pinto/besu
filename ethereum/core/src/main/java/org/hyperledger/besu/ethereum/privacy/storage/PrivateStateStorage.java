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
package org.hyperledger.besu.ethereum.privacy.storage;

import org.hyperledger.besu.ethereum.privacy.PrivateTransactionReceipt;

import java.util.Optional;

import org.apache.tuweni.bytes.v2.Bytes;

public interface PrivateStateStorage {

  Optional<PrivateTransactionReceipt> getTransactionReceipt(Bytes blockHash, Bytes txHash);

  Optional<PrivateBlockMetadata> getPrivateBlockMetadata(Bytes blockHash, Bytes privacyGroupId);

  Optional<PrivacyGroupHeadBlockMap> getPrivacyGroupHeadBlockMap(Bytes blockHash);

  int getSchemaVersion();

  Optional<Bytes> getAddDataKey(Bytes privacyGroupId);

  boolean isEmpty();

  Updater updater();

  interface Updater {

    Updater putTransactionReceipt(
        Bytes blockHash, Bytes transactionHash, PrivateTransactionReceipt receipt);

    Updater putPrivateBlockMetadata(
        Bytes blockHash, Bytes privacyGroupId, PrivateBlockMetadata metadata);

    Updater putPrivacyGroupHeadBlockMap(Bytes blockHash, PrivacyGroupHeadBlockMap map);

    Updater putDatabaseVersion(int version);

    Updater putAddDataKey(Bytes privacyGroupId, Bytes addDataKey);

    void commit();

    void rollback();

    void remove(final Bytes key, final Bytes keySuffix);
  }
}
