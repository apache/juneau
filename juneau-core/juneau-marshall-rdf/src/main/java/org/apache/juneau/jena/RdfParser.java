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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Parses RDF into POJOs.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * 
 * The following direct subclasses are provided for language-specific parsers:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link RdfParser.Xml} - RDF/XML and RDF/XML-ABBREV.
 * 	<li>
 * 		{@link RdfParser.NTriple} - N-TRIPLE.
 * 	<li>
 * 		{@link RdfParser.Turtle} - TURTLE.
 * 	<li>
 * 		{@link RdfParser.N3} - N3.
 * </ul>
 *
 * <h5 class='section'>Additional information:</h5>
 * 
 * See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
 */
public class RdfParser extends ReaderParser implements RdfCommon {

	private static final Namespace 
		DEFAULT_JUNEAU_NS = Namespace.create("j", "http://www.apache.org/juneau/"),
		DEFAULT_JUNEAUBP_NS = Namespace.create("jp", "http://www.apache.org/juneaubp/");

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RdfParser.";

	/**
	 * <b>Configuration property:</b>  Trim whitespace from text elements.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfParser.trimWhitespace.b"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 */
	public static final String RDF_trimWhitespace = PREFIX + "trimWhitespace.b";

	
	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------
	
	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new Xml(PropertyStore2.DEFAULT);

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new Turtle(PropertyStore2.DEFAULT);

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new NTriple(PropertyStore2.DEFAULT);

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new N3(PropertyStore2.DEFAULT);


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Consumes RDF/XML input */
	public static class Xml extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param ps The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore2 ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_RDF_XML)
					.build(), 
				"text/xml+rdf"
			);
		}
	}

	/** Consumes N-Triple input */
	public static class NTriple extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param ps The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore2 ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_NTRIPLE)
					.build(), 
				"text/n-triple"
			);
		}
	}

	/** Consumes Turtle input */
	public static class Turtle extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param ps The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore2 ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_TURTLE)
					.build(), 
				"text/turtle"
			);
		}
	}

	/** Consumes N3 input */
	public static class N3 extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param ps The property store containing all the settings for this object.
		 */
		public N3(PropertyStore2 ps) {
			super(
				ps.builder()
					.set(RDF_language, LANG_N3)
					.build(), 
				"text/n3"
			);
		}
	}
	
	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final boolean trimWhitespace, looseCollections;
	final String rdfLanguage;
	final Namespace juneauNs, juneauBpNs;
	final RdfCollectionFormat collectionFormat;
	final Map<String,Object> jenaSettings = new HashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	public RdfParser(PropertyStore2 ps, String...consumes) {
		super(ps, consumes);
		trimWhitespace = getProperty(RDF_trimWhitespace, boolean.class, false);
		looseCollections = getProperty(RDF_looseCollections, boolean.class, false);
		rdfLanguage = getProperty(RDF_language, String.class, "RDF/XML-ABBREV");
		juneauNs = getInstanceProperty(RDF_juneauNs, Namespace.class, DEFAULT_JUNEAU_NS);
		juneauBpNs = getInstanceProperty(RDF_juneauBpNs, Namespace.class, DEFAULT_JUNEAUBP_NS);
		collectionFormat = getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param ps The property store containing all the settings for this object.
	 */
	public RdfParser(PropertyStore2 ps) {
		this(ps, "text/xml+rdf");
	}	
	
	@Override /* CoreObject */
	public RdfParserBuilder builder() {
		return new RdfParserBuilder(getPropertyStore());
	}

	/**
	 * Instantiates a new clean-slate {@link RdfParserBuilder} object.
	 * 
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> RdfParserBuilder()</code>.
	 * 
	 * <p>
	 * Note that this method creates a builder initialized to all default settings, whereas {@link #builder()} copies 
	 * the settings of the object called on.
	 * 
	 * @return A new {@link RdfParserBuilder} object.
	 */
	public static RdfParserBuilder create() {
		return new RdfParserBuilder();
	}

	@Override /* Parser */
	public ReaderParserSession createSession(ParserSessionArgs args) {
		return new RdfParserSession(this, args);
	}
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("RdfParser", new ObjectMap()
				.append("trimWhitespace", trimWhitespace)
				.append("looseCollections", looseCollections)
				.append("rdfLanguage", rdfLanguage)
				.append("juneauNs", juneauNs)
				.append("juneauBpNs", juneauBpNs)
				.append("collectionFormat", collectionFormat)
			);
	}
}
