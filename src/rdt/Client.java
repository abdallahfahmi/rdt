package rdt;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client extends Thread{
	String serverIP;
	short serverPort;
	String clientIP;
	short clientPort;
	String fileName;
	DatagramSocket recivingSocket;
	DatagramSocket sendingSocket;
	ConcurrentLinkedQueue<ACKPacket> ackQueue = new ConcurrentLinkedQueue<ACKPacket>();
	CopyOnWriteArrayList<Short> recivedPacketNumbers = new CopyOnWriteArrayList<Short>();
	ConcurrentLinkedQueue<TCPPacket> recivedPackets = new ConcurrentLinkedQueue<TCPPacket>();
	
	
	public Client(String serverIP, short serverPort, String clientIP, short clientPort, String fileName) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		this.clientIP = clientIP;
		this.clientPort = clientPort;
		this.fileName = fileName;
		try {
			// Two sockets one for sending ACKs and one for receiving data from server.
			recivingSocket = new DatagramSocket(this.clientPort);
			sendingSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		} 
		
	}

	@Override
	public void run() {
		try {
			ClientDataHandler handler = new ClientDataHandler();   // handles receiving data.
			ClientACKHandler ackHandler = new ClientACKHandler();  // handles sending ACKs.
			
			// Create a new packet and add the needed file name to it.
			byte[] fileNameBuffer = this.fileName.getBytes();
			TCPPacket filePacket = new TCPPacket(this.clientPort, this.serverPort,(short) 0, fileNameBuffer);
			filePacket.setFileName(this.fileName);
			byte[] filePacketBytes = filePacket.encode();
			DatagramPacket fileNamePacket = new DatagramPacket(filePacketBytes, filePacketBytes.length, InetAddress.getLocalHost(), this.serverPort);
			
			handler.start();
			ackHandler.start();
			
			// Keep sending the packet every 5 seconds until the handler starts receiving.
			while(!handler.isReciving()) {
				this.sendingSocket.send(fileNamePacket);
				Thread.sleep(5000);
			}
			
			// wait for both handlers to finish before exiting.
			handler.join();
			ackHandler.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class ClientDataHandler extends Thread {
		boolean reciving = false;
		boolean lastPacketRecived = false;
		
		@Override
		public void run() {
			try {
				// Wait for the first packet to be received.
				byte[] packetBuffer = new byte[512];
				DatagramPacket recivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
				recivingSocket.receive(recivedPacket);
				
				// set the receiving to stop requesting the file from the server.
				this.reciving = true;
				recivePacket(recivedPacket);
				
				// Keep receiving the data packet until the last packet is received.
				while(!lastPacketRecived) {
					packetBuffer = new byte[512];
					recivedPacket = new DatagramPacket(packetBuffer, packetBuffer.length);
					recivingSocket.receive(recivedPacket);
					recivePacket(recivedPacket);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void recivePacket(DatagramPacket recivedDatagram) {
			// Decode the received packet.
			byte[] encodedData = recivedDatagram.getData();
			TCPPacket recivedPacket = new TCPPacket();
			recivedPacket.decode(encodedData);
			
			if (!recivedPacket.isCorrupted()) {
				// Create a new ACK packet.
				short sequanceNumber = recivedPacket.getSequanceNumber();
				ACKPacket ackPacket = new ACKPacket(sequanceNumber);
				
				// Add the packet if not already received.
				if (!recivedPacketNumbers.contains(sequanceNumber)) {
					recivedPacketNumbers.add(sequanceNumber);
					recivedPackets.offer(recivedPacket);
				}
				
				// Set the flag if it is the last  packet.
				if (recivedPacket.isFinalPacket()) {
					lastPacketRecived = true;
					ackPacket.setFinalACKPacket(true);
				}
				
				// Add packet to queue.
				ackQueue.offer(ackPacket);
			}
		}
		
		public boolean isReciving() {
			return reciving;
		}
	}
	
	private class ClientACKHandler extends Thread{

		@Override
		public void run() {
			try {
				while(true) {
					if (ackQueue.isEmpty()) {
						continue;
					}
					// get next ACK packetm encode it and send it.
					ACKPacket ackPacket = ackQueue.poll();
					byte[] buffer = ackPacket.encode();
					DatagramPacket Packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), serverPort);
					recivingSocket.send(Packet);
					
					// If final packet sent break.
					if (ackPacket.isFinalACKPacket()) {
						break;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}
