package org.hyperledger.besu.evm.internal;

import java.util.Arrays;
import java.util.Objects;

import org.apache.tuweni.bytes.Bytes32;
import org.hyperledger.besu.datatypes.Address;

public record TransientStorageKey(Address address, Bytes32 slot) implements Comparable<TransientStorageKey> {
  @Override
  public int hashCode() {
    int initialValue = Arrays.hashCode(address.getBytes().toArrayUnsafe());
    return 31 * initialValue + Arrays.hashCode(slot.toArrayUnsafe());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (obj instanceof TransientStorageKey other) {
      return Objects.equals(address, other.address) && Objects.equals(slot, other.slot);
    }
    return false;
  }

  @Override
  public int compareTo(final TransientStorageKey other) {
    if (this == other) {
      return 0;
    }
    if (other == null) {
      return 1;
    }
    int compare = Arrays.compare(address.getBytes().toArrayUnsafe(), other.address.getBytes().toArrayUnsafe());
    return compare != 0 ? compare : Arrays.compare(slot.toArrayUnsafe(), other.slot.toArrayUnsafe());
  }
}