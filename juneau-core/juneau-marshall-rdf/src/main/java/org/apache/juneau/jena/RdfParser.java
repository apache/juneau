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
import org.apache.juneau.parser.*;

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
public class RdfParser extends ReaderParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "RdfParser.";

	/**
	 * <b>Configuration property:</b>  Trim whitespace from text elements.
	 * 
	 * <ul>
	 * 	<li><b>Name:</b> <js>"RdfParser.trimWhitespace"</js>
	 * 	<li><b>Data type:</b> <code>Boolean</code>
	 * 	<li><b>Default:</b> <jk>false</jk>
	 * 	<li><b>Session-overridable:</b> <jk>true</jk>
	 * </ul>
	 * 
	 * <p>
	 * If <jk>true</jk>, whitespace in text elements will be automatically trimmed.
	 */
	public static final String RDF_trimWhitespace = PREFIX + "trimWhitespace";

	
	//-------------------------------------------------------------------------------------------------------------------
	// Predefined instances
	//-------------------------------------------------------------------------------------------------------------------
	
	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new Xml(PropertyStore.create());

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new Turtle(PropertyStore.create());

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new NTriple(PropertyStore.create());

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new N3(PropertyStore.create());


	//-------------------------------------------------------------------------------------------------------------------
	// Predefined subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Consumes RDF/XML input */
	public static class Xml extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML), "text/xml+rdf");
		}
	}

	/** Consumes N-Triple input */
	public static class NTriple extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_NTRIPLE), "text/n-triple");
		}
	}

	/** Consumes Turtle input */
	public static class Turtle extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_TURTLE), "text/turtle");
		}
	}

	/** Consumes N3 input */
	public static class N3 extends RdfParser {

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

	private final RdfParserContext ctx;

	/**
	 * Constructor.
	 * 
	 * @param propertyStore The property store containing all the settings for this object.
	 * @param consumes The list of media types that this parser consumes (e.g. <js>"application/json"</js>).
	 */
	public RdfParser(PropertyStore propertyStore, String...consumes) {
		super(propertyStore, consumes);
		this.ctx = createContext(RdfParserContext.class);
	}
	
	@Override /* CoreObject */
	public RdfParserBuilder builder() {
		return new RdfParserBuilder(propertyStore);
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
		return new RdfParserSession(ctx, args);
	}
}
