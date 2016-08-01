/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import com.ibm.juno.core.serializer.*;

/**
 * Constants used by the {@link RdfSerializer} and {@link RdfParser} classes.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class Constants {

	//--------------------------------------------------------------------------------
	// Built-in Jena languages.
	//--------------------------------------------------------------------------------

	/** Jena language support: <js>"RDF/XML"</js>.*/
	public static final String LANG_RDF_XML = "RDF/XML";

	/** Jena language support: <js>"RDF/XML-ABBREV"</js>.*/
	public static final String LANG_RDF_XML_ABBREV = "RDF/XML-ABBREV";

	/** Jena language support: <js>"N-TRIPLE"</js>.*/
	public static final String LANG_NTRIPLE = "N-TRIPLE";

	/** Jena language support: <js>"TURTLE"</js>.*/
	public static final String LANG_TURTLE = "TURTLE";

	/** Jena language support: <js>"N3"</js>.*/
	public static final String LANG_N3 = "N3";


	//--------------------------------------------------------------------------------
	// Built-in Juno properties.
	//--------------------------------------------------------------------------------

	/**
	 * RDF property identifier <js>"items"</js>.
	 * <p>
	 * For resources that are collections, this property identifies the RDF Sequence
	 * 	container for the items in the collection.
	 */
	public static final String RDF_junoNs_ITEMS = "items";

	/**
	 * RDF property identifier <js>"root"</js>.
	 * <p>
	 * Property added to root nodes to help identify them as root elements during parsing.
	 * <p>
	 * Added if {@link RdfSerializerProperties#RDF_addRootProperty} setting is enabled.
	 */
	public static final String RDF_junoNs_ROOT = "root";

	/**
	 * RDF property identifier <js>"class"</js>.
	 * <p>
	 * Property added to bean resources to identify the class type.
	 * <p>
	 * Added if {@link SerializerProperties#SERIALIZER_addClassAttrs} setting is enabled.
	 */
	public static final String RDF_junoNs_CLASS = "class";

	/**
	 * RDF property identifier <js>"value"</js>.
	 * <p>
	 * Property added to nodes to identify a simple value.
	 */
	public static final String RDF_junoNs_VALUE = "value";

	/**
	 * RDF resource that identifies a <jk>null</jk> value.
	 */
	public static final String RDF_NIL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";

	/**
	 * RDF resource that identifies a <code>Seq</code> value.
	 */
	public static final String RDF_SEQ = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq";

	/**
	 * RDF resource that identifies a <code>Bag</code> value.
	 */
	public static final String RDF_BAG = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag";
}
