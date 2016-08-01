/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.html;

import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;

/**
 * Configurable properties on the {@link HtmlParser} class.
 * <p>
 * 	Use the {@link HtmlParser#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link HtmlParser}.
 * <ul>
 * 	<li>{@link ParserProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class HtmlParserProperties implements Cloneable {

	/**
	 * Sets the specified property value.
	 *
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	protected boolean setProperty(String property, Object value) {
		return false;
	}

	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Lockable */
	public HtmlParserProperties clone() {
		try {
			return (HtmlParserProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
