/**
 * openHAB, the open Home Automation Bus.
 * Copyright (C) 2010-2012, openHAB.org <admin@openhab.org>
 *
 * See the contributors.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or
 * combining it with Eclipse (or a modified version of that library),
 * containing parts covered by the terms of the Eclipse Public License
 * (EPL), the licensors of this Program grant you additional permission
 * to convey the resulting work.
 */

package org.openhab.binding.openenergymonitor.protocol;

import org.openhab.binding.openenergymonitor.internal.OpenEnergyMonitorException;

/**
 * Class for present data parser rule.
 * 
 * @author Pauli Anttila
 * @since 1.4.0
 */
public class OpenEnergyMonitorParserRule {
	
	public enum DataType {
		U8, U16, U32, S8, S16, S32;
	}

	byte address = 0;
	DataType datatype = DataType.U32;
	int[] bytesIndex = null;
	
	public OpenEnergyMonitorParserRule(String rule) throws OpenEnergyMonitorException {
		try {
			
			String[] parts = rule.split(":");
			
			if(parts.length != 2) {
				throw new OpenEnergyMonitorException("Invalid parser rule '" + rule + "'");
			}
			
			this.address = Byte.parseByte(parts[0]);
			String[] b = parts[1].split("\\|");
			bytesIndex = new int[b.length];
			
			for (int i=0; i<b.length; i++) {
				bytesIndex[i] = Integer.parseInt(b[i]);
			}
			
		} catch (Exception e) {
			throw new OpenEnergyMonitorException("Invalid parser rule", e);
		}
	}
	
	public byte getAddress() {
		return address;
	}

	public int[] getParseBytes() {
		return bytesIndex;
	}

}