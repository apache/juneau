/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.annotation;

/**
 * Inheritance values for the {@link RestMethod#serializersInherit()} and {@link RestMethod#parsersInherit()}
 * 	annotations.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum Inherit {

	/** Inherit serializers from parent. */
	SERIALIZERS,

	/** Inherit parsers from parent. */
	PARSERS,

	/** Inherit filters from parent. */
	FILTERS,

	/** Inherit properties from parent. */
	PROPERTIES
}
