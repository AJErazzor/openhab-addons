package org.openhab.binding.fs20.internal;

import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.fs20.FS20BindingConfig;
import org.openhab.binding.fs20.FS20BindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.types.Command;
import org.openhab.io.transport.cul.CULCommunicationException;
import org.openhab.io.transport.cul.CULDeviceException;
import org.openhab.io.transport.cul.CULHandler;
import org.openhab.io.transport.cul.CULListener;
import org.openhab.io.transport.cul.CULManager;
import org.openhab.io.transport.cul.CULMode;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the communcation between openHAB and FS20 devices. Via
 * RF received updates are received directly, there is no polling.
 * 
 * @author Till Klocke
 * @since 1.4.0
 */
public class FS20Binding extends AbstractActiveBinding<FS20BindingProvider>
		implements ManagedService, CULListener {

	private static final Logger logger = LoggerFactory
			.getLogger(FS20Binding.class);

	private final static String KEY_DEVICE_NAME = "device";

	private String deviceName;

	private CULHandler cul;

	/**
	 * the refresh interval which is used to poll values from the FS20 server
	 * (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;

	public FS20Binding() {
	}

	public void activate() {
		logger.debug("Activating FS20 binding");
	}

	private void setNewDeviceName(String deviceName) {
		if (cul != null) {
			CULManager.close(cul);
		}
		this.deviceName = deviceName;
		getCULHandler();
	}

	private void getCULHandler() {
		try {
			logger.debug("Opening CUL device on " + deviceName);
			cul = CULManager.getOpenCULHandler(deviceName, CULMode.SLOW_RF);
			cul.registerListener(this);
		} catch (CULDeviceException e) {
			logger.error("Can't open cul device", e);
			cul = null;
		}
	}

	public void deactivate() {
		logger.debug("Deactivating FS20 binding");
		cul.unregisterListener(this);
		CULManager.close(cul);
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected String getName() {
		return "FS20 Refresh Service";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void execute() {
		// Nothing to do here
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		FS20BindingConfig bindingConfig = null;
		for (FS20BindingProvider provider : super.providers) {
			bindingConfig = provider.getConfigForItemName(itemName);
			if (bindingConfig != null) {
				break;
			}
		}
		if (bindingConfig != null) {
			logger.debug("Received command " + command.toString()
					+ " for item " + itemName);
			try {
				FS20Command fs20Command = FS20CommandHelper
						.convertHABCommandToFS20Command(command);
				cul.send("F" + bindingConfig.getAddress()
						+ fs20Command.getHexValue());
			} catch (CULCommunicationException e) {
				logger.error("An exception occured while sending a command", e);
			}
		}
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	public void updated(Dictionary<String, ?> config)
			throws ConfigurationException {
		logger.debug("Received new config");
		if (config != null) {

			// to override the default refresh interval one has to add a
			// parameter to openhab.cfg like
			// <bindingName>:refresh=<intervalInMs>
			String refreshIntervalString = (String) config.get("refresh");
			if (StringUtils.isNotBlank(refreshIntervalString)) {
				refreshInterval = Long.parseLong(refreshIntervalString);
			}
			String deviceName = (String) config.get(KEY_DEVICE_NAME);
			if (StringUtils.isEmpty(deviceName)) {
				logger.error("No device name configured");
				setProperlyConfigured(false);
				throw new ConfigurationException(KEY_DEVICE_NAME,
						"The device name can't be empty");
			} else {
				setNewDeviceName(deviceName);
			}

			setProperlyConfigured(true);
			// read further config parameters here ...

		}
	}

	@Override
	public void dataReceived(String data) {
		// It is possible that we see here messages of other protocols
		if (data.startsWith("F")) {
			logger.debug("Received FS20 message: " + data);
			handleReceivedMessage(data);
		}

	}

	private void handleReceivedMessage(String message) {
		String houseCode = (message.substring(1, 5));
		String address = (message.substring(5, 7));
		String command = message.substring(7, 9);
		String fullAddress = houseCode + address;
		FS20BindingConfig config = null;
		for (FS20BindingProvider provider : providers) {
			config = provider.getConfigForAddress(fullAddress);
			if (config != null) {
				break;
			}
		}
		if (config != null) {
			FS20Command fs20Command = FS20Command.getFromHexValue(command);
			logger.debug("Received command " + fs20Command.toString()
					+ " for device " + config.getAddress());
			eventPublisher.postUpdate(config.getItem().getName(),
					FS20CommandHelper.getStateFromFS20Command(fs20Command));
		} else {
			logger.debug("Received message for unknown device " + fullAddress);
		}
	}

	@Override
	public void error(Exception e) {
		logger.error("Error while communicating with CUL", e);

	}

}
