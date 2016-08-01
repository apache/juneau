/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.jena;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Configurable properties on the {@link RdfSerializer} class.
 * <p>
 * Context properties are set by calling {@link ContextFactory#setProperty(String, Object)} on the context factory
 * returned {@link CoreApi#getContextFactory()}.
 * <p>
 * The following convenience methods are also provided for setting context properties:
 * <ul>
 * 	<li>{@link RdfSerializer#setProperty(String,Object)}
 * 	<li>{@link RdfSerializer#setProperties(ObjectMap)}
 * 	<li>{@link RdfSerializer#addNotBeanClasses(Class[])}
 * 	<li>{@link RdfSerializer#addTransforms(Class[])}
 * 	<li>{@link RdfSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class RdfSerializerContext extends SerializerContext implements RdfCommonContext {

	/**
	 * Add XSI data types to non-<code>String</code> literals ({@link Boolean}, default=<jk>false</jk>).
	 */
	public static final String RDF_addLiteralTypes = "RdfSerializer.addLiteralTypes";

	/**
	 * Add RDF root identifier property to root node ({@link Boolean}, default=<jk>false</jk>).
	 * <p>
	 * When enabled an RDF property <code>http://www.ibm.com/juneau/root</code> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without
	 * 	incoming predicates to identify root notes, which can introduce a considerable performance
	 * 	degradation.
	 */
	public static final String RDF_addRootProperty = "RdfSerializer.addRootProperty";

	/**
	 * Auto-detect namespace usage ({@link Boolean}, default=<jk>true</jk>).
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 */
	public static final String RDF_autoDetectNamespaces = "RdfSerializer.autoDetectNamespaces";

	/**
	 * Default namespaces (<code>List&lt;Namespace&gt;</code>, default=<code>Namespace[0]</code>).
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String RDF_namespaces = "RdfSerializer.namespaces.list";


	final boolean addLiteralTypes, addRootProperty, useXmlNamespaces, looseCollection, autoDetectNamespaces;
	final String rdfLanguage;
	final Namespace juneauNs;
	final Namespace juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings = new HashMap<String,Object>();
	final Namespace[] namespaces;

	/**
	 * Constructor.
	 * <p>
	 * Typically only called from {@link ContextFactory#getContext(Class)}.
	 *
	 * @param cf The factory that created this context.
	 */
	public RdfSerializerContext(ContextFactory cf) {
		super(cf);
		addLiteralTypes = cf.getProperty(RDF_addLiteralTypes, boolean.class, false);
		addRootProperty = cf.getProperty(RDF_addRootProperty, boolean.class, false);
		useXmlNamespaces = cf.getProperty(RDF_useXmlNamespaces, boolean.class, true);
		looseCollection = cf.getProperty(RDF_looseCollection, boolean.class, false);
		autoDetectNamespaces = cf.getProperty(RDF_autoDetectNamespaces, boolean.class, true);
		rdfLanguage = cf.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = cf.getProperty(RDF_juneauNs, Namespace.class, new Namespace("j", "http://www.ibm.com/juneau/"));
		juneauBpNs = cf.getProperty(RDF_juneauBpNs, Namespace.class, new Namespace("jp", "http://www.ibm.com/juneaubp/"));
		collectionFormat = cf.getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
		namespaces = cf.getProperty(RDF_namespaces, Namespace[].class, new Namespace[0]);
	}
}
