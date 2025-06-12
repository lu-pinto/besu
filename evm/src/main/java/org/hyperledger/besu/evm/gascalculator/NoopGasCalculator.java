package org.hyperledger.besu.evm.gascalculator;

import java.util.function.Supplier;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Transaction;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.account.Account;
import org.hyperledger.besu.evm.frame.MessageFrame;

public class NoopGasCalculator implements GasCalculator {
  @Override
  public long idPrecompiledContractGasCost(final Bytes input) {
    return 0;
  }

  @Override
  public long getEcrecPrecompiledContractGasCost() {
    return 0;
  }

  @Override
  public long sha256PrecompiledContractGasCost(final Bytes input) {
    return 0;
  }

  @Override
  public long ripemd160PrecompiledContractGasCost(final Bytes input) {
    return 0;
  }

  @Override
  public long getZeroTierGasCost() {
    return 0;
  }

  @Override
  public long getVeryLowTierGasCost() {
    return 0;
  }

  @Override
  public long getLowTierGasCost() {
    return 0;
  }

  @Override
  public long getBaseTierGasCost() {
    return 0;
  }

  @Override
  public long getMidTierGasCost() {
    return 0;
  }

  @Override
  public long getHighTierGasCost() {
    return 0;
  }

  @Override
  public long callOperationBaseGasCost() {
    return 0;
  }

  @Override
  public long callValueTransferGasCost() {
    return 0;
  }

  @Override
  public long newAccountGasCost() {
    return 0;
  }

  @Override
  public long callOperationGasCost(final MessageFrame frame, final long stipend, final long inputDataOffset, final long inputDataLength,
                                   final long outputDataOffset, final long outputDataLength, final Wei transferValue, final Account recipient,
                                   final Address contract, final boolean accountIsWarm) {
    return 0;
  }

  @Override
  public long getAdditionalCallStipend() {
    return 0;
  }

  @Override
  public long gasAvailableForChildCall(final MessageFrame frame, final long stipend, final boolean transfersValue) {
    return 0;
  }

  @Override
  public long getMinRetainedGas() {
    return 0;
  }

  @Override
  public long getMinCalleeGas() {
    return 0;
  }

  @SuppressWarnings("removal")
  @Override
  public long createOperationGasCost(final MessageFrame frame) {
    return 0;
  }

  @SuppressWarnings("removal")
  @Override
  public long create2OperationGasCost(final MessageFrame frame) {
    return 0;
  }

  @Override
  public long txCreateCost() {
    return 0;
  }

  @Override
  public long createKeccakCost(final int initCodeLength) {
    return 0;
  }

  @Override
  public long initcodeCost(final int initCodeLength) {
    return 0;
  }

  @Override
  public long gasAvailableForChildCreate(final long stipend) {
    return 0;
  }

  @Override
  public long dataCopyOperationGasCost(final MessageFrame frame, final long offset, final long length) {
    return 0;
  }

  @Override
  public long memoryExpansionGasCost(final MessageFrame frame, final long offset, final long length) {
    return 0;
  }

  @Override
  public long getBalanceOperationGasCost() {
    return 0;
  }

  @Override
  public long getBlockHashOperationGasCost() {
    return 0;
  }

  @Override
  public long expOperationGasCost(final int numBytes) {
    return 0;
  }

  @Override
  public long extCodeCopyOperationGasCost(final MessageFrame frame, final long offset, final long length) {
    return 0;
  }

  @Override
  public long extCodeHashOperationGasCost() {
    return 0;
  }

  @Override
  public long getExtCodeSizeOperationGasCost() {
    return 0;
  }

  @Override
  public long getJumpDestOperationGasCost() {
    return 0;
  }

  @Override
  public long logOperationGasCost(final MessageFrame frame, final long dataOffset, final long dataLength, final int numTopics) {
    return 0;
  }

  @Override
  public long mLoadOperationGasCost(final MessageFrame frame, final long offset) {
    return 0;
  }

  @Override
  public long mStoreOperationGasCost(final MessageFrame frame, final long offset) {
    return 0;
  }

  @Override
  public long mStore8OperationGasCost(final MessageFrame frame, final long offset) {
    return 0;
  }

  @Override
  public long selfDestructOperationGasCost(final Account recipient, final Wei inheritance) {
    return 0;
  }

  @Override
  public long keccak256OperationGasCost(final MessageFrame frame, final long offset, final long length) {
    return 0;
  }

  @Override
  public long getSloadOperationGasCost() {
    return 0;
  }

  @Override
  public long calculateStorageCost(final UInt256 newValue, final Supplier<UInt256> currentValue, final Supplier<UInt256> originalValue) {
    return 0;
  }

  @Override
  public long calculateStorageRefundAmount(final UInt256 newValue, final Supplier<UInt256> currentValue,
                                           final Supplier<UInt256> originalValue) {
    return 0;
  }

  @Override
  public long getSelfDestructRefundAmount() {
    return 0;
  }

  @Override
  public long codeDepositGasCost(final int codeSize) {
    return 0;
  }

  @Override
  public long transactionIntrinsicGasCost(final Transaction transaction, final long baselineGas) {
    return 0;
  }

  @Override
  public long transactionFloorCost(final Bytes transactionPayload, final long payloadZeroBytes) {
    return 0;
  }

  @Override
  public long getMinimumTransactionCost() {
    return 0;
  }

  @Override
  public long calculateGasRefund(final Transaction transaction, final MessageFrame initialFrame, final long codeDelegationRefund) {
    return 0;
  }
}
