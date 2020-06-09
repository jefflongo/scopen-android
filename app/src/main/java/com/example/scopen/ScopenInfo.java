package com.example.scopen;

import java.net.InetSocketAddress;

/**
 * ScopenInfo class is used for storing and encapsulating the scopen related information.
 */
public class ScopenInfo {
  private String serialNumber;
  private int version;
  private InetSocketAddress txSocketAddress;
  private InetSocketAddress rxSocketAddress;

  public ScopenInfo(String serialNumber, int version, InetSocketAddress txAddress, InetSocketAddress rxAddress) {
    this.serialNumber = serialNumber;
    this.version = version;
    this.txSocketAddress = txAddress;
    this.rxSocketAddress = rxAddress;
  }

  /**
   * Getter methods
   */
  public InetSocketAddress getTxSocketAddress() {
    return txSocketAddress;
  }

  public InetSocketAddress getRxSocketAddress() { return rxSocketAddress; }

  public String getSerialNumber() {
    return serialNumber;
  }

  int getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "Scopen#:" + serialNumber;
  }
}
