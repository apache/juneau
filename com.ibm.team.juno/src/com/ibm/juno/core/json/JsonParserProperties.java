/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.json;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Configurable properties on the {@link JsonParser} class.
 * <p>
 * 	Use the {@link JsonParser#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link JsonParser}.
 * <ul>
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonParserProperties implements Cloneable {

	/**
	 * Set strict mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When in strict mode, parser throws exceptions on the following invalid JSON syntax:
	 * <ul>
	 * 	<li>Unquoted attributes.
	 * 	<li>Missing attribute values.
	 * 	<li>Concatenated strings.
	 * 	<li>Javascript comments.
	 * 	<li>Numbers and booleans when Strings are expected.
	 * </ul>
	 */
	public static final String JSON_strictMode = "JsonParser.strictMode";

	private boolean
		strictMode = false;

	/**
	 * Sets the specified property value.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 * @throws LockedException If the bean context has been locked.
	 */
	protected boolean setProperty(String property, Object value) throws LockedException {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(JSON_strictMode))
			strictMode = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}

	/**
	 * Returns the current {@link #JSON_strictMode} value.
	 *
	 * @return The current {@link #JSON_strictMode} value.
	 */
	public boolean isStrictMode() {
		return strictMode;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Object */
	public JsonParserProperties clone() {
		try {
			return (JsonParserProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
