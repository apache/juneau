/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena;

import static com.ibm.juno.core.jena.Constants.*;
import static com.ibm.juno.core.jena.RdfProperties.*;

import java.lang.reflect.*;
import java.util.*;

import com.hp.hpl.jena.rdf.model.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.xml.*;

/**
 * Context object that lives for the duration of a single parse of {@link RdfParser}.
 * <p>
 * 	See {@link ParserContext} for details.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class RdfParserContext extends ParserContext {

	final String rdfLanguage;
	final Namespace junoNs, junoBpNs;
	final Property pRoot, pValue, pClass, pType;
	final Model model;
	final boolean trimWhitespace, looseCollection;
	final RDFReader rdfReader;
	final Set<Resource> urisVisited = new HashSet<Resource>();
	final RdfCollectionFormat collectionFormat;

	/**
	 * Constructor.
	 *
	 * @param beanContext The bean context being used by the parser.
	 * @param pp Default general parser properties.
	 * @param rpp Default Jena parser properties.
	 * @param op Override properties.
	 * @param javaMethod The java method that called this parser, usually the method in a REST servlet.
	 * @param outer The outer object for instantiating top-level non-static inner classes.
	 */
	protected RdfParserContext(BeanContext beanContext, ParserProperties pp, RdfParserProperties rpp, ObjectMap op, Method javaMethod, Object outer) {
		super(beanContext, pp, op, javaMethod, outer);
		ObjectMap jenaSettings = new ObjectMap();
		jenaSettings.putAll(rpp.jenaSettings);
		if (op == null || op.isEmpty()) {
			this.rdfLanguage = rpp.rdfLanguage;
			this.junoNs = rpp.junoNs;
			this.junoBpNs = rpp.junoBpNs;
			this.trimWhitespace = rpp.trimWhitespace;
			this.collectionFormat = rpp.collectionFormat;
			this.looseCollection = rpp.looseCollection;
		} else {
			this.rdfLanguage = op.getString(RDF_language, rpp.rdfLanguage);
			this.junoNs = (op.containsKey(RDF_junoNs) ? NamespaceFactory.parseNamespace(op.get(RDF_junoNs)) : rpp.junoNs);
			this.junoBpNs = (op.containsKey(RDF_junoBpNs) ? NamespaceFactory.parseNamespace(op.get(RDF_junoBpNs)) : rpp.junoBpNs);
			this.trimWhitespace = op.getBoolean(RdfParserProperties.RDF_trimWhitespace, rpp.trimWhitespace);
			this.collectionFormat = RdfCollectionFormat.valueOf(op.getString(RDF_collectionFormat, "DEFAULT"));
			this.looseCollection = op.getBoolean(RDF_looseCollection, rpp.looseCollection);
		}
		this.model = ModelFactory.createDefaultModel();
		addModelPrefix(junoNs);
		addModelPrefix(junoBpNs);
		this.pRoot = model.createProperty(junoNs.getUri(), RDF_junoNs_ROOT);
		this.pValue = model.createProperty(junoNs.getUri(), RDF_junoNs_VALUE);
		this.pClass = model.createProperty(junoNs.getUri(), RDF_junoNs_CLASS);
		this.pType = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		rdfReader = model.getReader(rdfLanguage);

		// Note: NTripleReader throws an exception if you try to set any properties on it.
		if (! rdfLanguage.equals(LANG_NTRIPLE)) {
			for (Map.Entry<String,Object> e : jenaSettings.entrySet())
				rdfReader.setProperty(e.getKey(), e.getValue());
		}
	}

	boolean wasAlreadyProcessed(Resource r) {
		return ! urisVisited.add(r);
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
	 * Constructs a <code>Property</code> in the specified namespace in this mode.
	 *
	 * @param namespaceUri The namespace URI.
	 * @param name The property name.
	 * @return The new property object.
	 */
	public Property getProperty(String namespaceUri, String name) {
		return model.createProperty(namespaceUri, name);
	}

	/**
	 * Constructs a <code>Property</code> in the Juno Bean namespace in this mode.
	 *
	 * @param name The property name.
	 * @return The new property object.
	 */
	public Property getProperty(String name) {
		return model.createProperty(junoBpNs.getUri(), name);
	}

	/**
	 * Returns the format for serializing collections.
	 *
	 * @return The format for serializing collections.
	 */
	protected RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Returns the {@link RdfProperties#RDF_looseCollection} property value.
	 *
	 * @return The {@link RdfProperties#RDF_looseCollection} property value.
	 */
	protected boolean isLooseCollection() {
		return looseCollection;
	}
}
