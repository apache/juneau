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
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Serializes POJOs to RDF.
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
@ConfigurableContext(prefixes={RdfCommon.PREFIX,RdfSerializer.PREFIX})
public class RdfSerializer extends WriterSerializer implements RdfCommon, RdfMetaProvider {

	private static final Namespace
		DEFAULT_JUNEAU_NS = Namespace.create("j", "http://www.apache.org/juneau/"),
		DEFAULT_JUNEAUBP_NS = Namespace.create("jp", "http://www.apache.org/juneaubp/");

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "RdfSerializer";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_addBeanTypes RDF_addBeanTypes}
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfSerializer.addBeanTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_ADDBEANTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#addBeanTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#addBeanTypes()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred
	 * through reflection.
	 *
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypes} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String RDF_addBeanTypes = PREFIX + ".addBeanTypes.b";

	/**
	 * Configuration property:  Add XSI data types to non-<c>String</c> literals.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_addLiteralTypes RDF_addLiteralTypes}
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addLiteralTypes.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfSerializer.addLiteralTypes</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_ADDLITERALTYPES</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#addLiteralTypes()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#addLiteralTypes(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#addLiteralTypes()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_addLiteralTypes = PREFIX + ".addLiteralTypes.b";

	/**
	 * Configuration property:  Add RDF root identifier property to root node.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_addRootProperty RDF_addRootProperty}
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addRootProperty.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfSerializer.addRootProperty</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_ADDROOTPROPERTY</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#addRootProperty()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#addRootProperty(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#addRootProperty()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled an RDF property <c>http://www.apache.org/juneau/root</c> is added with a value of <js>"true"</js>
	 * to identify the root node in the graph.
	 * <br>This helps locate the root node during parsing.
	 *
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without incoming predicates to
	 * identify root notes, which can introduce a considerable performance degradation.
	 */
	public static final String RDF_addRootProperty = PREFIX + ".addRootProperty.b";

	/**
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_autoDetectNamespaces RDF_autoDetectNamespaces}
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.autoDetectNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>RdfSerializer.autoDetectNamespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_AUTODETECTNAMESPACES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#autoDetectNamespaces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#autoDetectNamespaces(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#dontAutoDetectNamespaces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Detect namespace usage before serialization.
	 *
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before
	 * the root element is serialized.
	 */
	public static final String RDF_autoDetectNamespaces = PREFIX + ".autoDetectNamespaces.b";

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_namespaces RDF_namespaces}
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.namespaces.ls"</js>
	 * 	<li><b>Data type:</b>  <c>List&lt;{@link org.apache.juneau.xml.Namespace}&gt;</c>
	 * 	<li><b>System property:</b>  <c>RdfSerializer.namespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_NAMESPACES</c>
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.Rdf#namespace()}
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#namespaces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#namespaces(Namespace...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String RDF_namespaces = PREFIX + ".namespaces.ls";

	/**
	 * Configuration property:  Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.jena.RdfSerializer#RDF_useXmlNamespaces RDF_useXmlNamespaces}
	 * 	<li><b>Name:</b>  <js>"Rdf.useXmlNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>Rdf.useXmlNamespaces</c>
	 * 	<li><b>Environment variable:</b>  <c>RDFSERIALIZER_USEXMLNAMESPACES</c>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.jena.annotation.RdfConfig#useXmlNamespaces()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#useXmlNamespaces(boolean)}
	 * 			<li class='jm'>{@link org.apache.juneau.jena.RdfSerializerBuilder#dontUseXmlNamespaces()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When specified, namespaces defined using {@link XmlNs @XmlNs} and {@link Xml @Xml} will be inherited by the RDF serializers.
	 * <br>Otherwise, namespaces will be defined using {@link RdfNs @RdfNs} and {@link Rdf @Rdf}.
	 */
	public static final String RDF_useXmlNamespaces = PREFIX + ".useXmlNamespaces.b";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final boolean
		addLiteralTypes,
		addRootProperty,
		useXmlNamespaces,
		looseCollections,
		autoDetectNamespaces,
		addBeanTypes;
	private final String rdfLanguage;
	private final Namespace juneauNs;
	private final Namespace juneauBpNs;
	private final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaProperties;
	final Namespace[] namespaces;

	private final Map<ClassMeta<?>,RdfClassMeta> rdfClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,RdfBeanMeta> rdfBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,RdfBeanPropertyMeta> rdfBeanPropertyMetas = new ConcurrentHashMap<>();

	private final Map<ClassMeta<?>,XmlClassMeta> xmlClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanMeta<?>,XmlBeanMeta> xmlBeanMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,XmlBeanPropertyMeta> xmlBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <c>media-type</c> specification of {@doc RFC2616.section14.1}
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <c>produces</c>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode w800'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	public RdfSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
		addLiteralTypes = getBooleanProperty(RDF_addLiteralTypes, false);
		addRootProperty = getBooleanProperty(RDF_addRootProperty, false);
		useXmlNamespaces = getBooleanProperty(RDF_useXmlNamespaces, true);
		looseCollections = getBooleanProperty(RDF_looseCollections, false);
		autoDetectNamespaces = getBooleanProperty(RDF_autoDetectNamespaces, true);
		rdfLanguage = getStringProperty(RDF_language, "RDF/XML-ABBREV");
		juneauNs = getProperty(RDF_juneauNs, Namespace.class, DEFAULT_JUNEAU_NS);
		juneauBpNs = getProperty(RDF_juneauBpNs, Namespace.class, DEFAULT_JUNEAUBP_NS);
		collectionFormat = getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
		namespaces = getProperty(RDF_namespaces, Namespace[].class, new Namespace[0]);
		addBeanTypes = getBooleanProperty(RDF_addBeanTypes, getBooleanProperty(SERIALIZER_addBeanTypes, false));

		ASortedMap<String,Object> m = ASortedMap.of();
		for (String k : getPropertyKeys("RdfCommon"))
			if (k.startsWith("jena."))
				m.put(k.substring(5), getProperty("RdfCommon." + k));
		jenaProperties = m.unmodifiable();
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public RdfSerializer(PropertyStore ps) {
		this(ps, getProduces(ps), (String)null);
	}

	private static String getProduces(PropertyStore ps) {
		String rdfLanguage = ps.getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		switch(rdfLanguage) {
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

	@Override /* Context */
	public RdfSerializerBuilder builder() {
		return new RdfSerializerBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link RdfSerializerBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> RdfSerializerBuilder()</code>.
	 *
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies
	 * the settings of the object called on.
	 *
	 * @return A new {@link RdfSerializerBuilder} object.
	 */
	public static RdfSerializerBuilder create() {
		return new RdfSerializerBuilder();
	}

	@Override /* Context */
	public  RdfSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
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
	 * Configuration property:  RDF format for representing collections and arrays.
	 *
	 * @see #RDF_collectionFormat
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 *
	 * @see #RDF_juneauBpNs
	 * @return
	 * 	The XML namespace to use for bean properties.
	 */
	protected final Namespace getJuneauBpNs() {
		return juneauBpNs;
	}

	/**
	 * Configuration property:  XML namespace for Juneau properties.
	 *
	 * @see #RDF_juneauNs
	 * @return
	 * 	The XML namespace to use for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * Configuration property:  RDF language.
	 *
	 * @see #RDF_language
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getLanguage() {
		return rdfLanguage;
	}

	/**
	 * Configuration property:  Collections should be serialized and parsed as loose collections.
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
	 * Configuration property:  All Jena-related configuration properties.
	 *
	 * @return
	 * 	A map of all Jena-related configuration properties.
	 */
	protected final Map<String,Object> getJenaProperties() {
		return jenaProperties;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
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
	 * Configuration property:  Add XSI data types to non-<c>String</c> literals.
	 *
	 * @see #RDF_addLiteralTypes
	 * @return
	 * 	<jk>true</jk> if XSI data types should be added to string literals.
	 */
	protected final boolean isAddLiteralTypes() {
		return addLiteralTypes;
	}

	/**
	 * Configuration property:  Add RDF root identifier property to root node.
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
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * @see #RDF_autoDetectNamespaces
	 * @return
	 * 	<jk>true</jk> if namespaces usage should be detected before serialization.
	 */
	protected final boolean isAutoDetectNamespaces() {
		return autoDetectNamespaces;
	}

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * @see #RDF_namespaces
	 * @return
	 * 	The default list of namespaces associated with this serializer.
	 */
	public Namespace[] getNamespaces() {
		return namespaces;
	}

	/**
	 * Configuration property:  Reuse XML namespaces when RDF namespaces not specified.
	 *
	 * @see #RDF_useXmlNamespaces
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
			.a("RdfSerializer", new DefaultFilteringOMap()
				.a("addLiteralTypes", addLiteralTypes)
				.a("addRootProperty", addRootProperty)
				.a("useXmlNamespaces", useXmlNamespaces)
				.a("looseCollections", looseCollections)
				.a("autoDetectNamespaces", autoDetectNamespaces)
				.a("rdfLanguage", rdfLanguage)
				.a("juneauNs", juneauNs)
				.a("juneauBpNs", juneauBpNs)
				.a("collectionFormat", collectionFormat)
				.a("namespaces", namespaces)
				.a("addBeanTypes", addBeanTypes)
			);
	}
}
