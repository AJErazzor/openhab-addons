/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.serialmessage;

import org.openhab.binding.zwave.internal.protocol.SerialMessage;
import org.openhab.binding.zwave.internal.protocol.TransmissionState;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveWakeUpCommandClass;
import org.openhab.binding.zwave.internal.protocol.commandclass.ZWaveCommandClass.CommandClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller
 * 
 * @author Chris Jackson
 * @since 1.5.0
 */
public class SendDataMessageClass extends ZWaveCommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SendDataMessageClass.class);

	@Override
	public boolean handleResponse(ZWaveController zController, SerialMessage lastSentMessage,
			SerialMessage incomingMessage) {
		logger.trace("Handle Message Send Data Response");
		if (incomingMessage.getMessagePayloadByte(0) != 0x00) {
			logger.debug("Sent Data successfully placed on stack.");
		}
		else {
			// This is an error. This means that the transaction is complete!
			// Set the flag, and return false.
			logger.error("Sent Data was not placed on stack due to error {}.", incomingMessage.getMessagePayloadByte(0));
//			transactionComplete = true;

			return false;
		}
		
		return true;
	}

	@Override
	public boolean handleRequest(ZWaveController zController, SerialMessage lastSentMessage,
			SerialMessage incomingMessage) {
		logger.trace("Handle Message Send Data Request");

		int callbackId = incomingMessage.getMessagePayloadByte(0);
		TransmissionState status = TransmissionState.getTransmissionState(incomingMessage.getMessagePayloadByte(1));
		SerialMessage originalMessage = lastSentMessage;

		if (status == null) {
			logger.warn("Transmission state not found, ignoring.");
			return false;
		}

		ZWaveNode node = zController.getNode(originalMessage.getMessageNode());
		if(node == null) {
			logger.warn("Node not found!");
			return false;
		}

		logger.debug("NODE {}: SendData Request. CallBack ID = {}, Status = {}({})", node.getNodeId(), callbackId, status.getLabel(), status.getKey());

		if (originalMessage == null || originalMessage.getCallbackId() != callbackId) {
			logger.warn("NODE {}: Already processed another send data request for this callback Id, ignoring.", node.getNodeId());
			return false;
		}

		switch (status) {
		case COMPLETE_OK:
			// Consider this as a received frame since the controller did receive an ACK from the device.
			node.incrementReceiveCount();

			// If the node is DEAD, but we've just received a message from it, then it's not dead!
			if(node.isDead()) {
				node.setAlive();
				logger.debug("NODE {}: Node has risen from the DEAD. Set stage to {}.", node.getNodeId(), node.getNodeStage());			
			}
			else {
				node.resetResendCount();
			}
			checkTransactionComplete(lastSentMessage, incomingMessage);
			return true;
		case COMPLETE_NO_ACK:
			// timeOutCount.incrementAndGet();
		case COMPLETE_FAIL:
		case COMPLETE_NOT_IDLE:
		case COMPLETE_NOROUTE:
			try {
				handleFailedSendDataRequest(zController, originalMessage);
			} finally {
				transactionComplete = true;
			}
			break;
		default:
			break;
		}

		return false;
	}

	public boolean handleFailedSendDataRequest(ZWaveController zController, SerialMessage originalMessage) {

		ZWaveNode node = zController.getNode(originalMessage.getMessageNode());

		// No retries if the node is DEAD or FAILED
		if (node.isDead()) {
			return false;
		}

		// High priority messages get requeued, low priority get dropped
		if (!node.isListening() && !node.isFrequentlyListening()
				&& originalMessage.getPriority() != SerialMessagePriority.Low) {
			ZWaveWakeUpCommandClass wakeUpCommandClass = (ZWaveWakeUpCommandClass) node
					.getCommandClass(CommandClass.WAKE_UP);

			if (wakeUpCommandClass != null) {
				// It's a battery operated device, place in wake-up queue.
				wakeUpCommandClass.setAwake(false);
				wakeUpCommandClass.processOutgoingWakeupMessage(originalMessage);
				return false;
			}
		} else if (!node.isListening() && !node.isFrequentlyListening()
				&& originalMessage.getPriority() == SerialMessagePriority.Low)
			return false;

		node.incrementResendCount();

		logger.error("NODE {}: Got an error while sending data. Resending message.", node.getNodeId());
		zController.sendData(originalMessage);
		return true;
	}
}
