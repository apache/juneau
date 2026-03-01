/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.xml.*;

/**
 * Stream-based RDF parser for binary formats (RDF/THRIFT, RDF/PROTO).
 *
 * <p>
 * Extends {@link InputStreamParser} and delegates RDF metadata to an internal {@link RdfParser}.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'>{doc jmr.RdfDetails}
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class RdfStreamParser extends InputStreamParser implements RdfMetaProvider {

	// Argument name constants
	private static final String ARG_builder = "builder";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends InputStreamParser.Builder {

		private static final Cache<HashKey,RdfStreamParser> CACHE = Cache.of(HashKey.class, RdfStreamParser.class).build();

		private String language;
		private RdfParser rdfParser;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			language = Constants.LANG_RDFTHRIFT;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			language = copyFrom.language;
			rdfParser = copyFrom.rdfParser;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(RdfStreamParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			language = copyFrom.language;
			rdfParser = copyFrom.rdfParser;
		}

		/**
		 * RDF language (e.g. {@link Constants#LANG_RDFTHRIFT}, {@link Constants#LANG_RDFPROTO}).
		 *
		 * @param value The RDF language.
		 * @return This object.
		 */
		public Builder language(String value) {
			language = value;
			rdfParser = null; // Rebuild on next get
			return this;
		}

		@Override
		public RdfStreamParser build() {
			return cache(CACHE).build(RdfStreamParser.class);
		}

		@Override
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), language);
		}

		RdfParser getRdfParser() {
			if (rdfParser == null)
				rdfParser = RdfParser.create().language(language).build();
			return rdfParser;
		}
	}

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final String language;
	private final RdfParser rdfParser;

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public RdfStreamParser(Builder builder) {
		super(assertArgNotNull(ARG_builder, builder).consumes(getConsumes(builder)));
		language = builder.language;
		rdfParser = builder.getRdfParser();
	}

	private static String getConsumes(Builder builder) {
		if (builder.getConsumes() != null)
			return builder.getConsumes();
		return switch (builder.language) {
			case Constants.LANG_RDFTHRIFT -> "application/vnd.apache.thrift.binary";
			case Constants.LANG_RDFPROTO -> "application/vnd.apache.protobuf";
			default -> "application/octet-stream";
		};
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public RdfStreamParserSession.Builder createSession() {
		return RdfStreamParserSession.create(this);
	}

	@Override
	public RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		return rdfParser.getRdfBeanMeta(bm);
	}

	@Override
	public RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		return rdfParser.getRdfBeanPropertyMeta(bpm);
	}

	@Override
	public RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return rdfParser.getRdfClassMeta(cm);
	}

	@Override
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		return rdfParser.getXmlBeanMeta(bm);
	}

	@Override
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return rdfParser.getXmlBeanPropertyMeta(bpm);
	}

	@Override
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		return rdfParser.getXmlClassMeta(cm);
	}

	/**
	 * Returns the RDF language.
	 *
	 * @return The RDF language.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Returns the delegated RdfParser.
	 *
	 * @return The delegated RdfParser.
	 */
	protected RdfParser getRdfParser() {
		return rdfParser;
	}

	/** Collection format - delegates to RdfParser. */
	protected RdfCollectionFormat getCollectionFormat() {
		return rdfParser.getCollectionFormat();
	}

	/** Juneau namespace - delegates to RdfParser. */
	protected Namespace getJuneauNs() {
		return rdfParser.getJuneauNs();
	}

	/** Juneau BP namespace - delegates to RdfParser. */
	protected Namespace getJuneauBpNs() {
		return rdfParser.getJuneauBpNs();
	}

	/** Loose collections - delegates to RdfParser. */
	protected boolean isLooseCollections() {
		return rdfParser.isLooseCollections();
	}

	/** Trim whitespace - delegates to RdfParser. */
	protected boolean isTrimWhitespace() {
		return rdfParser.isTrimWhitespace();
	}
}
