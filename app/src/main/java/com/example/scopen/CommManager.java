package com.example.scopen;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class CommManager{
    public static String SCAN_MESSAGE = "SCOPEN_SCAN";
    public static int SCAN_TRANSMIT_PORT = 4445;
    public static int SCAN_RECEIVE_PORT = 4446;
    public static int SCAN_MESSAGE_SIZE = 1024;

    // Scanning response format.
    public static int SCOPEN_IDENTIFIER = 0x69;
    public static int SCAN_RESPONSE_ARGC = 6;

    // Transmission header format.
    public static int HEADER_SIZE = 5;
    public static int HEADER_SIZE_FEILD = 4;
    public static int HEADER_TYPE_FIELD = 1;

    // State machine
    private boolean isConnected = false;

    // Socket for listening the broadcast response.
    // Need to be a member variable because the timer thread should be able to end it.
    private DatagramSocket receiveSocket = null;

    // TCP communication variables.
    private Socket txSocket, rxSocket;
    private CommEventsInterface eventsInterface;
    private Thread dataRecvThread;

    private final ArrayList<ScopenInfo> avaliableScopens;

    public CommManager(CommEventsInterface eventsHandler) {
        // Create an unconnected socket. Use connect to establish a connection.
        txSocket = new Socket();
        rxSocket = new Socket();
        // Receive a data receive handler to handle the event that data is received from the Scopen.
        eventsInterface = eventsHandler;
        avaliableScopens = new ArrayList<>();
    }



    /**
     * A method that will scan the current network for available Scopens. It will try to send a scan request through the
     * broadcast channel and try to listen the broadcast channel for reply. This is an asynchronous method.
     */
    public void scanNetwork(int timeout)  {
        eventsInterface.onScanStarted();
        synchronized (avaliableScopens) {
            // Clear the existing pens.
            avaliableScopens.clear();
        }

        // Start listening before sending the broadcast.
        final Thread receiveThread = new Thread(new ResponseHandler(), "Receive Thread");
        receiveThread.setDaemon(true);
        receiveThread.start();

        // Send broadcast information to query the available pens.
        try {
            broadcast();
        } catch (Exception exception) {
            System.err.println("Failed to boardcast scan request.");
            eventsInterface.onScanFinished();
            return;
        }

        // Set the timer to end the receive.
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (receiveThread.isAlive()) {
                    receiveSocket.close();
                    receiveThread.interrupt();
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
        // Try to connect to the pen using TCP.
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
            if (txSocket == null || txSocket.isConnected() || rxSocket == null || rxSocket.isConnected())
                return false;
            try {
                txSocket.connect(scopenInfo.getTxSocketAddress());
                rxSocket.connect(scopenInfo.getRxSocketAddress());
            } catch (Exception exception) {
                return false;
            }
            // Start the listening thread to receive the data and command from the pen.
            dataRecvThread = new Thread(new ReceiveHandler(), "TCP Receive Thread");
            dataRecvThread.setDaemon(true);
            dataRecvThread.start();
            // TODO: Probably add connect event handler.
            eventsInterface.onConnected();
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
            dataRecvThread.join();
            eventsInterface.onDisconnected();
            isConnected = false;
            // TODO: Add disconnect event handler
        } catch (Exception exception) {
            System.err.println("Failed to close socket.");
        }
    }

    /**
     * Transmit a chunck of bytes to the connected Scopen.
     * @param data  The raw data to be transmitted, a chunk of bytes.
     * @param type  The type of the data, such as command or data. Reserved for future use.
     * @return  true if the transmission is successful or false if it's not.
     */
    public boolean transmit(byte[] data, byte type) {
        // Check if the connection is established.
        if (txSocket == null || !txSocket.isConnected())
            return false;
        InputStream inputStream;
        OutputStream outputStream;
        try {
            inputStream = txSocket.getInputStream();
            outputStream = txSocket.getOutputStream();
        } catch (Exception exception) {
            System.err.println(exception.getMessage());
            System.err.println(exception.getStackTrace());
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
        } catch (Exception exception) {
            System.err.println("Failed to send the header.");
            return false;
        }
        // Receive the acknowledge
        // TODO: Add timeout to this read.
        int ack;
        try {
            ack = inputStream.read();
        } catch (Exception exception) {
            System.err.println("Reading acknowledgement failed.");
            return false;
        }
        // Check the validity of the acknowledgement.
        if (ack != (int)'A') {
            System.out.println("Received invalid header acknowledgement.");
            return false;
        }
        // Continue to transmit the raw data.
        try {
            outputStream.write(data);
        } catch (Exception exception) {
            System.err.println("Failed to send the payload.");
            return false;
        }
        // Receive the acknowledge
        // TODO: Add timeout to this read.
        try {
            ack = inputStream.read();
        } catch (Exception exception) {
            System.err.println("Reading acknowledgement failed");
            return false;
        }
        // Check the validity of the acknowledgement.
        if (ack != (int)'A') {
            System.out.println("Recieved invalid body acknowledgement.");
            return false;
        }
        return true;
    }

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
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
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
    private class ResponseHandler implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void run() {
            try {
                receiveScanResponse();
            } catch (Exception exception) {
                // System.err.println("Receiving failed or process stopped.");
            }
            System.out.println("Done listening scan response.");
            eventsInterface.onScanFinished();
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private void receiveScanResponse() throws Exception {
            while(true) {
                byte[] buffer = new byte[SCAN_MESSAGE_SIZE];
                // Creating new socket.
                receiveSocket = new DatagramSocket(SCAN_RECEIVE_PORT);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                // The receive process will block until the packet buffer has been filled.
                receiveSocket.receive(packet);
                String receivedString = new String(packet.getData(), 0, packet.getLength());

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
    private class ReceiveHandler implements Runnable {
        @Override
        public void run() {
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
                    System.err.println(exception.getMessage());
                    System.err.println(exception.getStackTrace());
                    break;
                }

                // Trying to receive the header.
                int status;
                byte[] header = new byte[HEADER_SIZE];
                try {
                    status = inputStream.read(header);
                } catch (IOException exception) {
                    System.err.println(exception.getMessage());
                    System.err.println(exception.getStackTrace());
                    break;
                }
                if (status == -1) {
                    System.out.println("TCP input stream has been closed. Stop listening.");
                    break;
                }

                // Parse the header information.
                ByteBuffer lengthField = ByteBuffer.wrap(header, 0, HEADER_SIZE_FEILD);
                ByteBuffer typeField = ByteBuffer.wrap(header, HEADER_SIZE_FEILD, 1);
                int dataSize = lengthField.getInt();
                byte type = typeField.get();

                // Send the acknowledgement
                try {
                    outputStream.write('A');
                } catch (Exception exception) {
                    System.err.println(exception.getMessage());
                    System.err.println(exception.getStackTrace());
                    break;
                }

                // Read the message.
                // Add timeout to this read.
                byte[] data = new byte[dataSize];
                try {
                    status = inputStream.read(data);
                } catch (Exception exception) {
                    System.err.println(exception.getMessage());
                    System.out.println(exception.getStackTrace());
                    break;
                }
                if (status == -1) {
                    System.out.println("TCP input stream has been closed. Stop listening.");
                    break;
                } else if (status != dataSize) {
                    System.out.println("Recieved message that are not the same as the sized indicated in the header.");
                    continue;
                }

                // Handle data receive event.
                eventsInterface.onDataReceived(data, type);
            }
        }
    }

    /**
     * Getters and Setters
     */
    public boolean isConnected() {
        return isConnected;
    }



}
