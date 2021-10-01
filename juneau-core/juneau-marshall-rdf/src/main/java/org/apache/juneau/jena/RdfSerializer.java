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

import static java.util.Optional.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJOs to RDF.
 * {@review}
 *
 * <h5 class='topic'>Behavior-specific subclasses</h5>
 *
 * The following direct subclasses are provided for language-specific serializers:
 * <ul>
 * 	<li>{@link RdfXmlSerializer} - RDF/XML.
 * 	<li>{@link RdfXmlAbbrevSerializer} - RDF/XML-ABBREV.
 * 	<li>{@link NTripleSerializer} - N-TRIPLE.
 * 	<li>{@link TurtleSerializer} - TURTLE.
 * 	<li>{@link N3Serializer} - N3.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc juneau-marshall-rdf}
 * </ul>
 */
public class RdfSerializer extends WriterSerializer implements RdfMetaProvider {

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean
		addLiteralTypes,
		addRootProperty,
		useXmlNamespaces,
		looseCollections,
		autoDetectNamespaces,
		addBeanTypesRdf;

	final String language;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings;
	final Namespace[] namespaces;

	private final boolean addBeanTypes;
	private final Map<ClassMeta<?>,RdfClassMeta> rdfClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,RdfBeanMeta> rdfBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,RdfBeanPropertyMeta> rdfBeanPropertyMetas = new ConcurrentHashMap<>();

	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected RdfSerializer(RdfSerializerBuilder builder) {
		super(builder.produces(getProduces(builder)).accept(getAccept(builder)));
		addLiteralTypes = builder.addLiteralTypes;
		addRootProperty = builder.addRootProperty;
		useXmlNamespaces = ! builder.disableUseXmlNamespaces;
		looseCollections = builder.looseCollections;
		autoDetectNamespaces = ! builder.disableAutoDetectNamespaces;
		language = builder.language;
		juneauNs = builder.juneauNs;
		juneauBpNs = builder.juneauBpNs;
		collectionFormat = builder.collectionFormat;
		namespaces = ofNullable(builder.namespaces).map(x -> x.toArray(new Namespace[0])).orElse(new Namespace[0]);
		addBeanTypesRdf = builder.addBeanTypesRdf;
		jenaSettings = new TreeMap<>(builder.jenaSettings);

		addBeanTypes = addBeanTypesRdf || super.isAddBeanTypes();
	}

	private static String getProduces(RdfSerializerBuilder builder) {
		if (builder.getProduces() != null)
			return builder.getProduces();
		switch(builder.language) {
			case "RDF/XML": return "text/xml+rdf+abbrev";
			case "RDF/XML-ABBREV": return "text/xml+rdf";
			case "N-TRIPLE": return "text/n-triple";
			case "N3": return "text/n3";
			case "N3-PP": return "text/n3-pp";
			case "N3-PLAIN": return "text/n3-plain";
			case "N3-TRIPLES": return "text/n3-triples";
			case "TURTLE": return "text/turtle";
			default: return "text/xml+rdf";
		}
	}

	private static String getAccept(RdfSerializerBuilder builder) {
		if (builder.getAccept() != null)
			return builder.getAccept();
		switch(builder.language) {
			case "RDF/XML": return "text/xml+rdf+abbrev";
			case "RDF/XML-ABBREV": return "text/xml+rdf+abbrev,text/xml+rdf;q=0.9";
			case "N-TRIPLE": return "text/n-triple";
			case "N3": return "text/n3";
			case "N3-PP": return "text/n3-pp";
			case "N3-PLAIN": return "text/n3-plain";
			case "N3-TRIPLES": return "text/n3-triples";
			case "TURTLE": return "text/turtle";
			default: return "text/xml+rdf";
		}
	}

	@Override /* Context */
	public RdfSerializerBuilder copy() {
		return new RdfSerializerBuilder(this);
	}

	/**
	 * Instantiates a new clean-slate {@link RdfSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> RdfSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #copy()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link RdfSerializerBuilder} object.
	 */
	public static RdfSerializerBuilder create() {
		return new RdfSerializerBuilder();
	}

	@Override /* Context */
	public  RdfSerializerSession createSession() {
		return createSession(defaultArgs());
	}

	@Override /* Serializer */
	public RdfSerializerSession createSession(SerializerSessionArgs args) {
		return new RdfSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Extended metadata
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* RdfMetaProvider */
	public RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		RdfClassMeta m = rdfClassMetas.get(cm);
		if (m == null) {
			m = new RdfClassMeta(cm, this);
			rdfClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* RdfMetaProvider */
	public RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		RdfBeanMeta m = rdfBeanMetas.get(bm);
		if (m == null) {
			m = new RdfBeanMeta(bm, this);
			rdfBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* RdfMetaProvider */
	public RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		RdfBeanPropertyMeta m = rdfBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new RdfBeanPropertyMeta(bpm.getDelegateFor(), this);
			rdfBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		XmlClassMeta m = xmlClassMetas.get(cm);
		if (m == null) {
			m = new XmlClassMeta(cm, this);
			xmlClassMetas.put(cm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		XmlBeanMeta m = xmlBeanMetas.get(bm);
		if (m == null) {
			m = new XmlBeanMeta(bm, this);
			xmlBeanMetas.put(bm, m);
		}
		return m;
	}

	@Override /* XmlMetaProvider */
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		XmlBeanPropertyMeta m = xmlBeanPropertyMetas.get(bpm);
		if (m == null) {
			m = new XmlBeanPropertyMeta(bpm.getDelegateFor(), this);
			xmlBeanPropertyMetas.put(bpm, m);
		}
		return m;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Common properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * RDF format for representing collections and arrays.
	 *
	 * @see #RDF_collectionFormat
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Default XML namespace for bean properties.
	 *
	 * @see #RDF_juneauBpNs
	 * @return
	 * 	The XML namespace to use for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return juneauBpNs;
	}

	/**
	 * XML namespace for Juneau properties.
	 *
	 * @see #RDF_juneauNs
	 * @return
	 * 	The XML namespace to use for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * RDF language.
	 *
	 * @see #RDF_language
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return language;
	}

	/**
	 * Collections should be serialized and parsed as loose collections.
	 *
	 * @see #RDF_looseCollections
	 * @return
	 * 	<jk>true</jk> if collections of resources are handled as loose collections of resources in RDF instead of
	 * 	resources that are children of an RDF collection (e.g. Sequence, Bag).
	 */
	protected final boolean isLooseCollections() {
		return looseCollections;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Jena properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaSettings() {
		return jenaSettings;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Add <js>"_type"</js> properties when needed.
	 *
	 * @see #RDF_addBeanTypes
	 * @return
	 * 	<jk>true</jk> if <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * 	through reflection.
	 */
	@Override
	protected final boolean isAddBeanTypes() {
		return addBeanTypes;
	}

	/**
	 * Add XSI data types to non-<c>String</c> literals.
	 *
	 * @see #RDF_addLiteralTypes
	 * @return
	 * 	<jk>true</jk> if XSI data types should be added to string literals.
	 */
	protected final boolean isAddLiteralTypes() {
		return addLiteralTypes;
	}

	/**
	 * Add RDF root identifier property to root node.
	 *
	 * @see RdfSerializer#RDF_addRootProperty
	 * @return
	 * 	<jk>true</jk> if RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 */
	protected final boolean isAddRootProp() {
		return addRootProperty;
	}

	/**
	 * Auto-detect namespace usage.
	 *
	 * @see #RDF_disableAutoDetectNamespaces
	 * @return
	 * 	<jk>true</jk> if namespaces usage should be detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Default namespaces.
	 *
	 * @see #RDF_namespaces
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	public Namespace[] getNamespaces() {
		return namespaces;
	}

	/**
	 * Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * @see #RDF_disableUseXmlNamespaces
	 * @return
	 * 	<jk>true</jk> if namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * 	<br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 */
	protected final boolean isUseXmlNamespaces() {
		return useXmlNamespaces;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"RdfSerializer",
				OMap
					.create()
					.filtered()
					.a("addLiteralTypes", addLiteralTypes)
					.a("addRootProperty", addRootProperty)
					.a("useXmlNamespaces", useXmlNamespaces)
					.a("looseCollections", looseCollections)
					.a("autoDetectNamespaces", autoDetectNamespaces)
					.a("language", language)
					.a("juneauNs", juneauNs)
					.a("juneauBpNs", juneauBpNs)
					.a("collectionFormat", collectionFormat)
					.a("namespaces", namespaces)
					.a("addBeanTypes", addBeanTypes)
			);
	}
}
