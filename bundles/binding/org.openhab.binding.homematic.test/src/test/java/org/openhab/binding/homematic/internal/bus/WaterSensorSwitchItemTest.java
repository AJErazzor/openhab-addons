/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homematic.internal.bus;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openhab.binding.homematic.internal.device.ParameterKey;
import org.openhab.binding.homematic.test.HomematicBindingProviderMock;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;

@Ignore("Switch Item not yet suppported")
public class WaterSensorSwitchItemTest extends BasicBindingTest {

    @Before
    public void setupProvider() {
        provider.setItem(new SwitchItem(HomematicBindingProviderMock.DEFAULT_ITEM_NAME));
    }

    @Test
    public void allBindingsChangedNoWater() {
        String sensor = "0";
        checkInitialValue(ParameterKey.STATE, new Integer(sensor), OnOffType.OFF);
    }

    @Test
    public void allBindingsChangedHumid() {
        String sensor = "1";
        checkInitialValue(ParameterKey.STATE, new Integer(sensor), OnOffType.OFF);
    }

    @Test
    public void allBindingsChangedWater() {
        String sensor = "2";
        checkInitialValue(ParameterKey.STATE, new Integer(sensor), OnOffType.ON);
    }

    @Test
    public void receiveSensorUpdateNoWater() {
        String sensor = "0";
        checkValueReceived(ParameterKey.STATE, new Integer(sensor), OnOffType.OFF);
    }

    @Test
    public void receiveSensorUpdateHumid() {
        String sensor = "1";
        checkValueReceived(ParameterKey.STATE, new Integer(sensor), OnOffType.OFF);
    }

    @Test
    public void receiveSensorUpdateWater() {
        String sensor = "2";
        checkValueReceived(ParameterKey.STATE, new Integer(sensor), OnOffType.ON);
    }

}
