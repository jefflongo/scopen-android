package com.example.scopen;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  Design considerations:
 *  The scan-response messages should have same size in bytes. (Due to the property of UDP)
 *  The scan-response messages should follow a specific format.
 *  Format used:
 *  - < [identifier] | [version] | [serial number] | [ip address] | [port to pen] | [port from pen] >
 *  - The message string starts with < and ends with >
 *  - Variables are separated by |.
 *
 *  The TCP communication should include two parts of the information: Header and body.
 *  The header should have a fixed length, which includes the information about the body, such as the size in bytes.
 *  The body should contain the raw data, either this data is sampling data or simply command.
 *  Header format: [length: 4 bytes] [type: 1 byte]   All bytes use the big endian format.
 *  The transmiting thread should wait for the acknowledgement after it sends the header.
 *  After receiving the acknowledgement, it should proceed on sending the data.
 *  Acknowledge format: [length: 1 byte]      'A'     This byte use the big endian format.
 */

/**
 * CommManager class handles methods related to the communication between the PC and the pen.
 * Specifically, it includes the following functions:
 * - Search the network and look for the pen.
 * - Connect to the pen server through TCP communication.
 * - Send command to the server. (Actively)
 * - Receive commands from the server async.
 * - Make callback while new commands or data are received.
 */
public class CommManager {

  /**
   * Pen scanning related parameters.
   */
  public static final String SCAN_MESSAGE = "SCOPEN_SCAN";
  public static final int SCAN_TRANSMIT_PORT = 4445;
  public static final int SCAN_RECEIVE_PORT = 4446;
  public static final int SCAN_MESSAGE_SIZE = 1024;

  /**
   * Scanning response format.
   */
  public static final int SCOPEN_IDENTIFIER = 0x69;
  public static final int SCAN_RESPONSE_ARGC = 6;

  /**
   * TCP Transmission Related
   */
  public static final int HEADER_SIZE = 5;
  public static final int HEADER_SIZE_FEILD = 4;
  public static final int HEADER_TYPE_FIELD = 1;

  /**
   * Up Stream parameters
   * @note These parameters are closely related to the ESP32 send parameters.
   */
  public static final int ESP32_MAX_TRANSMIT_SIZE = 1436;


  // State machine
  private boolean isConnected = false;

  // Socket for listening the broadcast response.
  // Need to be a member variable because the timer thread should be able to end it.
  private DatagramSocket receiveSocket = null;

  // TCP communication variables.
  private Socket txSocket, rxSocket;
  private CommEventsInterface commEventsInterface;
  private Thread dataRecvThread;

  private final ArrayList<ScopenInfo> avaliableScopens;

  public CommManager(CommEventsInterface eventsHandler) {
    // Create an unconnected socket. Use connect to establish a connection.
    txSocket = new Socket();
    rxSocket = new Socket();
    // Receive a data receive handler to handle the event that data is received from the Scopen.
    commEventsInterface = eventsHandler;
    avaliableScopens = new ArrayList<>();
  }

  /**
   * A method that will scan the current network for available Scopens. It will try to send a scan request through the
   * broadcast channel and try to listen the broadcast channel for reply. This is an asynchronous method.
   */
  public void scanNetwork(int timeout)  {
    commEventsInterface.onScanStarted();
    synchronized (avaliableScopens) {
      // Clear the existing pens.
      avaliableScopens.clear();
    }

    // Start listening before sending the broadcast.
    final Thread udpRecvThread = new Thread(new UdpReceiveHandler(), "Receive Thread");
    udpRecvThread.setDaemon(true);
    udpRecvThread.start();

    // Send broadcast information to query the available pens.
    try {
      broadcast();
    } catch (Exception exception) {
      System.err.println("Failed to boardcast scan request.");
      commEventsInterface.onScanFinished();
      return;
    }

    // Set the timer to end the receive.
    Timer timer = new Timer(true);
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (udpRecvThread.isAlive()) {
          receiveSocket.close();
          receiveSocket = null;
          udpRecvThread.interrupt();
        }
      }
    }, timeout * 1000);
  }

  /**
   * Connect to an available Scopen based on the input Scopen information.
   * @param scopenInfo  An ScopenInfo object which has InetSocketAddress and
   * @return true if it's successful and false if the connection fails.
   */
  public void connectScopen(ScopenInfo scopenInfo) {
    final Thread connectorThread = new Thread(new scopenConnector(scopenInfo), "Connect Thread");
    connectorThread.setDaemon(true);
    connectorThread.start();
  }
  private class scopenConnector implements Runnable{
    ScopenInfo scopenInfo;
    public scopenConnector(ScopenInfo scopenInfo){
      this.scopenInfo = scopenInfo;
    }
    public boolean connectScopen(ScopenInfo scopenInfo){
      // Check if the connection is already established.
      if (txSocket == null || txSocket.isConnected() || rxSocket == null || rxSocket.isConnected()) {
        System.out.println("[CONNECT] Invalid socket or any of the socket is already connected.");
        return false;
      }

      // Try to connect to the pen using TCP.
      try {
        txSocket.connect(scopenInfo.getTxSocketAddress());
        rxSocket.connect(scopenInfo.getRxSocketAddress());
      } catch (Exception exception) {
        System.out.println("[CONNECT] Cannot connect to the address. Socket exception.");
        return false;
      }

      // Start the listening thread to receive the data and command from the pen.
      dataRecvThread = new Thread(new TCPReceiveHandler(), "TCP Receive Thread");
      dataRecvThread.setDaemon(true);
      dataRecvThread.start();
      // TODO: Probably add connect event handler.
      commEventsInterface.onConnected();
      isConnected = true;
      return true;
    }
    @Override
    public void run() {
      connectScopen(scopenInfo);
    }
  }
  /**
   * Disconnect the currently connected pen if there is.
   */
  public void disconnectScopen() {
    if (txSocket == null || !txSocket.isConnected())
      return;
    try {
      txSocket.close();
      rxSocket.close();
      txSocket = new Socket();
      rxSocket = new Socket();
      dataRecvThread.join();
      System.out.println("[DISCONNECT] Data receive thread closed successfully.");
      isConnected = false;
      // TODO: Add disconnect event handler
    } catch (Exception exception) {
      System.err.println("[DISCONNECT] Failed to close socket.");
    }
    commEventsInterface.onDisconnected();
  }

  /**
   * Transmit a chunck of bytes to the connected Scopen.
   * @param data  The raw data to be transmitted, a chunk of bytes.
   * @param type  The type of the data, such as command or data. Reserved for future use.
   * @return  true if the transmission is successful or false if it's not.
   */
  public void transmitCommand(byte[] data, byte type) {
    final Thread broadcastThread = new Thread(new Transmitter(data,type), "Transmit Thread");
    broadcastThread.setDaemon(true);
    broadcastThread.start();
  }
  private class Transmitter implements Runnable{
    byte [] data;
    byte type;
    public Transmitter(byte[] data, byte type){
      this.data = data;
      this.type = type;
    }
    private boolean transmitCommand(byte[] data, byte type){
      // Check if the connection is established.
      if (txSocket == null || !txSocket.isConnected())
        return false;
      InputStream inputStream;
      OutputStream outputStream;
      try {
        inputStream = txSocket.getInputStream();
        outputStream = txSocket.getOutputStream();
      } catch (Exception exception) {
        System.out.println("[TCP SEND] Cannot get i/o streams.");
        System.err.println(exception.getMessage());
        return false;
      }
      // Sending the header and waiting for the acknowledgement.
      // Construct the header.
      byte[] sizeField = ByteBuffer.allocate(HEADER_SIZE_FEILD).order(ByteOrder.BIG_ENDIAN).putInt(data.length).array();
      byte[] typeField = ByteBuffer.allocate(HEADER_TYPE_FIELD).order(ByteOrder.BIG_ENDIAN).put(type).array();
      byte[] header = ByteBuffer.allocate(HEADER_SIZE).put(sizeField).put(typeField).array();
      // Send the header
      try {
        outputStream.write(header);
        outputStream.flush();
      } catch (Exception exception) {
        System.err.println("[TCP SEND] Failed to send the header.");
        return false;
      }
      // Receive the acknowledge
      // TODO: Add timeout to this read.
      int ack;
      try {
        System.out.println("[TCP SEND] Waiting for the header ack.");
        ack = inputStream.read();
      } catch (Exception exception) {
        System.err.println("[TCP SEND] Reading acknowledgement failed.");
        return false;
      }
      // Check the validity of the acknowledgement.
      if (ack != (int)'A') {
        System.out.println("[TCP SEND] Received invalid header acknowledgement.");
        return false;
      }
      // Continue to transmit the raw data.
      try {
        outputStream.write(data);
        outputStream.flush();
      } catch (Exception exception) {
        System.err.println("[TCP SEND] Failed to send the payload.");
        return false;
      }
      // Receive the acknowledge
      // TODO: Add timeout to this read.
      try {
        System.out.println("[TCP SEND] Waiting for the body ack.");
        ack = inputStream.read();
      } catch (Exception exception) {
        System.err.println("[TCP SEND] Reading acknowledgement failed");
        return false;
      }
      // Check the validity of the acknowledgement.
      if (ack != (int)'A') {
        System.out.println("[TCP SEND] Recieved invalid body acknowledgement.");
        return false;
      }
      return true;
    }
    public void run(){
      transmitCommand(data,type);
    }
  };
   /**
   * Getters and setters.
   */
  public ArrayList<ScopenInfo> getAvaliableScopens() {
    return avaliableScopens;
  }

  /**
   * A method that broadcast in the current network to search for available Scopens.
   * This method will be run synchronously. It will block the thread until it's finished.
   */
  private void broadcast() throws Exception {
    final Thread broadcastThread = new Thread(new Broadcaster(), "Broadcast Thread");
    broadcastThread.setDaemon(true);
    broadcastThread.start();
  }

  private class Broadcaster implements Runnable{
    public void run(){
      try {
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.setBroadcast(true);
        // Convert the broadcast message to bytes.
        byte[] buffer = SCAN_MESSAGE.getBytes();
        // Broad cast the scan request.
        InetAddress broadcastAddress = InetAddress.getByName("192.168.4.255");
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, SCAN_TRANSMIT_PORT);
        datagramSocket.send(packet);
        datagramSocket.close();
      }catch (Exception e){
        System.out.println("error");
      }

    }
  };

  /**
   * ResponseHanlder class listens the multi-cast channel to wait for Scopens' information.
   */
  private class UdpReceiveHandler implements Runnable {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
      try {
        receiveScanResponse();
      } catch (Exception exception) {
         System.out.println("Receiving process stopped.");
//         System.err.println(exception.getMessage());
      }
//      System.out.println("Done listening scan response.");
      commEventsInterface.onScanFinished();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void receiveScanResponse() throws Exception {
      while(true) {
        byte[] buffer = new byte[SCAN_MESSAGE_SIZE];
        // Creating new socket.
        if (receiveSocket == null) {
          try {
            receiveSocket = new DatagramSocket(SCAN_RECEIVE_PORT);
          } catch (Exception exception) {
            System.err.println("Cannot create the receiveSocket");
            System.err.println(exception.getMessage());
            return;
          }
        }
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        // The receive process will block until the packet buffer has been filled.
        receiveSocket.receive(packet);
        String receivedString = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received udp response: " + receivedString);

        // Start parsing the result.
        int startIndex = receivedString.indexOf('<');
        int endIndex = receivedString.indexOf('>');
        String message = receivedString.substring(startIndex + 1, endIndex);
        String[] parameters = message.split("\\|");
        if (parameters.length != SCAN_RESPONSE_ARGC) {
          System.out.println("Received invalid scan response.");
          continue;
        }

        int identifier = Integer.parseUnsignedInt(parameters[0]);
        int version = Integer.parseUnsignedInt(parameters[1]);
        int txPort = Integer.parseUnsignedInt(parameters[4]);
        int rxPort = Integer.parseUnsignedInt(parameters[5]);
        String serial = parameters[2];
        String address = parameters[3];

        if (identifier != SCOPEN_IDENTIFIER) {
          System.out.println("Received valid scan response from an invalid product.");
          continue;
        }
        // Create the soketAddress objects
        InetAddress inetAddress = InetAddress.getByName(address);
        InetSocketAddress txAddress = new InetSocketAddress(inetAddress, txPort);
        InetSocketAddress rxAddress = new InetSocketAddress(inetAddress, rxPort);
        // Use synchronize to prevent race condition in the future updates.
        ScopenInfo scopenInfo = new ScopenInfo(serial, version, txAddress, rxAddress);
        synchronized (avaliableScopens) {
          avaliableScopens.add(scopenInfo);
        }
      }
    }
  }

  /**
   * ReceiveHanlder class listens the TCP channel to wait for the sampling results or commands from Scopens.
   */
  private class TCPReceiveHandler implements Runnable {
    @Override
    public void run() {
      System.out.println("[TCP RECV] TCP receive thread is running.");
      receiveScopenMessage();
      System.out.println("Done listening messages.");
    }

    private void receiveScopenMessage() {
      while (rxSocket != null && rxSocket.isConnected()) {
        // Get the input and output streams from the rxSocket
        InputStream inputStream;
        OutputStream outputStream;
        try {
          inputStream = rxSocket.getInputStream();
          outputStream = rxSocket.getOutputStream();
        } catch (Exception exception) {
          System.out.println("[TCP RECV] Cannot get i/o streams.");
          System.err.println(exception.getMessage());
          break;
        }

        // Trying to receive the header.
        int status;
        byte[] header = new byte[HEADER_SIZE];
        try {
          status = inputStream.read(header);
        } catch (IOException exception) {
          System.out.println("[TCP RECV] Socket is closed. Failed to receive header.");
          System.err.println(exception.getMessage());
          break;
        }
        if (status == -1) {
          System.out.println("[TCP RECV] EOF is received. Header received failed. Restart receive loop.");
          break;
        }

        // Parse the header information.
        ByteBuffer lengthField = ByteBuffer.wrap(header, 0, HEADER_SIZE_FEILD).order(ByteOrder.BIG_ENDIAN);
        ByteBuffer typeField = ByteBuffer.wrap(header, HEADER_SIZE_FEILD, 1);
        int dataSize = lengthField.getInt();
        int type = typeField.get();
        if (!CmdManager.verifyCommandType(type) || dataSize < 0 || dataSize > 20000)
          continue;
//         NOTE: Checked! Received right data.
//         System.out.println("Received header. Body size: " + dataSize + " Type: " + type);

        // Send the acknowledgement
        try {
          outputStream.write('A');
          //outputStream.flush();
        } catch (Exception exception) {
          System.err.println(exception.getMessage());
          System.err.println(exception.getStackTrace());
          break;
        }

        // Read the message.
        // Add timeout to this read.
        byte[] data = new byte[dataSize];
        int filledSize = 0;
        int leftSize = dataSize;
        while(leftSize > 0) {
          int currRecvSize = leftSize > ESP32_MAX_TRANSMIT_SIZE ? ESP32_MAX_TRANSMIT_SIZE : leftSize;
          byte[] buffer = new byte[currRecvSize];
          try {
            status = inputStream.read(buffer);
          } catch (Exception exception) {
            System.err.println("[TCP RECV] Failed to receive the data. Received: " + filledSize + " Left: " + leftSize);
            System.err.println(exception.getMessage());
            break;
          }
          // Check if the read is successful.
          if (status == -1) {
            System.err.println("[TCP RECV] Input stream has been closed. Stop listening.");
            break;
          } else if (status != currRecvSize) {
            System.err.println("[TCP RECV] Recieved size is not same as the currRecvSize");
            break;
          }
          // Replay with ACK
          try {
            outputStream.write('A');
            //outputStream.flush();
          } catch (Exception exception) {
            System.out.println("[TCP RECV] Failed to send ACK. Received: " + filledSize + " Left: " + leftSize);
            System.err.println(exception.getMessage());
            break;
          }
          // Fill the data array using the received data.
          System.arraycopy(buffer, 0, data, filledSize, currRecvSize);
          filledSize += currRecvSize;
          leftSize -= currRecvSize;
        }
        if (filledSize == dataSize) {
          System.out.println("[TCP RECV] Data received successfully.");
          System.out.println("[TCP RECV] Received size: " + data.length);
        }
        // Handle data receive event.
        commEventsInterface.onDataReceived(data, type);
      }
    }
  }

  /**
   * Getters and Setters
   */
  public boolean isConnected() {
    return isConnected;
  }

  public static void main(String[] args) {

  }

}
