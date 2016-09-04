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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Configurable properties on the {@link RdfParser} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link RdfParser#setProperty(String,Object)}
 * 	<li>{@link RdfParser#setProperties(ObjectMap)}
 * 	<li>{@link RdfParser#addNotBeanClasses(Class[])}
 * 	<li>{@link RdfParser#addBeanFilters(Class[])}
 * 	<li>{@link RdfParser#addPojoSwaps(Class[])}
 * 	<li>{@link RdfParser#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class RdfParserContext extends ParserContext implements RdfCommonContext {

	/**
	 * Trim whitespace from text elements ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 */
	public static final String RDF_trimWhitespace = "RdfParser.trimWhitespace";

	final boolean trimWhitespace, looseCollection;
	final String rdfLanguage;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings = new HashMap<String,Object>();

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public RdfParserContext(ContextFactory cf) {
		super(cf);
		trimWhitespace = cf.getProperty(RDF_trimWhitespace, boolean.class, false);
		looseCollection = cf.getProperty(RDF_looseCollection, boolean.class, false);
		rdfLanguage = cf.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = cf.getProperty(RDF_juneauNs, Namespace.class, new Namespace("j", "http://www.ibm.com/juneau/"));
		juneauBpNs = cf.getProperty(RDF_juneauBpNs, Namespace.class, new Namespace("j", "http://www.ibm.com/juneaubp/"));
		collectionFormat = cf.getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
	}
}
