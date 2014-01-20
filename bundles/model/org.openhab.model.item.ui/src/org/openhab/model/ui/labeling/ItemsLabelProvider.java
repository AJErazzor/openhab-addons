/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/*
* generated by Xtext
*/
package org.openhab.model.ui.labeling;

import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider;
import org.openhab.model.items.ModelBinding;
import org.openhab.model.items.ModelGroupItem;
import org.openhab.model.items.ModelItem;
import org.openhab.model.items.ModelNormalItem;

import com.google.inject.Inject;

/**
 * Provides labels for a EObjects.
 * 
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#labelProvider
 */
public class ItemsLabelProvider extends DefaultEObjectLabelProvider {

	@Inject
	public ItemsLabelProvider(AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}

	String text(ModelItem item) {
		if(item instanceof ModelGroupItem) {
			return "Group " + item.getName();
		}
		if(item instanceof ModelNormalItem) {
			String name = item.getName();
			return ((ModelNormalItem) item).getType() + " " + name;
		}
		return item.getLabel();
	}
	
	String text(ModelBinding binding) {
		return binding.getType();
	}

    String image(ModelItem item) {
		if(item instanceof ModelGroupItem) {
			return "group.png";
		}
		if(item instanceof ModelNormalItem) {
			return ((ModelNormalItem) item).getType().toLowerCase() + ".png";
		}
		return null;
    }

    String image(ModelBinding binding) {
		return "binding.png";
    }
}
