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

import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Session object that lives for the duration of a single use of {@link RdfSerializer}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public final class RdfSerializerSession extends SerializerSession {

	private final String rdfLanguage;
	private final Namespace juneauNs, juneauBpNs;
	private final boolean addLiteralTypes, addRootProperty, useXmlNamespaces, looseCollection, autoDetectNamespaces;
	private final Property pRoot, pValue, pClass;
	private final Model model;
	private final RDFWriter writer;
	private final RdfCollectionFormat collectionFormat;
	private final Namespace[] namespaces;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param output The output object.  See {@link JsonSerializerSession#getWriter()} for valid class types.
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 */
	protected RdfSerializerSession(RdfSerializerContext ctx, BeanContext beanContext, Object output, ObjectMap op, Method javaMethod) {
		super(ctx, beanContext, output, op, javaMethod);
		ObjectMap jenaSettings = new ObjectMap();
		jenaSettings.put("rdfXml.tab", isUseIndentation() ? 2 : 0);
		jenaSettings.put("rdfXml.attributeQuoteChar", Character.toString(getQuoteChar()));
		jenaSettings.putAll(ctx.jenaSettings);
		if (op == null || op.isEmpty()) {
			this.rdfLanguage = ctx.rdfLanguage;
			this.juneauNs = ctx.juneauNs;
			this.juneauBpNs = ctx.juneauBpNs;
			this.addLiteralTypes = ctx.addLiteralTypes;
			this.addRootProperty = ctx.addRootProperty;
			this.collectionFormat = ctx.collectionFormat;
			this.looseCollection = ctx.looseCollection;
			this.useXmlNamespaces = ctx.useXmlNamespaces;
			this.autoDetectNamespaces = ctx.autoDetectNamespaces;
			this.namespaces = ctx.namespaces;
		} else {
			this.rdfLanguage = op.getString(RDF_language, ctx.rdfLanguage);
			this.juneauNs = (op.containsKey(RDF_juneauNs) ? NamespaceFactory.parseNamespace(op.get(RDF_juneauNs)) : ctx.juneauNs);
			this.juneauBpNs = (op.containsKey(RDF_juneauBpNs) ? NamespaceFactory.parseNamespace(op.get(RDF_juneauBpNs)) : ctx.juneauBpNs);
			this.addLiteralTypes = op.getBoolean(RDF_addLiteralTypes, ctx.addLiteralTypes);
			this.addRootProperty = op.getBoolean(RDF_addRootProperty, ctx.addRootProperty);
			for (Map.Entry<String,Object> e : op.entrySet()) {
				String key = e.getKey();
				if (key.startsWith("Rdf.jena."))
					jenaSettings.put(key.substring(9), e.getValue());
			}
			this.collectionFormat = RdfCollectionFormat.valueOf(op.getString(RDF_collectionFormat, "DEFAULT"));
			this.looseCollection = op.getBoolean(RDF_looseCollection, ctx.looseCollection);
			this.useXmlNamespaces = op.getBoolean(RDF_useXmlNamespaces, ctx.useXmlNamespaces);
			this.autoDetectNamespaces = op.getBoolean(RDF_autoDetectNamespaces, ctx.autoDetectNamespaces);
			this.namespaces = op.get(Namespace[].class, RDF_namespaces, ctx.namespaces);
		}
		this.model = ModelFactory.createDefaultModel();
		addModelPrefix(juneauNs);
		addModelPrefix(juneauBpNs);
		for (Namespace ns : this.namespaces)
			addModelPrefix(ns);
		this.pRoot = model.createProperty(juneauNs.getUri(), RDF_juneauNs_ROOT);
		this.pValue = model.createProperty(juneauNs.getUri(), RDF_juneauNs_VALUE);
		this.pClass = model.createProperty(juneauNs.getUri(), RDF_juneauNs_CLASS);
		writer = model.getWriter(rdfLanguage);

		// Only apply properties with this prefix!
		String propPrefix = RdfCommonContext.LANG_PROP_MAP.get(rdfLanguage);
		if (propPrefix == null)
			throw new RuntimeException("Unknown RDF language encountered: '"+rdfLanguage+"'");

		for (Map.Entry<String,Object> e : jenaSettings.entrySet())
			if (e.getKey().startsWith(propPrefix))
				writer.setProperty(e.getKey().substring(propPrefix.length()), e.getValue());
	}

	/**
	 * Adds the specified namespace as a model prefix.
	 *
	 * @param ns The XML namespace.
	 */
	public void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_collectionFormat} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_collectionFormat} setting value for this session.
	 */
	public final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_useXmlNamespaces} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_useXmlNamespaces} setting value for this session.
	 */
	public final boolean isUseXmlNamespaces() {
		return useXmlNamespaces;
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_looseCollection} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_looseCollection} setting value for this session.
	 */
	public final boolean isLooseCollection() {
		return looseCollection;
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_language} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_language} setting value for this session.
	 */
	public final String getRdfLanguage() {
		return rdfLanguage;
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_juneauNs} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_juneauNs} setting value for this session.
	 */
	public final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * Returns the {@link RdfCommonContext#RDF_juneauBpNs} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_juneauBpNs} setting value for this session.
	 */
	public final Namespace getJuneauBpNs() {
		return juneauBpNs;
	}

	/**
	 * Returns the {@link RdfSerializerContext#RDF_addLiteralTypes} setting value for this session.
	 *
	 * @return The {@link RdfSerializerContext#RDF_addLiteralTypes} setting value for this session.
	 */
	public final boolean isAddLiteralTypes() {
		return addLiteralTypes;
	}

	/**
	 * Returns the {@link RdfSerializerContext#RDF_addRootProperty} setting value for this session.
	 *
	 * @return The {@link RdfSerializerContext#RDF_addRootProperty} setting value for this session.
	 */
	public final boolean isAddRootProperty() {
		return addRootProperty;
	}

	/**
	 * Returns the {@link RdfSerializerContext#RDF_autoDetectNamespaces} setting value for this session.
	 *
	 * @return The {@link RdfSerializerContext#RDF_autoDetectNamespaces} setting value for this session.
	 */
	public final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Returns the RDF property that identifies the root node in the RDF model.
	 *
	 * @return The RDF property that identifies the root node in the RDF model.
	 */
	public final Property getRootProperty() {
		return pRoot;
	}

	/**
	 * Returns the RDF property that represents a value in the RDF model.
	 *
	 * @return The RDF property that represents a value in the RDF model.
	 */
	public final Property getValueProperty() {
		return pValue;
	}

	/**
	 * Returns the RDF property that represents a class in the RDF model.
	 *
	 * @return The RDF property that represents a class in the RDF model.
	 */
	public final Property getClassProperty() {
		return pClass;
	}

	/**
	 * Returns the RDF model being serialized.
	 *
	 * @return The RDF model being serialized.
	 */
	public final Model getModel() {
		return model;
	}

	/**
	 * Returns the RDF writer that's being serialized to.
	 *
	 * @return The RDF writer that's being serialized to.
	 */
	public final RDFWriter getRdfWriter() {
		return writer;
	}

	/**
	 * XML-encodes the specified string using the {@link XmlUtils#encodeTextInvalidChars(Object)} method.
	 *
	 * @param o The string being encoded.
	 * @return The encoded string, or <jk>null</jk> if the input was <jk>null</jk>.
	 */
	public final String encodeTextInvalidChars(Object o) {
		if (o == null)
			return null;
		String s = toString(o);
		return XmlUtils.encodeTextInvalidChars(s);
	}

	/**
	 * XML-encoded the specified element name using the {@link XmlUtils#encodeElementName(Object)} method.
	 *
	 * @param o The string being encoded.
	 * @return The encoded string.
	 */
	public final String encodeElementName(Object o) {
		String s = toString(o);
		return XmlUtils.encodeElementName(s);
	}
}
