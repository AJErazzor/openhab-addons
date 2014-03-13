/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xpl.internal;

import java.util.Set;
import org.openhab.binding.xpl.xPLBindingConfig;
import org.openhab.binding.xpl.xPLBindingProvider;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;
import org.cdp1802.xpl.*;

/**
 * This class is responsible for parsing the binding configuration.
 * 
 * @author Clinique
 * @since 1.5.0
 */
public class xPLGenericBindingProvider extends AbstractGenericBindingProvider implements xPLBindingProvider {

	/**
	 * {@inheritDoc}
	 */
	public String getBindingType() {
		return "xpl";
	}

	/**
	 * @{inheritDoc}
	 */
	@Override
	public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
		if (!(	item instanceof SwitchItem || 
				item instanceof NumberItem ||
				item instanceof StringItem) ) {
			throw new BindingConfigParseException("item '" + item.getName()
					+ "' is of type '" + item.getClass().getSimpleName()
					+ "', only String, Switch and Number are allowed - please check your *.items configuration");
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public xPLBindingConfig getConfig(String itemName) {
		xPLBindingConfig config = (xPLBindingConfig) bindingConfigs.get(itemName);
		return config;
	}
	
	@Override
	public Item getItem(String itemName) {
		for (Set<Item> items : contextMap.values()) {
			if (items != null) {
				for (Item item : items) {
					if (itemName.equals(item.getName())) {
						return item;
					}
				}
			}
		}
		return null;
	}	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String hasMessage(xPL_MessageI theMessage) {
		if (theMessage.getType() == xPL_MessageI.MessageType.COMMAND) {										// the message must not be is not a xpl-cmnd
			return null;
		}

		for (String key: bindingConfigs.keySet()) {
			xPLBindingConfig config = (xPLBindingConfig) bindingConfigs.get(key);
			NamedValuesI theBody = config.Message.getMessageBody();
			if ( (theBody !=  null) && 	(!theBody.isEmpty()) &&
					(config.Message.getTarget().isBroadcastIdentifier() || config.Message.getTarget().equals(theMessage.getSource()))	&&
					config.Message.getSchemaClass().equalsIgnoreCase(theMessage.getSchemaClass()) &&
					config.Message.getSchemaType().equalsIgnoreCase(theMessage.getSchemaType()) 					
				) 
			{			
				boolean bodyMatched = true;									
				for(NamedValueI theValue: theBody.getAllNamedValues()) {						// iterate through the item body to 
					String aKey = theValue.getName();											// see if ...
					String aValue = theValue.getValue();
					String bValue = theMessage.getNamedValue(aKey);
					boolean	lineMatched = (bValue != null) && (
											aKey.equalsIgnoreCase(config.NamedParameter) ||
											aValue.equalsIgnoreCase(bValue)
										   );
					
					bodyMatched = bodyMatched && lineMatched;
				}
				
				if (bodyMatched) {
					return key;
				}					
			}
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processBindingConfiguration(String context, Item item, String bindingConfig) throws BindingConfigParseException {
		super.processBindingConfiguration(context, item, bindingConfig);
		
		String[] configParts = bindingConfig.trim().split(",");
		if (configParts.length < 5) {
			throw new BindingConfigParseException("xPL binding configuration must contain : target,message type,schema at at least one body key/value pair");
		}
		xPLBindingConfig config = new xPLBindingConfig();
		
		config.Message = xPL_Utils.createMessage();
		config.Message.setTarget(configParts[0]);
		
	    // Parse type
	    if (configParts[1].equalsIgnoreCase("TRIGGER"))
	    	config.Message.setType(xPL_MessageI.MessageType.TRIGGER);
	    else if (configParts[1].equalsIgnoreCase("STATUS"))
	    	config.Message.setType(xPL_MessageI.MessageType.STATUS);
	    else if (configParts[1].equalsIgnoreCase("COMMAND"))
	    	config.Message.setType(xPL_MessageI.MessageType.COMMAND);
	    else
	    	config.Message.setType(xPL_MessageI.MessageType.UNKNOWN);
	    
		config.Message.setSchema(configParts[2]);
		
	    // Parse name/value pairs 	   
	    String theName = null;
	    String theValue = null;
	    int delimPtr;
	    
	    for (int pairPtr = 3; pairPtr < configParts.length; pairPtr++) {
	    	delimPtr = configParts[pairPtr].indexOf("=");
	    	theName = configParts[pairPtr].substring(0, delimPtr);
	    	theValue = configParts[pairPtr].substring(delimPtr + 1);
	    	config.Message.addNamedValue(theName, theValue);
	    	if (theValue.equalsIgnoreCase("#COMMAND") || theValue.equalsIgnoreCase("#CURRENT")) {
	    		config.NamedParameter = new String(theName);
	    	}
	    } 
		
		addBindingConfig(item, config);		
	}
		
}

