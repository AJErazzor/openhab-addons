/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.wemo.internal;


import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import org.openhab.binding.wemo.WemoBindingProvider;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.net.http.HttpUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Hans-Jörg Merk
 * @since 1.6.0
 */
public class WemoBinding extends AbstractActiveBinding<WemoBindingProvider> implements ManagedService {

	private static final Logger logger = 
		LoggerFactory.getLogger(WemoBinding.class);

	protected Map<String, String> wemoConfigMap = new HashMap<String, String>();
	
	
	/** 
	 * the refresh interval which is used to poll values from the Wemo-Devices
	 */
	private long refreshInterval = 60000;

	public InetAddress address;
	public boolean isOn;
	
	public void activate() {
		//Start device discovery, each time the binding start.
		wemoDiscovery();
	}
	
	public void deactivate() {
	}

	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected String getName() {
		return "Wemo Refresh Service";
	}
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void execute() {
		logger.debug("execute() method is called!");
		
		for (WemoBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				logger.trace("Wemo switch '{}' state will be updated", itemName);

				try {
					String resp = wemoCall(itemName,
							"urn:Belkin:service:basicevent:1#GetBinaryState",
							IOUtils.toString(getClass().getResourceAsStream(
									"GetRequest.xml")));

					String state = resp.replaceAll(
							"[\\d\\D]*<BinaryState>(.*)</BinaryState>[\\d\\D]*", "$1");

					isOn = state.equals("1") ? true : false;
					logger.trace("{} state on = {}", itemName, isOn);
					
					if (state.equals("0")) {
						State itemState = OnOffType.valueOf("OFF");
						logger.trace("Transformed state for item {} = {}", itemName, state);
						eventPublisher.postUpdate(itemName, itemState);
					}
					if (state.equals("1")) {
						State itemState = OnOffType.valueOf("ON");
						logger.trace("Transformed state for item {} = {}", itemName, state);
						eventPublisher.postUpdate(itemName, itemState);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		logger.debug("internalReceiveCommand() is called!");

		for (WemoBindingProvider provider : providers) {
			String switchFriendlyName = provider.getWemoFriendlyName(itemName);
		    logger.info("item '{}' is configured as '{}'",itemName, switchFriendlyName);
			}
		try {
			if (OnOffType.ON.equals(command)) {
				logger.trace("Command ON is about to be send to item '{}'",itemName );
				boolean onOff = true;
				setOn(itemName, onOff);
			} else if (OnOffType.OFF.equals(command)) {
				logger.trace("Command OFF is about to be send to item '{}'",itemName );
				boolean onOff = false;
				setOn(itemName, onOff);
			}
		} catch (Exception e) {
			logger.error("Failed to send {} command", command, e);
		}
	}			


	/**
	 * @{inheritDoc}
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		logger.debug("internalReceiveCommand() is called!");
	}
		
	public void wemoDiscovery() {
		logger.debug("wemoDiscovery() is called!");
		try {
			final int SSDP_PORT = 1900;
			final int SSDP_SEARCH_PORT = 1901;

			// Broadcast address
			final String SSDP_IP = "239.255.255.250";

			// Connection timeout
			int TIMEOUT = 5000;

			// Send from localhost:1901
			InetAddress localhost = InetAddress.getLocalHost();
			InetSocketAddress srcAddress = new InetSocketAddress(localhost,	SSDP_SEARCH_PORT);
			
			// Send to 239.255.255.250:1900
			InetSocketAddress dstAddress = new InetSocketAddress(InetAddress.getByName(SSDP_IP), SSDP_PORT);

			// Request-Packet-Constructor
			StringBuffer discoveryMessage = new StringBuffer();
			discoveryMessage.append("M-SEARCH * HTTP/1.1\r\n");
			discoveryMessage.append("HOST: " + SSDP_IP + ":" + SSDP_PORT + "\r\n");
			discoveryMessage.append("ST: urn:Belkin:device:controllee:1\r\n");
			discoveryMessage.append("MAN: \"ssdp:discover\"\r\n");
			discoveryMessage.append("MX: 5\r\n");
			discoveryMessage.append("\r\n");
		    logger.trace("Request: {}", discoveryMessage.toString());
			byte[] discoveryMessageBytes = discoveryMessage.toString().getBytes();
			DatagramPacket discoveryPacket = new DatagramPacket(
					discoveryMessageBytes, discoveryMessageBytes.length, dstAddress);

			// Send multi-cast packet
			MulticastSocket multicast = null;
			try {
				multicast = new MulticastSocket(null);
				multicast.bind(srcAddress);
				logger.trace("Source-Address = '{}'", srcAddress);
				multicast.setTimeToLive(4);
				logger.trace("Send multicast request.");
				multicast.send(discoveryPacket);
			} finally {
				logger.trace("Multicast ends. Close connection.");
				multicast.disconnect();
				multicast.close();
			}

			// Response-Listener
			DatagramSocket wemoReceiveSocket = null;
			DatagramPacket receivePacket = null;
			try {
				wemoReceiveSocket = new DatagramSocket(SSDP_SEARCH_PORT);
				wemoReceiveSocket.setSoTimeout(TIMEOUT);
				logger.trace("Send datagram packet.");
				wemoReceiveSocket.send(discoveryPacket);

				while (true) {
					try {
						logger.trace("Receive SSDP Message.");
						receivePacket = new DatagramPacket(new byte[1536], 1536);
						wemoReceiveSocket.receive(receivePacket);
						final String message = new String(receivePacket.getData());
						logger.trace("Recieved message: {}", message);
				
						new Thread(new Runnable() {
							@Override
							public void run() {
								String messageSearch = "LOCATION:";
								int findString=message.lastIndexOf(messageSearch);
								if (findString !=0) {
								String slicedMessage = message.substring(findString+10, findString+36);
								logger.trace("Wemo found at URL '{}'", slicedMessage);
								
								try {
									int timeout = 5000;
									String friendlyNameResponse = HttpUtil.executeUrl("GET", slicedMessage+"/setup.xml", timeout);
									String findFriendlyNameStart = "<friendlyName>";
									String findFriendlyNameEnd = "</friendlyName>";
									int findStart=friendlyNameResponse.lastIndexOf(findFriendlyNameStart); 
									int findEnd=friendlyNameResponse.lastIndexOf(findFriendlyNameEnd);
									String slicedFriendlyName=friendlyNameResponse.substring(findStart+14, findEnd);
									logger.trace("Wemo friendlyName '{}' found at '{}'", slicedFriendlyName, slicedMessage);
									wemoConfigMap.put(slicedFriendlyName, slicedMessage);
									
								} catch (Exception te) {
									logger.error("Response transformation throws exception ", te);
								}
								}
							}
						}).start();

					} catch (SocketTimeoutException e) {
						logger.error("Message receive timed out.");
						break;
					}
				}
			} finally {
				if (wemoReceiveSocket != null) {
					wemoReceiveSocket.disconnect();
					wemoReceiveSocket.close();
				}
			}
			
		} catch (Exception e) {
			logger.error("Could not start wemo device discovery", e);
		}
		
	}
	public void setOn(String itemName, boolean onOff) throws IOException {
		logger.trace("setOn ={}", onOff);
		String wemoCallResponse = wemoCall(itemName,
				"urn:Belkin:service:basicevent:1#SetBinaryState",
				IOUtils.toString(
						getClass().getResourceAsStream("SetRequest.xml"))
						.replace("{{state}}", onOff ? "1" : "0"));

		logger.trace("setOn ={}", wemoCallResponse);
		isOn = onOff;
	}

	private String wemoCall(String itemName, String soapCall, String content) {
		try {
			
			String endpoint = "/upnp/control/basicevent1";
			String switchFriendlyName = null;
			
			for (WemoBindingProvider provider : providers) {
				switchFriendlyName = provider.getWemoFriendlyName(itemName);
				}

			String wemoLocation = wemoConfigMap.get(switchFriendlyName);
			if (wemoLocation != null) {
				logger.trace("item '{}' is located at '{}'", itemName, wemoLocation);
				URL url = new URL(wemoLocation + endpoint);
				Socket wemoSocket = new Socket(InetAddress.getByName(url.getHost()), url.getPort());
				try {
					OutputStream wemoOutputStream = wemoSocket.getOutputStream();
					StringBuffer wemoStringBuffer = new StringBuffer();
					wemoStringBuffer.append("POST " + url + " HTTP/1.1\r\n");
					wemoStringBuffer.append("Content-Type: text/xml; charset=utf-8\r\n");
					wemoStringBuffer.append("Content-Length: " + content.getBytes().length + "\r\n");
					wemoStringBuffer.append("SOAPACTION: \"" + soapCall + "\"\r\n");
					wemoStringBuffer.append("\r\n");
					wemoOutputStream.write(wemoStringBuffer.toString().getBytes());
					wemoOutputStream.write(content.getBytes());
					wemoOutputStream.flush();
					String wemoCallResponse = IOUtils.toString(wemoSocket.getInputStream());
					return wemoCallResponse;
				} finally {
					wemoSocket.close();
					}
			} else {
				logger.trace("No Location found for item '{}', start new discovery ", itemName);
				wemoDiscovery();
				String wemoCallResponse = "";
				return wemoCallResponse;
			}
		} catch (Exception e) {
			wemoDiscovery();
			throw new RuntimeException("Could not call Wemo, did rediscovery", e);
		}
	}
		
	
	/**
	 * @{inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		setProperlyConfigured(true);		
		if (config != null) {
			String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
			}

		}
	}

}
