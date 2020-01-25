package rdt;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Server extends Thread{
	String serverIP;
	short serverPort;
	DatagramSocket sendingSocket;
	DatagramSocket recivingSocket;
	
	public Server (String serverIP, short serverPort) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
		try {
			sendingSocket = new DatagramSocket();
			recivingSocket = new DatagramSocket(this.serverPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO
	}
	
	
	private class ServerDataHandler extends Thread{
		
		boolean sending;
		boolean lastPacketSent;
		
		//run()
		
		public DatagramPacket sendPacket(TCPPacket packet) {
			
			byte[] data = packet.encode();
			DatagramPacket dpacket = new DatagramPacket(data,packet.getSequanceNumber());
			
			return dpacket;
			
		}
		
	}
	
}
