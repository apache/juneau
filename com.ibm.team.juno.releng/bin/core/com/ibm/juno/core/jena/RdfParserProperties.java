/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Configurable properties on the {@link RdfParser} class.
 * <p>
 * 	Use the {@link RdfParser#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link RdfParser}.
 * <ul>
 * 	<li>{@link RdfProperties}
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RdfParserProperties extends RdfProperties implements Cloneable {


	/**
	 * Trim whitespace from text elements ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 */
	public static final String RDF_trimWhitespace = "RdfParser.trimWhitespace";

	boolean trimWhitespace = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	@Override /* RdfProperties */
	public boolean setProperty(String property, Object value) {
		if (property.equals(RDF_trimWhitespace))
			trimWhitespace = BeanContext.DEFAULT.convertToType(value, boolean.class);
		else
			return super.setProperty(property, value);
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public RdfParserProperties clone() {
		try {
			return (RdfParserProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
