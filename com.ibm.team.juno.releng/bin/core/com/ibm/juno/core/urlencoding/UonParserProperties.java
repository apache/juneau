/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.urlencoding;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Configurable properties on the {@link UonParser} and {@link UrlEncodingParser} classes.
 * <p>
 * 	Use the {@link UonParser#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link UonParser}.
 * <ul>
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class UonParserProperties implements Cloneable {

	/**
	 * Decode <js>"%xx"</js> sequences. ({@link Boolean}, default=<jk>false</jk> for {@link UonParser}, <jk>true</jk> for {@link UrlEncodingParser}).
	 * <p>
	 * Specify <jk>true</jk> if URI encoded characters should be decoded, <jk>false</jk>
	 * 	if they've already been decoded before being passed to this parser.
	 */
	public static final String UON_decodeChars = "UonParser.decodeChars";

	/**
	 * Expect input to contain readable whitespace characters from using the {@link UonSerializerProperties#UON_useWhitespace} setting ({@link Boolean}, default=<jk>false</jk>).
	 */
	public static final String UON_whitespaceAware = "UonParser.whitespaceAware";

	boolean
		decodeChars = false,
		whitespaceAware = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	public boolean setProperty(String property, Object value) {
		BeanContext bc = BeanContext.DEFAULT;
		if (property.equals(UON_decodeChars))
			decodeChars = bc.convertToType(value, Boolean.class);
		else if (property.equals(UON_whitespaceAware))
			whitespaceAware = bc.convertToType(value, Boolean.class);
		else
			return false;
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public UonParserProperties clone() {
		try {
			return (UonParserProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen
		}
	}
}
