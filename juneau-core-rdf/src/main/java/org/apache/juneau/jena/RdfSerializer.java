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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJOs to RDF.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * 
 * Refer to <a class="doclink" href="package-summary.html#SerializerConfigurableProperties">Configurable Properties</a>
 * 	for the entire list of configurable properties.
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
@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
public class RdfSerializer extends WriterSerializer {

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


	/** Produces RDF/XML output */
	@Produces("text/xml+rdf")
	public static class Xml extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML));
		}
	}

	/** Produces Abbreviated RDF/XML output */
	@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
	public static class XmlAbbrev extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public XmlAbbrev(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML_ABBREV));
		}
	}

	/** Produces N-Triple output */
	@Produces("text/n-triple")
	public static class NTriple extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_NTRIPLE));
		}
	}

	/** Produces Turtle output */
	@Produces("text/turtle")
	public static class Turtle extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_TURTLE));
		}
	}

	/** Produces N3 output */
	@Produces("text/n3")
	public static class N3 extends RdfSerializer {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public N3(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_N3));
		}
	}


	private final RdfSerializerContext ctx;

	/**
	 * Constructor.
	 * 
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public RdfSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(RdfSerializerContext.class);
	}

	@Override /* CoreObject */
	public RdfSerializerBuilder builder() {
		return new RdfSerializerBuilder(propertyStore);
	}

	@Override /* Serializer */
	public WriterSerializerSession createSession(SerializerSessionArgs args) {
		return new RdfSerializerSession(ctx, args);
	}
}
