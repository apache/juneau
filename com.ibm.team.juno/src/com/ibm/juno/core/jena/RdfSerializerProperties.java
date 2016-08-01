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
import com.ibm.juno.core.serializer.*;

/**
 * Configurable properties on the {@link RdfSerializer} class.
 * <p>
 * 	Use the {@link RdfSerializer#setProperty(String, Object)} method to set property values.
 * <p>
 * 	In addition to these properties, the following properties are also applicable for {@link RdfSerializer}.
 * <ul>
 * 	<li>{@link RdfProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RdfSerializerProperties extends RdfProperties implements Cloneable {

	/**
	 * Add XSI data types to non-<code>String</code> literals ({@link Boolean}, default=<jk>false</jk>).
	 */
	public static final String RDF_addLiteralTypes = "RdfSerializer.addLiteralTypes";

	/**
	 * Add RDF root identifier property to root node ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * 	When enabled an RDF property <code>http://www.ibm.com/juno/root</code> is added with a value of <js>"true"</js>
	 * 		to identify the root node in the graph.
	 * 	This helps locate the root node during parsing.
	 * <p>
	 * 	If disabled, the parser has to search through the model to find any resources without
	 * 		incoming predicates to identify root notes, which can introduce a considerable performance
	 * 		degradation.
	 */
	public static final String RDF_addRootProperty = "RdfSerializer.addRootProperty";

	boolean addLiteralTypes = false, addRootProperty = false;

	/**
	 * Sets the specified property value.
	 * @param property The property name.
	 * @param value The property value.
	 * @return <jk>true</jk> if property name was valid and property was set.
	 */
	@Override /* RdfProperties */
	public boolean setProperty(String property, Object value) {
		if (property.equals(RDF_addLiteralTypes))
			addLiteralTypes = BeanContext.DEFAULT.convertToType(value, boolean.class);
		else if (property.equals(RDF_addRootProperty))
			addRootProperty = BeanContext.DEFAULT.convertToType(value, boolean.class);
		else
			return super.setProperty(property, value);
		return true;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* Cloneable */
	public RdfSerializerProperties clone() {
		try {
			return (RdfSerializerProperties)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Shouldn't happen.
		}
	}
}
