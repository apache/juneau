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
import static org.apache.juneau.jena.RdfCommon.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Serializes POJOs to RDF.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
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
 * <h5 class='section'>Additional information:</h5>
 * 
 * See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
 */
public class RdfSerializer extends WriterSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RdfSerializer.";

	/**
	 * <b>Configuration property:</b>  Add XSI data types to non-<code>String</code> literals.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addLiteralTypes"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 */
	public static final String RDF_addLiteralTypes = PREFIX + "addLiteralTypes";

	/**
	 * <b>Configuration property:</b>  Add RDF root identifier property to root node.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addRootProperty"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * When enabled an RDF property <code>http://www.apache.org/juneau/root</code> is added with a value of <js>"true"</js>
	 * to identify the root node in the graph.
	 * This helps locate the root node during parsing.
	 * 
	 * <p>
	 * If disabled, the parser has to search through the model to find any resources without incoming predicates to 
	 * identify root notes, which can introduce a considerable performance degradation.
	 */
	public static final String RDF_addRootProperty = PREFIX + "addRootProperty";

	/**
	 * <b>Configuration property:</b>  Auto-detect namespace usage.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.autoDetectNamespaces"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>true</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * Detect namespace usage before serialization.
	 * 
	 * <p>
	 * If enabled, then the data structure will first be crawled looking for namespaces that will be encountered before 
	 * the root element is serialized.
	 */
	public static final String RDF_autoDetectNamespaces = PREFIX + "autoDetectNamespaces";

	/**
	 * <b>Configuration property:</b>  Default namespaces.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.namespaces.list"</js>
	 * 	<li><b>Data type:</b> <code>List&lt;{@link Namespace}&gt;</code>
	 * 	<li><b>Default:</b> empty list
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * The default list of namespaces associated with this serializer.
	 */
	public static final String RDF_namespaces = PREFIX + "namespaces.list";

	/**
	 * <b>Configuration property:</b>  Add <js>"_type"</js> properties when needed.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfSerializer.addBeanTypeProperties"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * If <jk>true</jk>, then <js>"_type"</js> properties will be added to beans if their type cannot be inferred 
	 * through reflection.
	 * This is used to recreate the correct objects during parsing if the object types cannot be inferred.
	 * For example, when serializing a {@code Map<String,Object>} field, where the bean class cannot be determined 
	 * from the value type.
	 * 
	 * <p>
	 * When present, this value overrides the {@link #SERIALIZER_addBeanTypeProperties} setting and is
	 * provided to customize the behavior of specific serializers in a {@link SerializerGroup}.
	 */
	public static final String RDF_addBeanTypeProperties = PREFIX + "addBeanTypeProperties";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XML = new Xml(PropertyStore.create());

	/** Default Abbreviated RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XMLABBREV = new XmlAbbrev(PropertyStore.create());

	/** Default Turtle serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_TURTLE = new Turtle(PropertyStore.create());

	/** Default N-Triple serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_NTRIPLE = new NTriple(PropertyStore.create());

	/** Default N3 serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_N3 = new N3(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Produces RDF/XML output */
	public static class Xml extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML), "text/xml+rdf");
		}
	}

	/** Produces Abbreviated RDF/XML output */
	public static class XmlAbbrev extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public XmlAbbrev(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML_ABBREV), "text/xml+rdf", "text/xml+rdf+abbrev");
		}
	}

	/** Produces N-Triple output */
	public static class NTriple extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_NTRIPLE), "text/n-triple");
		}
	}

	/** Produces Turtle output */
	public static class Turtle extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_TURTLE), "text/turtle");
		}
	}

	/** Produces N3 output */
	public static class N3 extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public N3(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_N3), "text/n3");
		}
	}


	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final RdfSerializerContext ctx;

	/**
	 * Constructor.
	 *
	 * @param propertyStore
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
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"application/json"</js>, <js>"text/json"</js>);</code>
	 * 	<br>...or...
	 * 	<br><code><jk>super</jk>(propertyStore, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);</code>
	 */
	public RdfSerializer(PropertyStore propertyStore, String produces, String...accept) {
		super(propertyStore, produces, accept);
		this.ctx = createContext(RdfSerializerContext.class);
	}

	@Override /* CoreObject */
	public RdfSerializerBuilder builder() {
		return new RdfSerializerBuilder(propertyStore);
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
		return new RdfSerializerSession(ctx, args);
	}
}
