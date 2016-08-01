/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import com.ibm.juno.core.annotation.*;

/**
 * Defines an API for converting conventional bean property names to some other form.
 * <p>
 * For example, given the bean property <js>"fooBarURL"</js>, the {@link PropertyNamerDashedLC}
 * 	property namer will convert this to <js>"foo-bar-url"</js>.
 * <p>
 * Property namers are associated with beans through the {@link Bean#propertyNamer} annotation.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public interface PropertyNamer {

	/**
	 * Convert the specified default property name to some other value.
	 * @param name The original bean property name.
	 * @return The converted property name.
	 */
	public String getPropertyName(String name);
}
