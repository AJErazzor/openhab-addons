/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.lghombot.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link LGHomBotBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Fredrik Ahlström - Initial contribution
 */
@NonNullByDefault
public class LGHomBotBindingConstants {

    private LGHomBotBindingConstants() {
        throw new IllegalStateException("Utility class");
    }

    private static final String BINDING_ID = "lghombot";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_LGHOMBOT = new ThingTypeUID(BINDING_ID, "LGHomBot");

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_LGHOMBOT);

    // List of all Channel ids
    static final String CHANNEL_STATE = "state";
    static final String CHANNEL_BATTERY = "battery";
    static final String CHANNEL_CPU_LOAD = "cpuLoad";
    static final String CHANNEL_SRV_MEM = "srvMem";
    static final String CHANNEL_CLEAN = "clean";
    static final String CHANNEL_START = "start";
    static final String CHANNEL_HOME = "home";
    static final String CHANNEL_PAUSE = "pause";
    static final String CHANNEL_MODE = "mode";
    static final String CHANNEL_TURBO = "turbo";
    static final String CHANNEL_REPEAT = "repeat";
    static final String CHANNEL_NICKNAME = "nickname";
    static final String CHANNEL_MOVE = "move";
    static final String CHANNEL_CAMERA = "camera";
    static final String CHANNEL_LAST_CLEAN = "lastClean";
    static final String CHANNEL_MAP = "map";
    static final String CHANNEL_MONDAY = "monday";
    static final String CHANNEL_TUESDAY = "tuesday";
    static final String CHANNEL_WEDNESDAY = "wednesday";
    static final String CHANNEL_THURSDAY = "thursday";
    static final String CHANNEL_FRIDAY = "friday";
    static final String CHANNEL_SATURDAY = "saturday";
    static final String CHANNEL_SUNDAY = "sunday";

    // List of all HomBot states
    static final String HBSTATE_UNKNOWN = "UNKNOWN";
    static final String HBSTATE_WORKING = "WORKING";
    static final String HBSTATE_BACKMOVING = "BACKMOVING";
    static final String HBSTATE_BACKMOVING_INIT = "BACKMOVING_INIT";
    static final String HBSTATE_BACKMOVING_JOY = "BACKMOVING_JOY";
    static final String HBSTATE_PAUSE = "PAUSE";
    static final String HBSTATE_STANDBY = "STANDBY";
    static final String HBSTATE_HOMING = "HOMING";
    static final String HBSTATE_DOCKING = "DOCKING";
    static final String HBSTATE_CHARGING = "CHARGING";
    static final String HBSTATE_DIAGNOSIS = "DIAGNOSIS";
    static final String HBSTATE_RESERVATION = "RESERVATION";
    static final String HBSTATE_ERROR = "ERROR";
}
