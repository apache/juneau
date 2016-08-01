/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.jsonschema;

/**
 * Convenience class for representing a property that's an array of simple types.
 * <p>
 * 	An instance of this object is equivalent to calling...
 *
 * <p class='bcode'>
 * 	SchemaProperty p = <jk>new</jk> SchemaProperty(name)
 * 		.setType(JsonType.<jsf>ARRAY</jsf>)
 * 		.setItems(
 * 			<jk>new</jk> Schema().setType(elementType)
 * 		);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SchemaPropertySimpleArray extends SchemaProperty {

	/**
	 * Constructor.
	 *
	 * @param name The name of the schema property.
	 * @param elementType The JSON type of the elements in the array.
	 */
	public SchemaPropertySimpleArray(String name, JsonType elementType) {
		setName(name);
		setType(JsonType.ARRAY);
		setItems(
			new Schema().setType(elementType)
		);
	}
}
