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
import com.ibm.juno.core.serializer.*;

/**
 * Configurable properties on the {@link JsonSerializer} class.
 * <p>
 * 	Use the {@link JsonSerializer#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link JsonSerializer}.
 * <ul>
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class JsonSerializerProperties implements Cloneable {

	/**
	 * Simple JSON mode ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, JSON attribute names will only be quoted when necessary.
	 * Otherwise, they are always quoted.
	 */
	public static final String JSON_simpleMode = "JsonSerializer.simpleMode";

	/**
	 * Use whitespace in output ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 */
	public static final String JSON_useWhitespace = "JsonSerializer.useWhitespace";

	/**
	 * Prefix solidus <js>'/'</js> characters with escapes ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, solidus (e.g. slash) characters should be escaped.
	 * The JSON specification allows for either format.
	 * However, if you're embedding JSON in an HTML script tag, this setting prevents
	 * 	confusion when trying to serialize <xt>&lt;\/script&gt;</xt>.
	 */
	public static final String JSON_escapeSolidus = "JsonSerializer.escapeSolidus";

	boolean
		simpleMode = false,
		useWhitespace = false,
		escapeSolidus = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(JSON_simpleMode))
			simpleMode = bc.convertToType(value, Boolean.class);
		else if (property.equals(JSON_useWhitespace))
			useWhitespace = bc.convertToType(value, Boolean.class);
		else if (property.equals(JSON_escapeSolidus))
			escapeSolidus = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public JsonSerializerProperties clone() {
		try {
			return (JsonSerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
