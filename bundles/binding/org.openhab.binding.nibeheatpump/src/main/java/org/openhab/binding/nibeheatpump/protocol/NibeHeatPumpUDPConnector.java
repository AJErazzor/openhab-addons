/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nibeheatpump.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import org.openhab.binding.nibeheatpump.internal.NibeHeatPumpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connector for UDP communication.
 * 
 * Command for testing:
 * 
 * OK:          echo -e "\x5C\x00\x20\x68\x50\x01\xA8\x1F\x01\x00\xA8\x64\x00\xFD\xA7\xD0\x03\x44\x9C\x1E\x00\x4F\x9C\xA0\x00\x50\x9C\x78\x00\x51\x9C\x03\x01\x52\x9C\x1B\x01\x87\x9C\x14\x01\x4E\x9C\xC6\x01\x47\x9C\x01\x01\x15\xB9\xB0\xFF\x3A\xB9\x4B\x00\xC9\xAF\x00\x00\x48\x9C\x0D\x01\x4C\x9C\xE7\x00\x4B\x9C\x00\x00\xFF\xFF\x00\x00\xFF\xFF\x00\x00\xFF\xFF\x00\x00\x45" | nc -4u -w1 localhost 9999
 * Special len: echo -e "\x5C\x00\x20\x68\x51\x44\x9C\x25\x00\x48\x9C\xFC\x00\x4C\x9C\xF1\x00\x4E\x9C\xC7\x01\x4D\x9C\x0B\x02\x4F\x9C\x25\x00\x50\x9C\x33\x00\x51\x9C\x0B\x01\x52\x9C\x5C\x5C\x01\x56\x9C\x31\x00\xC9\xAF\x00\x00\x01\xA8\x0C\x01\xFD\xA7\x16\xFA\xFA\xA9\x07\x00\x98\xA9\x1B\x1B\xFF\xFF\x00\x00\xA0\xA9\xCA\x02\xFF\xFF\x00\x00\x9C\xA9\x92\x12\xFF\xFF\x00\x00\xBE" | nc -4u -w1 localhost 9999
 * Special CRC: echo -e "\x5C\x00\x20\x68\x50\x44\x9C\x26\x00\x48\x9C\xF6\x00\x4C\x9C\xF1\x00\x4E\x9C\xD6\x01\x4D\x9C\x0C\x02\x4F\x9C\x45\x00\x50\x9C\x3F\x00\x51\x9C\xF1\x00\x52\x9C\x04\x01\x56\x9C\xD5\x00\xC9\xAF\x00\x00\x01\xA8\x0C\x01\xFD\xA7\x99\xFA\xFA\xA9\x02\x00\x98\xA9\x1A\x1B\xFF\xFF\x00\x00\xA0\xA9\xCA\x02\xFF\xFF\x00\x00\x9C\xA9\x92\x12\xFF\xFF\x00\x00\xC5" | nc -4u -w1 localhost 9999
 * CRC failure: echo -e "\x5C\x00\x20\x68\x50\x01\xA8\x1F\x01\x00\xA8\x64\x00\xFD\xA7\xD0\x03\x44\x9C\x1E\x00\x4F\x9C\xA0\x00\x50\x9C\x78\x00\x51\x9C\x03\x01\x52\x9C\x1B\x01\x87\x9C\x14\x01\x4E\x9C\xC6\x01\x47\x9C\x01\x01\x15\xB9\xB0\xFF\x3A\xB9\x4B\x00\xC9\xAF\x00\x00\x48\x9C\x0D\x01\x4C\x9C\xE7\x00\x4B\x9C\x00\x00\xFF\xFF\x00\x00\xFF\xFF\x00\x00\xFF\xFF\x00\x00\x44" | nc -4u -w1 localhost 9999
 *
 * @author Pauli Anttila
 * @since 1.3.0
 */
public class NibeHeatPumpUDPConnector extends NibeHeatPumpConnector {

	private static final Logger logger = LoggerFactory
			.getLogger(NibeHeatPumpUDPConnector.class);

	int port = 9999;
	DatagramSocket socket = null;

	public NibeHeatPumpUDPConnector(int port) {

		logger.debug("Nibe heatpump UDP message listener started");
		this.port = port;
	}

	@Override
	public void connect() throws NibeHeatPumpException {
		
		if (socket == null) {
			try {
				socket = new DatagramSocket(port);
			} catch (SocketException e) {
				throw new NibeHeatPumpException(e);
			}
		}
		
	}

	@Override
	public void disconnect() throws NibeHeatPumpException {
		
		if (socket != null) {
			socket.close();
		}
	}

	@Override
	public byte[] receiveDatagram() throws NibeHeatPumpException {

		final int PACKETSIZE = 255;

		try {

			if (socket == null) {
				socket = new DatagramSocket(port);
			}
			
			// Create a packet
			DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE],
					PACKETSIZE);

			// Receive a packet (blocking)
			socket.receive(packet);
			return Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

		} catch (SocketException e) {

			throw new NibeHeatPumpException(e);

		} catch (IOException e) {

			throw new NibeHeatPumpException(e);
		}

	}
}
