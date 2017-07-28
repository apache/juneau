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
import org.apache.juneau.parser.*;

/**
 * Parses RDF into POJOs.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * 
 * Refer to <a class="doclink" href="package-summary.html#ParserConfigurableProperties">Configurable Properties</a>
 * for the entire list of configurable properties.
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
@Consumes(value="text/xml+rdf")
public class RdfParser extends ReaderParser {

	/** Default XML parser, all default settings.*/
	public static final RdfParser DEFAULT_XML = new RdfParser(PropertyStore.create());

	/** Default Turtle parser, all default settings.*/
	public static final RdfParser DEFAULT_TURTLE = new Turtle(PropertyStore.create());

	/** Default N-Triple parser, all default settings.*/
	public static final RdfParser DEFAULT_NTRIPLE = new NTriple(PropertyStore.create());

	/** Default N3 parser, all default settings.*/
	public static final RdfParser DEFAULT_N3 = new N3(PropertyStore.create());


	/** Consumes RDF/XML input */
	@Consumes("text/xml+rdf")
	public static class Xml extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_RDF_XML));
		}
	}

	/** Consumes N-Triple input */
	@Consumes(value="text/n-triple")
	public static class NTriple extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_NTRIPLE));
		}
	}

	/** Consumes Turtle input */
	@Consumes(value="text/turtle")
	public static class Turtle extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_TURTLE));
		}
	}

	/** Consumes N3 input */
	@Consumes(value="text/n3")
	public static class N3 extends RdfParser {

		/**
		 * Constructor.
		 * 
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public N3(PropertyStore propertyStore) {
			super(propertyStore.copy().append(RDF_language, LANG_N3));
		}
	}


	private final RdfParserContext ctx;

	/**
	 * Constructor.
	 * 
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public RdfParser(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(RdfParserContext.class);
	}

	@Override /* CoreObject */
	public RdfParserBuilder builder() {
		return new RdfParserBuilder(propertyStore);
	}

	@Override /* Parser */
	public ReaderParserSession createSession(ParserSessionArgs args) {
		return new RdfParserSession(ctx, args);
	}
}
