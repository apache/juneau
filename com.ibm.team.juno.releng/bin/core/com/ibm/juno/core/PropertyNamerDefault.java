/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core;

import java.beans.*;

/**
 * Default property namer.
 * <p>
 * 	Examples:
 * <ul>
 * 	<li><js>"fooBar"</js> -&gt; <js>"fooBar"</js>
 * 	<li><js>"fooBarURL"</js> -&gt; <js>"fooBarURL"</js>
 * 	<li><js>"FooBarURL"</js> -&gt; <js>"fooBarURL"</js>
 * 	<li><js>"URL"</js> -&gt; <js>"URL"</js>
 * </ul>
 * <p>
 * 	See {@link Introspector#decapitalize(String)} for exact rules.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class PropertyNamerDefault implements PropertyNamer {

	@Override /* PropertyNamer */
	public String getPropertyName(String name) {
		return Introspector.decapitalize(name);
	}

}
