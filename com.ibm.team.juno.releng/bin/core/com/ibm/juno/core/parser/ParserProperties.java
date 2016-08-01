/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.parser;

import com.ibm.juno.core.*;

/**
 * Configurable properties common to all {@link Parser} classes.
 * <p>
 * 	Use the {@link Parser#setProperty(String, Object)} method to set property values.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class ParserProperties implements Cloneable {

	/**
	 * Debug mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * Enables the following additional information during parsing:
	 * <ul>
	 * 	<li>When bean setters throws exceptions, the exception includes the object stack information
	 * 		in order to determine how that method was invoked.
	 * </ul>
	 */
	public static final String PARSER_debug = "Parser.debug";

	boolean
		debug = false;

	/**
	 * Sets the specified property value.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(PARSER_debug))
			debug = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	@Override /* Object */
	public ParserProperties clone() throws CloneNotSupportedException {
		return (ParserProperties)super.clone();
	}
}
