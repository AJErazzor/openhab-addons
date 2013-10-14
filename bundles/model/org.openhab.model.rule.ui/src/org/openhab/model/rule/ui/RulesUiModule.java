/**
 * Copyright (c) 2010-2013, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/*
 * generated by Xtext
 */
package org.openhab.model.rule.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.ui.editor.hover.IEObjectHoverProvider;
import org.openhab.model.script.scoping.ActionClasspathBasedTypeScopeProvider;
import org.openhab.model.script.scoping.ActionClasspathTypeProviderFactory;
import org.openhab.model.script.ui.contentassist.ActionEObjectHoverProvider;

/**
 * Use this class to register components to be used within the IDE.
 */
@SuppressWarnings("restriction")
public class RulesUiModule extends org.openhab.model.rule.ui.AbstractRulesUiModule {
	public RulesUiModule(AbstractUIPlugin plugin) {
		super(plugin);
	}

	public Class<? extends org.eclipse.xtext.common.types.access.IJvmTypeProvider.Factory> bindIJvmTypeProvider$Factory() {
		return ActionClasspathTypeProviderFactory.class;
	}
	
	public Class<? extends org.eclipse.xtext.common.types.xtext.AbstractTypeScopeProvider> bindAbstractTypeScopeProvider() {
		return ActionClasspathBasedTypeScopeProvider.class;
	}

	public Class<? extends IEObjectHoverProvider> bindIEObjectHoverProvider() {
        return ActionEObjectHoverProvider.class;
    }

}
