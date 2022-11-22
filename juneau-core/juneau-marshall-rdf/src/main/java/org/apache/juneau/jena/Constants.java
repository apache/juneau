// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.jena;

/**
 * Constants used by the {@link RdfSerializer} and {@link RdfParser} classes.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
public final class Constants {

	//-----------------------------------------------------------------------------------------------------------------
	// Built-in Jena languages.
	//-----------------------------------------------------------------------------------------------------------------

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


	//-----------------------------------------------------------------------------------------------------------------
	// Built-in Juneau properties.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RDF property identifier <js>"items"</js>.
	 *
	 * <p>
	 * For resources that are collections, this property identifies the RDF Sequence container for the items in the
	 * collection.
	 */
	public static final String RDF_juneauNs_ITEMS = "items";

	/**
	 * RDF property identifier <js>"root"</js>.
	 *
	 * <p>
	 * Property added to root nodes to help identify them as root elements during parsing.
	 *
	 * <p>
	 * Added if {@link RdfSerializer.Builder#addRootProperty()} setting is enabled.
	 */
	public static final String RDF_juneauNs_ROOT = "root";

	/**
	 * RDF property identifier <js>"class"</js>.
	 *
	 * <p>
	 * Property added to bean resources to identify the class type.
	 *
	 * <p>
	 * Added if {@link org.apache.juneau.serializer.Serializer.Builder#addBeanTypes()} setting is enabled.
	 */
	public static final String RDF_juneauNs_TYPE = "_type";

	/**
	 * RDF property identifier <js>"value"</js>.
	 *
	 * <p>
	 * Property added to nodes to identify a simple value.
	 */
	public static final String RDF_juneauNs_VALUE = "value";

	/**
	 * RDF resource that identifies a <jk>null</jk> value.
	 */
	public static final String RDF_NIL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";

	/**
	 * RDF resource that identifies a <c>Seq</c> value.
	 */
	public static final String RDF_SEQ = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Seq";

	/**
	 * RDF resource that identifies a <c>Bag</c> value.
	 */
	public static final String RDF_BAG = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag";
}
