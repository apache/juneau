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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.jena.Constants.*;

import java.util.*;

import org.apache.juneau.*;
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
 * 	<li>{@link RdfSerializer.Xml} - RDF/XML.
 * 	<li>{@link RdfSerializer.XmlAbbrev} - RDF/XML-ABBREV.
 * 	<li>{@link RdfSerializer.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfSerializer.Turtle} - TURTLE.
 * 	<li>{@link RdfSerializer.N3} - N3.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="package-summary.html#TOC">org.apache.juneau.jena &gt; RDF Overview</a>
 * </ul>
 */
public class RdfSerializer extends WriterSerializer implements RdfCommon {

	private static final Namespace
		DEFAULT_JUNEAU_NS = Namespace.create("j", "http://www.apache.org/juneau/"),
		DEFAULT_JUNEAUBP_NS = Namespace.create("jp", "http://www.apache.org/juneaubp/");

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RdfSerializer.";

	/**
	 * Configuration property:  Add <js>"_type"</js> properties when needed.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addBeanTypes.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#addBeanTypes(boolean)}
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
	public static final String RDF_addBeanTypes = PREFIX + "addBeanTypes.b";

	/**
	 * Configuration property:  Add XSI data types to non-<code>String</code> literals.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addLiteralTypes.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#addLiteralTypes(boolean)}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#addLiteralTypes()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String RDF_addLiteralTypes = PREFIX + "addLiteralTypes.b";

	/**
	 * Configuration property:  Add RDF root identifier property to root node.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.addRootProperty.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#addRootProperty(boolean)}
	 * 			<li class='jm'>{@link RdfSerializerBuilder#addRootProperty()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * to identify the root node in the graph.
	 * <br>This helps locate the root node during parsing.
	 *
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without incoming predicates to
	 * identify root notes, which can introduce a considerable performance degradation.
	 */
	public static final String RDF_addRootProperty = PREFIX + "addRootProperty.b";

	/**
	 * Configuration property:  Auto-detect namespace usage.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.autoDetectNamespaces.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>true</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#autoDetectNamespaces(boolean)}
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
	public static final String RDF_autoDetectNamespaces = PREFIX + "autoDetectNamespaces.b";

	/**
	 * Configuration property:  Default namespaces.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfSerializer.namespaces.ls"</js>
	 * 	<li><b>Data type:</b>  <code>List&lt;String&gt;</code> (serialized {@link Namespace} objects)
	 * 	<li><b>Default:</b>  empty list
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link Rdf#namespace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfSerializerBuilder#namespaces(Namespace...)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String RDF_namespaces = PREFIX + "namespaces.ls";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XML = new Xml(PropertyStore.DEFAULT);

	/** Default Abbreviated RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XMLABBREV = new XmlAbbrev(PropertyStore.DEFAULT);

	/** Default Turtle serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_TURTLE = new Turtle(PropertyStore.DEFAULT);

	/** Default N-Triple serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_NTRIPLE = new NTriple(PropertyStore.DEFAULT);

	/** Default N3 serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_N3 = new N3(PropertyStore.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Produces RDF/XML output */
	public static class Xml extends RdfSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_RDF_XML)
					.build(),
				"text/xml+rdf", "text/xml+rdf,text/xml+rdf+abbrev;q=0.9"
			);
		}
	}

	/** Produces Abbreviated RDF/XML output */
	public static class XmlAbbrev extends RdfSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public XmlAbbrev(PropertyStore ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_RDF_XML_ABBREV)
					.build(),
				"text/xml+rdf", "text/xml+rdf+abbrev,text/xml+rdf;q=0.9"
			);
		}
	}

	/** Produces N-Triple output */
	public static class NTriple extends RdfSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_NTRIPLE)
					.build(),
				"text/n-triple", null
			);
		}
	}

	/** Produces Turtle output */
	public static class Turtle extends RdfSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_TURTLE)
					.build(),
				"text/turtle", null
			);
		}
	}

	/** Produces N3 output */
	public static class N3 extends RdfSerializer {

		/**
		 * Constructor.
		 *
		 * @param ps The property store containing all the settings for this object.
		 */
		public N3(PropertyStore ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_N3)
					.build(),
				"text/n3", null
			);
		}
	}


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
	final Map<String,Object> jenaSettings;
	final Namespace[] namespaces;

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
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode'>
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

		Map<String,Object> m = new LinkedHashMap<>();
		for (String k : getPropertyKeys("RdfCommon"))
			if (k.startsWith("jena."))
				m.put(k.substring(5), getProperty(k));
		jenaSettings = unmodifiableMap(m);
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 */
	public RdfSerializer(PropertyStore ps) {
		this(ps, "text/xml+rdf", null);
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

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new RdfSerializerSession(this, args);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Add XSI data types to non-<code>String</code> literals.
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
	 * @see #RDF_addRootProperty
	 * @return
	 * 	<jk>true</jk> if RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * 	to identify the root node in the graph.
	 */
	protected final boolean isAddRootProperty() {
		return addRootProperty;
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
	 * Configuration property:  RDF language.
	 *
	 * @see #RDF_language
	 * @return
	 * 	The RDF language to use.
	 */
	protected final String getRdfLanguage() {
		return rdfLanguage;
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
	 * Configuration property:  RDF format for representing collections and arrays.
	 *
	 * @see #RDF_collectionFormat
	 * @return
	 * 	RDF format for representing collections and arrays.
	 */
	protected final RdfCollectionFormat getCollectionFormat() {
		return collectionFormat;
	}

	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("RdfSerializer", new ObjectMap()
				.append("addLiteralTypes", addLiteralTypes)
				.append("addRootProperty", addRootProperty)
				.append("useXmlNamespaces", useXmlNamespaces)
				.append("looseCollections", looseCollections)
				.append("autoDetectNamespaces", autoDetectNamespaces)
				.append("rdfLanguage", rdfLanguage)
				.append("juneauNs", juneauNs)
				.append("juneauBpNs", juneauBpNs)
				.append("collectionFormat", collectionFormat)
				.append("namespaces", namespaces)
				.append("addBeanTypes", addBeanTypes)
			);
	}
}
