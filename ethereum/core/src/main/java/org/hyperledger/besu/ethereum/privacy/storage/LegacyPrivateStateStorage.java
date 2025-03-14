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

import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.evm.log.Log;

import java.util.List;
import java.util.Optional;

import org.apache.tuweni.bytes.v2.Bytes;

/** This interface contains the methods used to access the private state until version 1.3 */
@Deprecated
public interface LegacyPrivateStateStorage {

  Optional<Hash> getLatestStateRoot(Bytes privacyId);

  Optional<List<Log>> getTransactionLogs(Bytes transactionHash);

  Optional<Bytes> getTransactionOutput(Bytes transactionHash);

  Optional<Bytes> getStatus(Bytes transactionHash);

  Optional<Bytes> getRevertReason(Bytes transactionHash);

  Optional<PrivateTransactionMetadata> getTransactionMetadata(
      Bytes blockHash, Bytes transactionHash);

  boolean isPrivateStateAvailable(Bytes transactionHash);

  boolean isWorldStateAvailable(Bytes rootHash);

  Updater updater();

  interface Updater {

    Updater putLatestStateRoot(Bytes privacyId, Hash privateStateHash);

    Updater putTransactionLogs(Bytes transactionHash, List<Log> logs);

    Updater putTransactionResult(Bytes transactionHash, Bytes events);

    Updater putTransactionStatus(Bytes transactionHash, Bytes status);

    Updater putTransactionRevertReason(Bytes txHash, Bytes bytesValue);

    Updater putTransactionMetadata(
        Bytes blockHash, Bytes transactionHash, PrivateTransactionMetadata metadata);

    void commit();

    void rollback();
  }
}
