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
 * <h5 class='topic'>Behavior-specific subclasses</h5>
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
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='link'><a class="doclink" href="package-summary.html#TOC">org.apache.juneau.jena &gt; RDF Overview</a>
 * </ul>
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
	 * Configuration property:  Trim whitespace from text elements.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"RdfParser.trimWhitespace.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link RdfParserBuilder#trimWhitespace(boolean)}
	 * 			<li class='jm'>{@link RdfParserBuilder#trimWhitespace()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create an RDF parser that trims whitespace.</jc>
	 * 	ReaderParser p = RdfParser
	 * 		.<jsm>create</jsm>()
	 * 		.xml()
	 * 		.trimWhitespace()
	 * 		.build();
	 *
	 * 	<jc>// Same, but use property.</jc>
	 * 	ReaderParser p = RdfParser
	 * 		.<jsm>create</jsm>()
	 * 		.xml()
	 * 		.set(<jsf>RDF_trimWhitespace</jsf>, <jk>true</jk>)
	 * 		.build();
	 * </p>
	 */
	public static final String RDF_trimWhitespace = PREFIX + "trimWhitespace.b";


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------

	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new Xml(PropertyStore.DEFAULT);

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new Turtle(PropertyStore.DEFAULT);

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new NTriple(PropertyStore.DEFAULT);

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new N3(PropertyStore.DEFAULT);


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
		public Xml(PropertyStore ps) {
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
		public NTriple(PropertyStore ps) {
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
		public Turtle(PropertyStore ps) {
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
		public N3(PropertyStore ps) {
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

	private final boolean trimWhitespace, looseCollections;
	private final String rdfLanguage;
	private final Namespace juneauNs, juneauBpNs;
	private final RdfCollectionFormat collectionFormat;

	final Map<String,Object> jenaSettings = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	public RdfParser(PropertyStore ps, String...consumes) {
		super(ps, consumes);
		trimWhitespace = getBooleanProperty(RDF_trimWhitespace, false);
		looseCollections = getBooleanProperty(RDF_looseCollections, false);
		rdfLanguage = getStringProperty(RDF_language, "RDF/XML-ABBREV");
		juneauNs = getInstanceProperty(RDF_juneauNs, Namespace.class, DEFAULT_JUNEAU_NS);
		juneauBpNs = getInstanceProperty(RDF_juneauBpNs, Namespace.class, DEFAULT_JUNEAUBP_NS);
		collectionFormat = getProperty(RDF_collectionFormat, RdfCollectionFormat.class, RdfCollectionFormat.DEFAULT);
	}

	/**
	 * Constructor.
	 *
	 * @param ps The property store containing all the settings for this object.
	 */
	public RdfParser(PropertyStore ps) {
		this(ps, "text/xml+rdf");
	}

	@Override /* Context */
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

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Configuration property:  Trim whitespace from text elements.
	 *
	 * @see #RDF_trimWhitespace
	 * @return
	 * 	<jk>true</jk> if whitespace in text elements will be automatically trimmed.
	 */
	protected final boolean isTrimWhitespace() {
		return trimWhitespace;
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
	 * 	XML namespace for Juneau properties.
	 */
	protected final Namespace getJuneauNs() {
		return juneauNs;
	}

	/**
	 * Configuration property:  Default XML namespace for bean properties.
	 *
	 * @see #RDF_juneauBpNs
	 * @return
	 * 	Default XML namespace for bean properties.
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
