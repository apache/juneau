/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.dto.jsonschema;

import java.net.*;

/**
 * Convenience class for representing a schema reference such as <js>"{'$ref':'/url/to/ref'}"</js>.
 * <p>
 * 	An instance of this object is equivalent to calling...
 *
 * <p class='bcode'>
 * 	Schema s = <jk>new</jk> Schema().setRef(uri);
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class SchemaRef extends Schema {

	/**
	 * Constructor.
	 *
	 * @param uri The URI of the target reference.  Can be <jk>null</jk>.
	 */
	public SchemaRef(String uri) {
		this.setRef(uri == null ? null : URI.create(uri));
	}
}
