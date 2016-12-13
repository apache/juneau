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
 * 	<li>{@link RdfSerializer#addBeanFilters(Class[])}
 * 	<li>{@link RdfSerializer#addPojoSwaps(Class[])}
 * 	<li>{@link RdfSerializer#addToDictionary(Class[])}
 * 	<li>{@link RdfSerializer#addImplClass(Class,Class)}
 * </ul>
 * <p>
 * See {@link ContextFactory} for more information about context properties.
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties on the RDF serializers</h6>
 * <table class='styled' style='border-collapse: collapse;'>
 * 	<tr><th>Setting name</th><th>Description</th><th>Data type</th><th>Default value</th></tr>
 * 	<tr>
 * 		<td>{@link #RDF_addLiteralTypes}</td>
 * 		<td>Add XSI data types to non-<code>String</code> literals.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #RDF_addRootProperty}</td>
 * 		<td>Add RDF root identifier property to root node.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>false</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #RDF_autoDetectNamespaces}</td>
 * 		<td>Auto-detect namespace usage.</td>
 * 		<td><code>Boolean</code></td>
 * 		<td><jk>true</jk></td>
 * 	</tr>
 * 	<tr>
 * 		<td>{@link #RDF_namespaces}</td>
 * 		<td>Default namespaces.</td>
 * 		<td><code>List&lt;{@link Namespace}&gt;</code></td>
 * 		<td>empty list</td>
 * 	</tr>
 * </table>
 *
 * <h6 class='topic' id='ConfigProperties'>Configurable properties inherited by the RDF serializers</h6>
 * <ul class='javahierarchy'>
 * 	<li class='c'><a class='doclink' href='../BeanContext.html#ConfigProperties'>BeanContext</a> - Properties associated with handling beans on serializers and parsers.
 * 	<ul>
 * 		<li class='c'><a class='doclink' href='../serializer/SerializerContext.html#ConfigProperties'>SerializerContext</a> - Configurable properties common to all serializers.
 * 		<ul>
 * 			<li class='c'><a class='doclink' href='RdfCommonContext.html#ConfigProperties'>RdfCommonContext</a> - Configurable properties common to the RDF serializers and parsers.
 * 		</ul>
 * 	</ul>
 * </ul>
 */
public final class RdfSerializerContext extends SerializerContext implements RdfCommonContext {

	/**
	 * <b>Configuration property:</b>  Add XSI data types to non-<code>String</code> literals.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addLiteralTypes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 */
	public static final String RDF_addLiteralTypes = "RdfSerializer.addLiteralTypes";

	/**
	 * <b>Configuration property:</b>  Add RDF root identifier property to root node.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addRootProperty"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * </ul>
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without
	 * 	incoming predicates to identify root notes, which can introduce a considerable performance
	 * 	degradation.
	 */
	public static final String RDF_addRootProperty = "RdfSerializer.addRootProperty";

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * </ul>
	 * <p>
	 * Detect namespace usage before serialization.
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for
	 * namespaces that will be encountered before the root element is
	 * serialized.
	 */
	public static final String RDF_autoDetectNamespaces = "RdfSerializer.autoDetectNamespaces";

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * <p>
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.namespaces.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * </ul>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String RDF_namespaces = "RdfSerializer.namespaces.list";


	final boolean addLiteralTypes, addRootProperty, useXmlNamespaces, looseCollections, autoDetectNamespaces;
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
		looseCollections = cf.getProperty(RDF_looseCollections, boolean.class, false);
		autoDetectNamespaces = cf.getProperty(RDF_autoDetectNamespaces, boolean.class, true);
		rdfLanguage = cf.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = cf.getProperty(RDF_juneauNs, Namespace.class, new Namespace("j", "http://www.apache.org/juneau/"));
		juneauBpNs = cf.getProperty(RDF_juneauBpNs, Namespace.class, new Namespace("jp", "http://www.apache.org/juneaubp/"));
		collectionFormat = cf.getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
		namespaces = cf.getProperty(RDF_namespaces, Namespace[].class, new Namespace[0]);
	}
}
