/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import com.ibm.juno.core.jena.annotation.*;

/**
 * Used in conjunction with the {@link Rdf#collectionFormat() @Rdf.collectionFormat()} annotation to fine-tune how
 * 	classes, beans, and bean properties are serialized, particularly collections.
 * <p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public enum RdfCollectionFormat {

	/**
	 * Default formatting (default).
	 * <p>
	 * Inherit formatting from parent class or parent package.
	 *	If no formatting specified at any level, default is {@link #SEQ}.
	 */
	DEFAULT,

	/**
	 * Causes collections and arrays to be rendered as RDF sequences.
	 */
	SEQ,

	/**
	 * Causes collections and arrays to be rendered as RDF bags.
	 */
	BAG,

	/**
	 * Causes collections and arrays to be rendered as RDF lists.
	 */
	LIST,

	/**
	 * Causes collections and arrays to be rendered as multi-valued RDF properties instead of sequences.
	 * <p>
	 * 	Note that enabling this setting will cause order of elements in the collection to be lost.
	 */
	MULTI_VALUED;

}