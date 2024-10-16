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
package org.hyperledger.besu.evm.gascalculator;

import static org.hyperledger.besu.datatypes.Address.KZG_POINT_EVAL;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.BASIC_DATA_LEAF_KEY;
import static org.hyperledger.besu.ethereum.trie.verkle.util.Parameters.CODE_HASH_LEAF_KEY;
import static org.hyperledger.besu.evm.internal.Words.clampedAdd;

import org.hyperledger.besu.datatypes.AccessWitness;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.stateless.Eip4762AccessWitness;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.tuweni.units.bigints.UInt256;

public class Eip4762GasCalculator extends PragueGasCalculator {
  public static final Address HISTORY_STORAGE_ADDRESS =
      Address.fromHexString("0xfffffffffffffffffffffffffffffffffffffffe");
  private static final long CREATE_OPERATION_GAS_COST = 1_000L;

  /** Instantiates a new Prague Gas Calculator. */
  public Eip4762GasCalculator() {
    super(KZG_POINT_EVAL.toArrayUnsafe()[19]);
  }

  @Override
  public long getColdSloadCost() {
    return 0; // no cold gas cost after verkle
  }

  @Override
  public long getColdAccountAccessCost() {
    return 0; // no cold gas cost after verkle
  }

  @Override
  public long initcodeCost(final int initCodeLength) {
    return super.initcodeCost(initCodeLength);
  }

  @Override
  public long initcodeStatelessCost(
      final MessageFrame frame, final Address address, final Wei value) {
    return frame.getAccessWitness().touchAndChargeContractCreateInit(address);
  }

  @Override
  public long callOperationGasCost(
      final MessageFrame frame,
      final long stipend,
      final long inputDataOffset,
      final long inputDataLength,
      final long outputDataOffset,
      final long outputDataLength,
      final Wei transferValue,
      final Account recipient,
      final Address to,
      final boolean accountIsWarm) {

    long gas =
        super.callOperationGasCost(
            frame,
            stipend,
            inputDataOffset,
            inputDataLength,
            outputDataOffset,
            outputDataLength,
            transferValue,
            recipient,
            to,
            false);

    if (!transferValue.isZero()) {
      gas =
          clampedAdd(
              gas,
              frame
                  .getAccessWitness()
                  .touchAndChargeValueTransfer(frame.getContractAddress(), to, recipient == null));
    }

    if (isPrecompile(to) || isSystemContract(to)) {
      return clampedAdd(gas, accountIsWarm ? getWarmStorageReadCost() : 0);
    }

    long messageCallGas = frame.getAccessWitness().touchAndChargeMessageCall(to);
    if (messageCallGas == 0) {
      messageCallGas = getWarmStorageReadCost();
    }
    gas = clampedAdd(gas, messageCallGas);

    return gas;
  }

  @Override
  public long callValueTransferGasCost() {
    return 0L;
  }

  @Override
  public long txCreateCost() {
    return CREATE_OPERATION_GAS_COST;
  }

  @Override
  public long codeDepositGasCost(final MessageFrame frame, final int codeSize) {
    // Check the remaining gas costs here, should be 0 until now
    return frame
        .getAccessWitness()
        .touchCodeChunksUponContractCreation(frame.getContractAddress(), codeSize);
  }

  @Override
  public long calculateStorageCost(
      final MessageFrame frame,
      final UInt256 key,
      final UInt256 newValue,
      final Supplier<UInt256> currentValue,
      final Supplier<UInt256> originalValue) {
    AccessWitness accessWitness = frame.getAccessWitness();
    List<UInt256> treeIndexes = accessWitness.getStorageSlotTreeIndexes(key);
    long gasCost =
        frame
            .getAccessWitness()
            .touchAddressOnWriteResetAndComputeGas(
                frame.getRecipientAddress(), treeIndexes.get(0), treeIndexes.get(1));

    if (gasCost == 0) {
      return SLOAD_GAS;
    }

    return gasCost;
  }

  @Override
  public long calculateStorageRefundAmount(
      final UInt256 newValue,
      final Supplier<UInt256> currentValue,
      final Supplier<UInt256> originalValue) {
    return 0L;
  }

  @Override
  public long extCodeCopyOperationGasCost(
      final MessageFrame frame,
      final Address address,
      final boolean accountIsWarm,
      final long memOffset,
      final long codeOffset,
      final long readSize,
      final long codeSize) {
    long gasCost = extCodeCopyOperationGasCost(frame, memOffset, readSize);

    if (isPrecompile(address) || isSystemContract(address)) {
      return clampedAdd(gasCost, getWarmStorageReadCost());
    }

    long statelessGas =
        frame
            .getAccessWitness()
            .touchAddressOnReadAndComputeGas(address, UInt256.ZERO, BASIC_DATA_LEAF_KEY);
    if (statelessGas == 0) {
      statelessGas = getWarmStorageReadCost();
    }

    if (!frame.wasCreatedInTransaction(frame.getContractAddress())) {
      long actualReadSize = Math.min(readSize, codeSize - codeOffset);
      statelessGas =
          clampedAdd(
              statelessGas,
              frame
                  .getAccessWitness()
                  .touchCodeChunks(address, codeOffset, actualReadSize, codeSize));
    }

    return clampedAdd(gasCost, statelessGas);
  }

  @Override
  public long codeCopyOperationGasCost(
      final MessageFrame frame,
      final long memOffset,
      final long codeOffset,
      final long readSize,
      final long codeSize) {
    long gasCost = super.dataCopyOperationGasCost(frame, memOffset, readSize);
    if (!frame.wasCreatedInTransaction(frame.getContractAddress())) {
      gasCost =
          clampedAdd(
              gasCost,
              frame
                  .getAccessWitness()
                  .touchCodeChunks(frame.getContractAddress(), codeOffset, readSize, codeSize));
    }
    return gasCost;
  }

  @Override
  public long pushOperationGasCost(
      final MessageFrame frame, final long codeOffset, final long readSize, final long codeSize) {
    long gasCost = super.pushOperationGasCost(frame, codeOffset, readSize, codeSize);
    if (!frame.wasCreatedInTransaction(frame.getContractAddress())) {
      gasCost =
          clampedAdd(
              gasCost,
              frame
                  .getAccessWitness()
                  .touchCodeChunks(frame.getContractAddress(), codeOffset, readSize, codeSize));
    }
    return gasCost;
  }

  @Override
  public long getBalanceOperationGasCost(
      final MessageFrame frame, final boolean accountIsWarm, final Optional<Address> maybeAddress) {
    if (maybeAddress.isEmpty()) {
      // can happen if there was a problem getting the address - not charged
      return 0L;
    }

    final Address address = maybeAddress.get();
    if (isPrecompile(address) || isSystemContract(address)) {
      return getWarmStorageReadCost();
    }

    final long statelessGas =
        frame
            .getAccessWitness()
            .touchAddressOnReadAndComputeGas(address, UInt256.ZERO, BASIC_DATA_LEAF_KEY);
    if (statelessGas == 0) {
      return getWarmStorageReadCost();
    }
    return statelessGas;
  }

  @Override
  public long extCodeHashOperationGasCost(
      final MessageFrame frame, final boolean accountIsWarm, final Optional<Address> maybeAddress) {
    if (maybeAddress.isEmpty()) {
      // can happen if there was a problem getting the address - not charged
      return 0;
    }

    final Address address = maybeAddress.get();
    if (isPrecompile(address) || isSystemContract(address)) {
      return getWarmStorageReadCost();
    }

    long statelessGas =
        frame
            .getAccessWitness()
            .touchAddressOnReadAndComputeGas(address, UInt256.ZERO, CODE_HASH_LEAF_KEY);
    if (statelessGas == 0) {
      return getWarmStorageReadCost();
    }
    return statelessGas;
  }

  @Override
  public long extCodeSizeOperationGasCost(
      final MessageFrame frame, final boolean accountIsWarm, final Optional<Address> maybeAddress) {
    if (maybeAddress.isEmpty()) {
      // can happen if there was a problem getting the address - not charged
      return 0L;
    }

    final Address address = maybeAddress.get();
    if (isPrecompile(address) || isSystemContract(address)) {
      return getWarmStorageReadCost();
    }

    long statelessGas =
        frame
            .getAccessWitness()
            .touchAddressOnReadAndComputeGas(address, UInt256.ZERO, BASIC_DATA_LEAF_KEY);
    if (statelessGas == 0) {
      return getWarmStorageReadCost();
    }
    return statelessGas;
  }

  @Override
  public long selfDestructOperationGasCost(
      final MessageFrame frame,
      final Account recipient,
      final Address recipientAddress,
      final Wei value,
      final Address originatorAddress) {
    long gasCost =
        super.selfDestructOperationGasCost(
            frame, recipient, recipientAddress, value, originatorAddress);

    // if there was any balance in originating account make the transfer
    if (!value.isZero()) {
      gasCost =
          clampedAdd(
              gasCost,
              frame
                  .getAccessWitness()
                  .touchAndChargeValueTransferSelfDestruct(
                      originatorAddress, recipientAddress, recipient == null));
    }

    // check if there's any balance in the originating account in search of value
    gasCost =
        clampedAdd(
            gasCost,
            frame
                .getAccessWitness()
                .touchAddressOnReadAndComputeGas(
                    originatorAddress, UInt256.ZERO, BASIC_DATA_LEAF_KEY));

    // TODO: REMOVE - if code removed below there's no point to check for this
    if (isPrecompile(recipientAddress) || isSystemContract(recipientAddress)) {
      return gasCost;
    }

    // TODO: Recheck only here to pass tests - after talking with Geth team this will be dropped in
    // future
    if (!recipientAddress.equals(originatorAddress)) {
      gasCost =
          clampedAdd(
              gasCost,
              frame
                  .getAccessWitness()
                  .touchAddressOnReadAndComputeGas(
                      recipientAddress, UInt256.ZERO, BASIC_DATA_LEAF_KEY));
    }

    return gasCost;
  }

  @Override
  public long getSloadOperationGasCost(
      final MessageFrame frame, final UInt256 key, final boolean slotIsWarm) {
    AccessWitness accessWitness = frame.getAccessWitness();
    List<UInt256> treeIndexes = accessWitness.getStorageSlotTreeIndexes(key);
    long gasCost =
        frame
            .getAccessWitness()
            .touchAddressOnReadAndComputeGas(
                frame.getContractAddress(), treeIndexes.get(0), treeIndexes.get(1));
    if (gasCost == 0) {
      return getWarmStorageReadCost();
    }
    return gasCost;
  }

  @Override
  public long completedCreateContractGasCost(final MessageFrame frame) {
    return frame
        .getAccessWitness()
        .touchAndChargeContractCreateCompleted(frame.getContractAddress());
  }

  private static boolean isSystemContract(final Address address) {
    return HISTORY_STORAGE_ADDRESS.equals(address);
  }

  @Override
  public AccessWitness newAccessWitness() {
    return new Eip4762AccessWitness();
  }
}
