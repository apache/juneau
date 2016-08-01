/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2016. All Rights Reserved.
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
 * </p>
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public class RdfParserContext extends ParserContext {

	private final String rdfLanguage;
	private final Namespace junoNs, junoBpNs;
	private final Property pRoot, pValue, pClass, pType;
	private final Model model;
	private final boolean trimWhitespace, looseCollection;
	private final RDFReader rdfReader;
	private final Set<Resource> urisVisited = new HashSet<Resource>();
	private final RdfCollectionFormat collectionFormat;

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

	final boolean wasAlreadyProcessed(Resource r) {
		return ! urisVisited.add(r);
	}

	final Property getRootProperty() {
		return pRoot;
	}

	final Property getValueProperty() {
		return pValue;
	}

	final Property getClassProperty() {
		return pClass;
	}

	final Property getTypeProperty() {
		return pType;
	}

	final Model getModel() {
		return model;
	}

	final RDFReader getRdfReader() {
		return rdfReader;
	}

	final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	final boolean isLooseCollection() {
		return looseCollection;
	}

	final String getJunoNsUri() {
		return junoNs.getUri();
	}

	/**
	 * Adds the specified namespace as a model prefix.
	 *
	 * @param ns The XML namespace.
	 */
	final void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	/**
	 * Constructs a <code>Property</code> in the Juno Bean namespace in this mode.
	 *
	 * @param name The property name.
	 * @return The new property object.
	 */
	final Property getProperty(String name) {
		return model.createProperty(junoBpNs.getUri(), name);
	}

	/**
	 * Decodes the specified string.
	 * <p>
	 * 	If {@link RdfParserProperties#RDF_trimWhitespace} is <jk>true</jk>, the resulting string is trimmed before decoding.
	 * </p>
	 * <p>
	 * 	If {@link #isTrimStrings()} is <jk>true</jk>, the resulting string is trimmed after decoding.
	 * </p>
	 *
	 * @param s The string to trim.
	 * @return The trimmed string, or <jk>null</jk> if the string was <jk>null</jk>.
	 */
	final String decodeString(Object o) {
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
