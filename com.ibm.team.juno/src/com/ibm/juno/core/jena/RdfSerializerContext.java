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
import static com.ibm.juno.core.jena.RdfSerializerProperties.*;

import java.lang.reflect.*;
import java.util.*;

import com.hp.hpl.jena.rdf.model.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.xml.*;

/**
 * Context object that lives for the duration of a single serialization of {@link RdfSerializer}.
 * <p>
 * 	See {@link SerializerContext} for details.
 * </p>
 * <p>
 * 	This class is NOT thread safe.  It is meant to be discarded after one-time use.
 * </p>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
public final class RdfSerializerContext extends XmlSerializerContext {

	private final String rdfLanguage;
	private final Namespace junoNs, junoBpNs;
	private final boolean addLiteralTypes, addRootProperty, useXmlNamespaces, looseCollection;
	private final Property pRoot, pValue, pClass;
	private final Model model;
	private final RDFWriter writer;
	private final RdfCollectionFormat collectionFormat;

	/**
	 * Constructor.
	 * @param beanContext The bean context being used by the serializer.
	 * @param sp Default general serializer properties.
	 * @param xsp Default XML serializer properties.
	 * @param jsp Default Jena serializer properties.
	 * @param op Override properties.
	 * @param javaMethod Java method that invoked this serializer.
	 * 	When using the REST API, this is the Java method invoked by the REST call.
	 * 	Can be used to access annotations defined on the method or class.
	 */
	protected RdfSerializerContext(BeanContext beanContext, SerializerProperties sp, XmlSerializerProperties xsp, RdfSerializerProperties jsp, ObjectMap op, Method javaMethod) {
		super(beanContext, sp, xsp, op, javaMethod);
		ObjectMap jenaSettings = new ObjectMap();
		jenaSettings.put("rdfXml.tab", isUseIndentation() ? 2 : 0);
		jenaSettings.put("rdfXml.attributeQuoteChar", Character.toString(getQuoteChar()));
		jenaSettings.putAll(jsp.jenaSettings);
		if (op == null || op.isEmpty()) {
			this.rdfLanguage = jsp.rdfLanguage;
			this.junoNs = jsp.junoNs;
			this.junoBpNs = jsp.junoBpNs;
			this.addLiteralTypes = jsp.addLiteralTypes;
			this.addRootProperty = jsp.addRootProperty;
			this.collectionFormat = jsp.collectionFormat;
			this.looseCollection = jsp.looseCollection;
			this.useXmlNamespaces = jsp.useXmlNamespaces;
		} else {
			this.rdfLanguage = op.getString(RDF_language, jsp.rdfLanguage);
			this.junoNs = (op.containsKey(RDF_junoNs) ? NamespaceFactory.parseNamespace(op.get(RDF_junoNs)) : jsp.junoNs);
			this.junoBpNs = (op.containsKey(RDF_junoBpNs) ? NamespaceFactory.parseNamespace(op.get(RDF_junoBpNs)) : jsp.junoBpNs);
			this.addLiteralTypes = op.getBoolean(RDF_addLiteralTypes, jsp.addLiteralTypes);
			this.addRootProperty = op.getBoolean(RDF_addRootProperty, jsp.addRootProperty);
			for (Map.Entry<String,Object> e : op.entrySet()) {
				String key = e.getKey();
				if (key.startsWith("Rdf.jena."))
					jenaSettings.put(key.substring(9), e.getValue());
			}
			this.collectionFormat = RdfCollectionFormat.valueOf(op.getString(RDF_collectionFormat, "DEFAULT"));
			this.looseCollection = op.getBoolean(RDF_looseCollection, jsp.looseCollection);
			this.useXmlNamespaces = op.getBoolean(RDF_useXmlNamespaces, jsp.useXmlNamespaces);
		}
		this.model = ModelFactory.createDefaultModel();
		addModelPrefix(junoNs);
		addModelPrefix(junoBpNs);
		for (Namespace ns : this.getNamespaces())
			addModelPrefix(ns);
		this.pRoot = model.createProperty(junoNs.getUri(), RDF_junoNs_ROOT);
		this.pValue = model.createProperty(junoNs.getUri(), RDF_junoNs_VALUE);
		this.pClass = model.createProperty(junoNs.getUri(), RDF_junoNs_CLASS);
		writer = model.getWriter(rdfLanguage);

		// Only apply properties with this prefix!
		String propPrefix = RdfProperties.LANG_PROP_MAP.get(rdfLanguage);
		if (propPrefix == null)
			throw new RuntimeException("Unknown RDF language encountered: '"+rdfLanguage+"'");

		for (Map.Entry<String,Object> e : jenaSettings.entrySet())
			if (e.getKey().startsWith(propPrefix))
				writer.setProperty(e.getKey().substring(propPrefix.length()), e.getValue());
	}

	/**
	 * Adds the specified namespace as a model prefix.
	 * @param ns The XML namespace.
	 */
	public void addModelPrefix(Namespace ns) {
		model.setNsPrefix(ns.getName(), ns.getUri());
	}

	final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	final boolean isUseXmlNamespaces() {
		return useXmlNamespaces;
	}

	final boolean isLooseCollection() {
		return looseCollection;
	}

	final String getRdfLanguage() {
		return rdfLanguage;
	}

	final Namespace getJunoNs() {
		return junoNs;
	}

	final Namespace getJunoBpNs() {
		return junoBpNs;
	}

	final boolean isAddLiteralTypes() {
		return addLiteralTypes;
	}

	final boolean isAddRootProperty() {
		return addRootProperty;
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

	final Model getModel() {
		return model;
	}

	final RDFWriter getWriter() {
		return writer;
	}

	final String encodeTextInvalidChars(Object o) {
		if (o == null)
			return null;
		String s = o.toString();
		if (isTrimStrings())
			s = s.trim();
		return XmlUtils.encodeTextInvalidChars(s);
	}

	final String encodeElementName(Object o) {
		if (isTrimStrings() && o != null)
			o = o.toString().trim();
		return XmlUtils.encodeElementName(o);
	}
}
