/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.maxcul.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.openhab.binding.maxcul.MaxCulBindingProvider;
import org.openhab.core.binding.BindingChangeListener;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for parsing the binding configuration
 * and registering the {@link MaxCulBindingProvider}.
 *
 * The following devices have the following valid types:
 * <li>RadiatorThermostat - thermostat,temperature,battery,valvepos</li>
 * <li>WallThermostat - thermostat,temperature,battery</li>
 *
 * The generic binding configuration format is (optional arguments in []):
 * <code>{ maxcul="&lt;deviceType&gt;:&lt;serialNum&gt;:[bindingType]:[configTemp=&lt;comfortTemp&gt;/&lt;ecoTemp&gt;/&lt;maxTemp&gt;/&lt;minTemp&gt;/&lt;windowOpenTemperature&gt;/&lt;windowOpenDuration&gt;/&lt;measurementOffset&gt;]:[assoc=&lt;serialNum&gt;]
 *
 * Not setting configTemp will use whatever is already programmed into the device. Setting windowOpenTemp to anything other than 'Off' will enable detection of a window being opened using temperature. This would result in the thermostat turning off for windowOpenDuration minutes(?)
 * Setting assoc will associate the device specified with the one in the binding. This means that they will communicate directly changes in setpoint etc.
 *
 * Examples:
 * <li><code>{ maxcul="RadiatorThermostat:JEQ1234565" }</code> - will return/set the thermostat temperature of radiator thermostat with the serial number JEQ0304492</li>
 * <li><code>{ maxcul="RadiatorThermostat:JEQ1234565:battery" }</code> - will return the battery level of JEQ0304492</li>
 * <li><code>{ maxcul="WallThermostat:JEQ1234566:temperature" }</code> - will return the temperature of a wall mounted thermostat with serial number JEQ0304447</li>
 * <li><code>{ maxcul="WallThermostat:JEQ1234566:thermostat" }</code> - will set/return the desired temperature of a wall mounted thermostat with serial number JEQ0304447</li>
 * <li><code>{ maxcul="PushButton:JEQ1234567" }</code> - will default to 'switch' mode</li>
 * <li><code>{ maxcul="PairMode" }</code> - Switch only, enables pair mode for 60s. Will automatically switch off after this time.</li>
 * <li><code>{ maxcul="ListenMode" }</code> - Switch only, doesn't process messages - just listens to traffic, parses and outputs it.</li>
 * @author Paul Hampson (cyclingengineer)
 * @since 1.6.0
 */
public class MaxCulGenericBindingProvider extends AbstractGenericBindingProvider implements MaxCulBindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "maxcul";
	}

	private static final Logger logger =
			LoggerFactory.getLogger(MaxCulGenericBindingProvider.class);

	private HashMap<String,HashSet<MaxCulBindingConfig>> associationMap = new HashMap<String,HashSet<MaxCulBindingConfig>>();

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		MaxCulBindingConfig config = new MaxCulBindingConfig(bindingConfig);

		switch (config.getDeviceType())
		{
		case PAIR_MODE:
		case LISTEN_MODE:
			if (!(item instanceof SwitchItem))
				throw new BindingConfigParseException("Invalid item type. PairMode/ListenMode can only be a switch");
			break;
		case PUSH_BUTTON:
		case SHUTTER_CONTACT:
			if (config.getFeature() == MaxCulFeature.BATTERY && !(item instanceof SwitchItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'battery' can only be a Switch");
			if (config.getFeature() == MaxCulFeature.SWITCH && !(item instanceof SwitchItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'switch' can only be a Switch");
			break;
		case RADIATOR_THERMOSTAT:
		case RADIATOR_THERMOSTAT_PLUS:
		case WALL_THERMOSTAT:
			if (config.getFeature() == MaxCulFeature.TEMPERATURE && !(item instanceof NumberItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'temperature' can only be a Number");
			else if (config.getFeature() == MaxCulFeature.VALVE_POS && !(item instanceof NumberItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'valvepos' can only be a Number");
			else if (config.getFeature() == MaxCulFeature.THERMOSTAT && !((item instanceof NumberItem) || (item instanceof SwitchItem)))
				throw new BindingConfigParseException("Invalid item type. Feature 'thermostat' can only be a Number or a Switch");
			else if (config.getFeature() == MaxCulFeature.BATTERY && !(item instanceof SwitchItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'battery' can only be a Switch");
			else if (config.getFeature() == MaxCulFeature.MODE && !(item instanceof NumberItem))
				throw new BindingConfigParseException("Invalid item type. Feature 'mode' can only be a Number");
			break;
		default:
			throw new BindingConfigParseException("Invalid config device type. Wasn't expecting "+config.getDeviceType());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);

		final String itemName = item.getName();

		logger.debug("Processing item "+itemName);
		final MaxCulBindingConfig config = new MaxCulBindingConfig(bindingConfig);

		addBindingConfig(item, config);
		buildAssociationMap(); // update association map

		addBindingChangeListener(new BindingChangeListener() {

			@Override
			public void bindingChanged(BindingProvider provider, String itemName) {
				/* binding changed so update the association map */
				buildAssociationMap();
				// TODO check if config temperatures are set and flag that they should be sent the device because they might have changed?
			}

			@Override
			public void allBindingsChanged(BindingProvider provider) {
				if (!provider.providesBindingFor(itemName))
				{
					// TODO get serial number of itemName
					// then check if we still interact with that device, if not then
					// deassociate and send a reset to it
				}
			}
		});
	}

	@Override
	public MaxCulBindingConfig getConfigForItemName(String itemName) {
		MaxCulBindingConfig config = null;
		if (super.bindingConfigs.containsKey(itemName)) {
			config = (MaxCulBindingConfig) super.bindingConfigs.get(itemName);
		}
		return config;
	}

	public String getItemNameForConfig(MaxCulBindingConfig bc)
	{
		String itemName = null;
		if (super.bindingConfigs.containsValue(bc))
		{
			for (Entry<String,BindingConfig> entry : super.bindingConfigs.entrySet())
			{
				if (entry.getValue().equals(bc))
				{
					itemName = entry.getKey();
					break;
				}
			}
		}
		return itemName;
	}

	@Override
	public MaxCulBindingConfig getConfigForSerialNumber(String serial) {
		MaxCulBindingConfig config = null;
		for (BindingConfig c : super.bindingConfigs.values() )
		{
			config = (MaxCulBindingConfig)c;
			if (config.getSerialNumber().equalsIgnoreCase(serial))
				return config;
		}
		return null;
	}

	@Override
	public List<MaxCulBindingConfig> getConfigsForSerialNumber(String serial) {
		List<MaxCulBindingConfig> configs = new ArrayList<MaxCulBindingConfig>();
		for (BindingConfig c : super.bindingConfigs.values() )
		{
			MaxCulBindingConfig config = (MaxCulBindingConfig)c;
			if (config.getSerialNumber() != null) /* could be PairMode/ListenMode device which has no serial */
			{
				logger.debug("Comparing '"+config.getSerialNumber()+"' with '"+serial+"'");
				if (config.getSerialNumber().compareToIgnoreCase(serial) == 0)
					configs.add(config);
			}
		}
		if (configs.isEmpty())
			return null;
		else
			return configs;
	}

	@Override
	public List<MaxCulBindingConfig> getConfigsForRadioAddr(String addr) {
		List<MaxCulBindingConfig> configs = new ArrayList<MaxCulBindingConfig>();
		for (BindingConfig c : super.bindingConfigs.values() )
		{
			MaxCulBindingConfig config = (MaxCulBindingConfig)c;
			if (config.getSerialNumber() != null) /* could be PairMode/ListenMode device which has no serial */
			{
				logger.debug("Comparing '"+config.getDevAddr()+"' with '"+addr+"'");
				if (config.getDevAddr().equalsIgnoreCase(addr))
					configs.add(config);
			}
		}
		return configs;
	}

	private void buildAssociationMap() {
		/* loop over all bindings finding their associated devices and create entries for
		 * each one. So end up with something like (psuedo binding code):
		 * Step 1:
		 *  dev A { assoc=B,C }
		 * 	result:
		 * 		A -> B,C
		 * Step 2:
		 * dev B { assoc=C }
		 *  result:
		 * 		A -> B,C
		 * 		B -> C
		 * Step 3:
		 * dev B { assoc=D }
		 *  result:
		 * 		A -> B,C
		 * 		B -> C,D
		 */
		if (super.bindingConfigs.values().isEmpty() == false)
		{
			logger.debug("Found "+super.bindingConfigs.values().size()+" binding configs to process in association map");
			for (BindingConfig c : super.bindingConfigs.values())
			{
				MaxCulBindingConfig config = (MaxCulBindingConfig)c;
				logger.debug("Processing "+config.getSerialNumber()+" with "+config.getAssociatedSerialNum().size()+" associations");
				if (associationMap.containsKey(config.getSerialNumber()) && config.getAssociatedSerialNum().isEmpty() == false)
				{
					/* serial number already exists in the map so check
					 * if we need to add any devices to the association
					 */
					HashSet<MaxCulBindingConfig> set = associationMap.get(config.getSerialNumber());
					logger.debug("Found "+config.getSerialNumber()+" in map already with "+set.size()+" entrys");
					for (String serial : config.getAssociatedSerialNum())
					{
						MaxCulBindingConfig bc = getConfigForSerialNumber(serial);
						if (bc != null && set.contains(bc) == false)
						{
							set.add(bc);
						}
					}
				}
				else if (config.getAssociatedSerialNum().isEmpty() == false)
				{
					/* new serial number, add it and it's associations */
					HashSet<MaxCulBindingConfig> set = new HashSet<MaxCulBindingConfig>();
					for (String serial : config.getAssociatedSerialNum())
					{
						/* add first config for this serial number. This is enough to give us
						 * device type and destination address which is all we need.
						 */
						MaxCulBindingConfig bc = getConfigForSerialNumber(serial);
						if (bc != null)
						{
							logger.debug("Adding "+serial+" to set for "+config.getSerialNumber());
							set.add(bc);
						}
					}
					/* only add if it has entries */
					if (!set.isEmpty())
						associationMap.put(config.getSerialNumber(), set);
				}
			}

			/* debug print of association map */
			if (!associationMap.isEmpty())
			{
				for (String serialKey : associationMap.keySet())
				{
					if (serialKey != null)
					{
						logger.debug("Device "+serialKey+" associated with:");
						for (MaxCulBindingConfig bc : associationMap.get(serialKey))
						{
							logger.debug("\t=> "+bc.getSerialNumber());
						}
					}
				}
			}
		}
	}

	public HashSet<MaxCulBindingConfig> getAssociations(String deviceSerial) {
		return associationMap.get(deviceSerial);
	}
}
