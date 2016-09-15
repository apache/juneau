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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Session object that lives for the duration of a single use of {@link RdfParser}.
 * <p>
 * This class is NOT thread safe.  It is meant to be discarded after one-time use.
 *
 * @author James Bognar (james.bognar@salesforce.com)
 */
public class RdfParserSession extends ParserSession {

	private final String rdfLanguage;
	private final Namespace juneauNs, juneauBpNs;
	private final Property pRoot, pValue, pClass, pType;
	private final Model model;
	private final boolean trimWhitespace, looseCollections;
	private final RDFReader rdfReader;
	private final Set<Resource> urisVisited = new HashSet<Resource>();
	private final RdfCollectionFormat collectionFormat;

	/**
	 * Create a new session using properties specified in the context.
	 *
	 * @param ctx The context creating this session object.
	 * 	The context contains all the configuration settings for this object.
	 * @param beanContext The bean context being used.
	 * @param input The input.  Can be any of the following types:
	 * 	<ul>
	 * 		<li><jk>null</jk>
	 * 		<li>{@link Reader}
	 * 		<li>{@link CharSequence}
	 * 		<li>{@link InputStream} containing UTF-8 encoded text.
	 * 		<li>{@link File} containing system encoded text.
	 * 	</ul>
	 * @param op The override properties.
	 * 	These override any context properties defined in the context.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	protected RdfParserSession(RdfParserContext ctx, BeanContext beanContext, Object input, ObjectMap op, Method javaMethod, Object outer) {
		super(ctx, beanContext, input, op, javaMethod, outer);
		ObjectMap jenaSettings = new ObjectMap();
		jenaSettings.putAll(ctx.jenaSettings);
		if (op == null || op.isEmpty()) {
			this.rdfLanguage = ctx.rdfLanguage;
			this.juneauNs = ctx.juneauNs;
			this.juneauBpNs = ctx.juneauBpNs;
			this.trimWhitespace = ctx.trimWhitespace;
			this.collectionFormat = ctx.collectionFormat;
			this.looseCollections = ctx.looseCollections;
		} else {
			this.rdfLanguage = op.getString(RDF_language, ctx.rdfLanguage);
			this.juneauNs = (op.containsKey(RDF_juneauNs) ? NamespaceFactory.parseNamespace(op.get(RDF_juneauNs)) : ctx.juneauNs);
			this.juneauBpNs = (op.containsKey(RDF_juneauBpNs) ? NamespaceFactory.parseNamespace(op.get(RDF_juneauBpNs)) : ctx.juneauBpNs);
			this.trimWhitespace = op.getBoolean(RdfParserContext.RDF_trimWhitespace, ctx.trimWhitespace);
			this.collectionFormat = RdfCollectionFormat.valueOf(op.getString(RDF_collectionFormat, "DEFAULT"));
			this.looseCollections = op.getBoolean(RDF_looseCollections, ctx.looseCollections);
		}
		this.model = ModelFactory.createDefaultModel();
		addModelPrefix(juneauNs);
		addModelPrefix(juneauBpNs);
		this.pRoot = model.createProperty(juneauNs.getUri(), RDF_juneauNs_ROOT);
		this.pValue = model.createProperty(juneauNs.getUri(), RDF_juneauNs_VALUE);
		this.pClass = model.createProperty(juneauNs.getUri(), RDF_juneauNs_CLASS);
		this.pType = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		rdfReader = model.getReader(rdfLanguage);

		// Note: NTripleReader throws an exception if you try to set any properties on it.
		if (! rdfLanguage.equals(LANG_NTRIPLE)) {
			for (Map.Entry<String,Object> e : jenaSettings.entrySet())
				rdfReader.setProperty(e.getKey(), e.getValue());
		}
	}

	/**
	 * Returns <jk>true</jk> if this resource was already visited.
	 *
	 * @param r The resource to check.
	 * @return <jk>true</jk> if this resource was already visited.
	 */
	public final boolean wasAlreadyProcessed(Resource r) {
		return ! urisVisited.add(r);
	}

	/**
	 * Returns the root property.
	 *
	 * @return The root property.
	 */
	public final Property getRootProperty() {
		return pRoot;
	}

	/**
	 * Returns the RDF property identifier <js>"value"</js>.
	 *
	 * @return The RDF property identifier <js>"value"</js>.
	 */
	public final Property getValueProperty() {
		return pValue;
	}

	/**
	 * Returns the RDF property identifier <js>"class"</js>.
	 *
	 * @return The RDF property identifier <js>"class"</js>.
	 */
	public final Property getClassProperty() {
		return pClass;
	}

	/**
	 * Returns the RDF property identifier <js>"type"</js>.
	 *
	 * @return The RDF property identifier <js>"type"</js>.
	 */
	public final Property getTypeProperty() {
		return pType;
	}

	/**
	 * Returns the RDF model being parsed into.
	 *
	 * @return The RDF model being parsed into.
	 */
	public final Model getModel() {
		return model;
	}

	/**
	 * Returns the RDF reader that's reading the model.
	 *
	 * @return The RDF reader that's reading the model.
	 */
	public final RDFReader getRdfReader() {
		return rdfReader;
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
	 * Returns the {@link RdfCommonContext#RDF_looseCollections} setting value for this session.
	 *
	 * @return The {@link RdfCommonContext#RDF_looseCollections} setting value for this session.
	 */
	public final boolean isLooseCollections() {
		return looseCollections;
	}

	/**
	 * Returns the Juneau namespace URI.
	 *
	 * @return The Juneau namespace URI.
	 */
	public final String getJuneauNsUri() {
		return juneauNs.getUri();
	}

	/**
	 * Adds the specified namespace as a model prefix.
	 *
	 * @param ns The XML namespace.
	 */
	public final void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/**
	 * Constructs a <code>Property</code> in the Juneau Bean namespace in this mode.
	 *
	 * @param name The property name.
	 * @return The new property object.
	 */
	public final Property getProperty(String name) {
		return model.createProperty(juneauBpNs.getUri(), name);
	}

	/**
	 * Decodes the specified string.
	 * <p>
	 * If {@link RdfParserContext#RDF_trimWhitespace} is <jk>true</jk>, the resulting string is trimmed before decoding.
	 * <p>
	 * If {@link #isTrimStrings()} is <jk>true</jk>, the resulting string is trimmed after decoding.
	 *
	 * @param o The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	public final String decodeString(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (s.isEmpty())
			return s;
		if (trimWhitespace)
			s = s.trim();
		s = XmlUtils.decode(s);
		if (isTrimStrings())
			s = s.trim();
		return s;
	}
}
