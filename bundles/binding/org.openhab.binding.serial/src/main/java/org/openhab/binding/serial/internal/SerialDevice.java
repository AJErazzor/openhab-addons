/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.serial.internal;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a serial device that is linked to exactly one String item and/or Switch item.
 * 
 * @author Kai Kreuzer
 *
 */
public class SerialDevice implements SerialPortEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SerialDevice.class);

	private String port;
	private int baud = 9600;

	private EventPublisher eventPublisher;

	private CommPortIdentifier portId;
	private SerialPort serialPort;

	private InputStream inputStream;

	private OutputStream outputStream;

	private Map<String, ItemType> patternMap;

	class ItemType {
		Pattern pattern;
		Class<?> type;
	}
	
	public boolean isEmpty() {
		return patternMap.isEmpty();
	}
	
	public void addRegEx(String itemName, Class<?> type, Pattern pattern) {
		if(patternMap == null)
			patternMap = new HashMap<String, ItemType>();
		
		ItemType typeItem = new ItemType();
		typeItem.pattern = pattern;
		typeItem.type = type;
		
		patternMap.put(itemName, typeItem);
	}
	
	public void removeRegEx(String itemName) {
		if(patternMap != null) {
			patternMap.remove(itemName);
		}
	}

	public SerialDevice(String port) {
		this.port = port;
	}

	public SerialDevice(String port, int baud) {
		this.port = port;
		this.baud = baud;
	}

	public void setEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void unsetEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = null;
	}

	public String getPort() {
		return port;
	}

	/**
	 * Initialize this device and open the serial port
	 * 
	 * @throws InitializationException if port can not be opened
	 */
	@SuppressWarnings("rawtypes")
	public void initialize() throws InitializationException {
		// parse ports and if the default port is found, initialized the reader
		Enumeration portList = CommPortIdentifier.getPortIdentifiers();
		while (portList.hasMoreElements()) {
			CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
			if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if (id.getName().equals(port)) {
					logger.debug("Serial port '{}' has been found.", port);
					portId = id;
				}
			}
		}
		if (portId != null) {
			// initialize serial port
			try {
				serialPort = (SerialPort) portId.open("openHAB", 2000);
			} catch (PortInUseException e) {
				throw new InitializationException(e);
			}

			try {
				inputStream = serialPort.getInputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}

			try {
				serialPort.addEventListener(this);
			} catch (TooManyListenersException e) {
				throw new InitializationException(e);
			}

			// activate the DATA_AVAILABLE notifier
			serialPort.notifyOnDataAvailable(true);

			try {
				// set port parameters
				serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);
			} catch (UnsupportedCommOperationException e) {
				throw new InitializationException(e);
			}

			try {
				// get the output stream
				outputStream = serialPort.getOutputStream();
			} catch (IOException e) {
				throw new InitializationException(e);
			}
		} else {
			StringBuilder sb = new StringBuilder();
			portList = CommPortIdentifier.getPortIdentifiers();
			while (portList.hasMoreElements()) {
				CommPortIdentifier id = (CommPortIdentifier) portList.nextElement();
				if (id.getPortType() == CommPortIdentifier.PORT_SERIAL) {
					sb.append(id.getName() + "\n");
				}
			}
			throw new InitializationException("Serial port '" + port + "' could not be found. Available ports are:\n" + sb.toString());
		}
	}

	public void serialEvent(SerialPortEvent event) {
		switch (event.getEventType()) {
		case SerialPortEvent.BI:
		case SerialPortEvent.OE:
		case SerialPortEvent.FE:
		case SerialPortEvent.PE:
		case SerialPortEvent.CD:
		case SerialPortEvent.CTS:
		case SerialPortEvent.DSR:
		case SerialPortEvent.RI:
		case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
			break;
		case SerialPortEvent.DATA_AVAILABLE:
			// we get here if data has been received
			StringBuilder sb = new StringBuilder();
			byte[] readBuffer = new byte[20];
			try {
				do {
					// read data from serial device
					while (inputStream.available() > 0) {
						int bytes = inputStream.read(readBuffer);
						sb.append(new String(readBuffer, 0, bytes));
					}
					try {
						// add wait states around reading the stream, so that interrupted transmissions are merged
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// ignore interruption
					}
				} while (inputStream.available() > 0);
				// sent data
				String result = sb.toString();

				// send data to the bus
				logger.debug("Received message '{}' on serial port {}", new String[] { result, port });

				if (eventPublisher != null) {
					if(patternMap != null && !patternMap.isEmpty()) {
						for (Entry<String, ItemType> entry : patternMap.entrySet()) {

							// use pattern
							if(entry.getValue().pattern != null) {
								
								Matcher matcher = entry.getValue().pattern.matcher(result);

								while (matcher.find()) {
									if(matcher.groupCount() > 0) {
										String group = matcher.group(1);
										if(entry.getValue().type.equals(NumberItem.class)) {
											try {
												eventPublisher.postUpdate(entry.getKey(), new DecimalType(group));
											} catch (NumberFormatException e) {
												logger.warn("Unable to convert regex expression '{}' for item {} to number", new String[] { result, entry.getKey()});
											}
										} else {
											eventPublisher.postUpdate(entry.getKey(), new StringType(group));
										}
									}
								}
								
							} else if(entry.getValue().type.isInstance(StringItem.class)) {
								eventPublisher.postUpdate(entry.getKey(), new StringType(result));
								
							} else if(entry.getValue().type.isInstance(SwitchItem.class) && result.trim().isEmpty()) {
								eventPublisher.postUpdate(entry.getKey(), OnOffType.ON);
								eventPublisher.postUpdate(entry.getKey(), OnOffType.OFF);
							}
						}
					}
					
				}

			} catch (IOException e) {
				logger.debug("Error receiving data on serial port {}: {}", new String[] { port, e.getMessage() });
			}
			break;
		}
	}

	/**
	 * Sends a string to the serial port of this device
	 * 
	 * @param msg the string to send
	 */
	public void writeString(String msg) {
		logger.debug("Writing '{}' to serial port {}", new String[] { msg, port });
		try {
			// write string to serial port
			outputStream.write(msg.getBytes());
			outputStream.flush();
		} catch (IOException e) {
			logger.error("Error writing '{}' to serial port {}: {}", new String[] { msg, port, e.getMessage() });
		}
	}

	/**
	 * Close this serial device
	 */
	public void close() {
		serialPort.removeEventListener();
		IOUtils.closeQuietly(inputStream);
		IOUtils.closeQuietly(outputStream);
		serialPort.close();
	}
}
