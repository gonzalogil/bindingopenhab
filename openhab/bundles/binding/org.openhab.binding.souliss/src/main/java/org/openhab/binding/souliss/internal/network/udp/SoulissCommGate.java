/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.souliss.internal.network.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openhab.binding.souliss.internal.network.typicals.SoulissNetworkParameter;

/**
 * This class provide to construct MaCaco and UDP frame
 * @author Antonino-Fazio
 */
public class SoulissCommGate {
	
	private static Logger LOGGER = LoggerFactory.getLogger(SoulissCommGate.class);
	
	public static void sendFORCEFrame(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int IDNode, int slot, short shortCommand) {
		sendFORCEFrame(datagramSocket, soulissNodeIPAddress, soulissNodeIPAddressOnLAN, IDNode, slot, shortCommand, null, null, null);
	}

	//used for set dimmer value. It set command at first byte and dimmerVal to second byte
	public static void sendFORCEFrame(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int IDNode, int slot, short shortCommand, short lDimmer) {
		sendFORCEFrame(datagramSocket, soulissNodeIPAddress, soulissNodeIPAddressOnLAN, IDNode, slot, shortCommand, lDimmer, null, null);
	}

	//send force frame with command and RGB value
	public static void sendFORCEFrame(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int IDNode, int slot, short shortCommand, Short byte1, Short byte2, Short byte3) {
		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_force);
		
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) 0x0);// PUTIN
		MACACOframe.add((byte) 0x0);// PUTIN
		
		MACACOframe.add((byte) (IDNode));// Start Offset
		if( byte1==null && byte2==null && byte3==null )
			MACACOframe.add((byte) ((byte) slot+ 1)); //Number Of
		else if ( byte2==null && byte3==null )
			MACACOframe.add((byte) ((byte) slot+ 2)); //Number Of byte of payload= command + set byte
		else
			MACACOframe.add((byte) ((byte) slot+ 4)); //Number Of byte of payload= OnOFF + Red + Green + Blu
		
		for (int i=0;i<=slot-1;i++){
			MACACOframe.add((byte) 00); //pongo a zero i byte precedenti lo slot da modificare
		}
		MACACOframe.add((byte) shortCommand);// PAYLOAD

		if( byte1!=null && byte2!=null && byte3!=null ) {
			MACACOframe.add(byte1.byteValue());// PAYLOAD RED
			MACACOframe.add(byte2.byteValue());// PAYLOAD GREEN
			MACACOframe.add(byte3.byteValue());// PAYLOAD BLUE	
		} else if( byte1!=null) {
			MACACOframe.add(byte1.byteValue());// PAYLOAD DIMMER
		}
		
		LOGGER.debug("sendFORCEFrame - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
		
	}
	
	public static void sendDBStructFrame(DatagramSocket socket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN) {
		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_db_struct);
		MACACOframe.add((byte) 0x0);// PUTIN
		MACACOframe.add((byte) 0x0);// PUTIN
		MACACOframe.add((byte) 0x0);// Start Offset
		MACACOframe.add((byte) 0x07); //Number Of

		LOGGER.debug("sendDBStructFrame - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(socket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
		
//		Mi aspetto una risposta così:
//		nodes = mac.get(5);
//		maxnodes = mac.get(6);
//		maxTypicalXnode = mac.get(7);
//		maxrequests = mac.get(8);
//		MaCacoIN_s = mac.get(9);
//		MaCacoTyp_s = mac.get(10);
//		MaCacoOUT_s = mac.get(11);

	}
	
	private static void send(DatagramSocket socket, ArrayList<Byte> MACACOframe, String soulissNodeIPAddress, String sSoulissNodeIPAddressOnLAN) {

		int iUserIndex=SoulissNetworkParameter.UserIndex;
		int iNodeIndex= SoulissNetworkParameter.NodeIndex;;
		
		ArrayList<Byte> buf = buildVNetFrame(MACACOframe, sSoulissNodeIPAddressOnLAN, iUserIndex, iNodeIndex);
		byte[] merd = toByteArray(buf);
		
		InetAddress serverAddr;
		try {
			serverAddr = InetAddress.getByName(soulissNodeIPAddress);
			DatagramPacket packet = new DatagramPacket( merd, merd.length, serverAddr, ConstantsUDP.SOULISSPORT);
			SendDispatcherThread.put(socket, packet);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error(e.getMessage());
		}
	
	}


	private static ArrayList<Byte> buildVNetFrame(ArrayList<Byte> MACACOframe2, String soulissNodeIPAddress, int iUserIndex, int iNodeIndex) {
		ArrayList<Byte> frame = new ArrayList<Byte>();
		InetAddress ip;
		try {
			ip = InetAddress.getByName(soulissNodeIPAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return frame;
		}
		byte[] dude = ip.getAddress();
		frame.add((byte) 23);// PUTIN

		frame.add((byte) dude[3]);// es 192.168.1.XX BOARD
		// n broadcast : La comunicazione avviene utilizzando l'indirizzo IP
		// 255.255.255.255 a cui associare l'indirizzo vNet 0xFFFF.
			frame.add((byte) soulissNodeIPAddress.compareTo(ConstantsUDP.BROADCASTADDR) == 0 ? dude[2] : 0);
																					// 192.168.XX.0
		frame.add((byte) iNodeIndex); // NODE INDEX
		frame.add((byte) iUserIndex);// USER IDX

		// aggiunge in testa il calcolo
		frame.add(0, (byte) (frame.size() + MACACOframe2.size() + 1));
		frame.add(0, (byte) (frame.size() + MACACOframe2.size() + 1));// Check 2

		frame.addAll(MACACOframe2);

//		Byte[] ret = new Byte[frame.size()];
//
//		StringBuilder deb = new StringBuilder();
//		for (int i = 0; i < ret.length; i++) {
//			deb.append("0x" + Long.toHexString((long) frame.get(i) & 0xff) + " ");
//		}
				
		return frame;
		}

	/**
	 * Builds old-school byte array
	 * 
	 * @param buf
	 * @return
	 */
	private static byte[] toByteArray(ArrayList<Byte> buf) {
		byte[] merd = new byte[buf.size()];
		for (int i = 0; i < buf.size(); i++) {
			merd[i] = (byte) buf.get(i);
		}
		return merd;
	}
	
	public static void sendMULTICASTFORCEFrame(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, short typical, short shortCommand) {
		
		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_force_massive);
		
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) 0x0);// PUTIN
		MACACOframe.add((byte) 0x0);// PUTIN
		
		MACACOframe.add((byte) typical);// Start Offset
		MACACOframe.add((byte) 1); //Number Of
		
		MACACOframe.add((byte) shortCommand);// PAYLOAD
		LOGGER.debug("sendMULTICASTFORCEFrame - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
	}

	public static void sendPing(DatagramSocket datagramSocket, String soulissNodeIPAddress,  String soulissNodeIPAddressOnLAN, short putIn_1,	short punIn_2) {

		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_ping);
		
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) putIn_1);// PUTIN
		MACACOframe.add((byte) punIn_2);// PUTIN
		
		MACACOframe.add((byte) 0x00);// Start Offset
		MACACOframe.add((byte) 0x00); //Number Of
		LOGGER.debug("sendPing - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
	}

	public static void sendSUBSCRIPTIONframe(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int iNodes) {

		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_subscribe);
		
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte) 0x00);
		
		MACACOframe.add((byte) iNodes); // Start Offset (is the first node to consider
		LOGGER.debug("sendSUBSCRIPTIONframe - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);	
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
	}


	public static void sendHEALTY_REQUESTframe(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int iNodes) {

		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_healthReq);
		
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte) 0x00);
		MACACOframe.add((byte) iNodes); 
		LOGGER.debug("sendHEALTY_REQUESTframe - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
	}

	public static void sendTYPICAL_REQUESTframe(DatagramSocket datagramSocket, String soulissNodeIPAddress, String soulissNodeIPAddressOnLAN, int iNodes) {

		ArrayList<Byte> MACACOframe = new ArrayList<Byte>();
		MACACOframe.add((byte) ConstantsUDP.Souliss_UDP_function_typreq);
		// PUTIN, STARTOFFEST, NUMBEROF
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte) 0x00);// PUTIN
		MACACOframe.add((byte)  0x00); //startOffset
		MACACOframe.add((byte) iNodes); 
		LOGGER.debug("sendHEALTY_REQUESTframe - "+ MaCacoToString(MACACOframe) + ", soulissNodeIPAddress: " + soulissNodeIPAddress+ ", soulissNodeIPAddressOnLAN: "+ soulissNodeIPAddressOnLAN);
		send(datagramSocket, MACACOframe, soulissNodeIPAddress, soulissNodeIPAddressOnLAN);
	}

	
	
	static boolean flag=true;
	private static String MaCacoToString(ArrayList<Byte> mACACOframe) {
		while(!flag){};
		//copio array per evitare modifiche concorrenti
		ArrayList<Byte> mACACOframe2=new ArrayList<Byte> ();
		mACACOframe2.addAll(mACACOframe);
			flag=false;
			StringBuilder sb = new StringBuilder();
			sb.append("HEX: [");
			for (byte b : mACACOframe2) {
				sb.append(String.format("%02X ", b));
			}
			sb.append("]");
			flag=true;
			return sb.toString();
		}
		
	}

