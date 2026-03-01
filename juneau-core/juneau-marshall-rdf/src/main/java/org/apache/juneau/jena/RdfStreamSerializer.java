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
 * limitations under the License. */
package org.apache.juneau.jena;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;

/**
 * Stream-based RDF serializer for binary formats (RDF/THRIFT, RDF/PROTO).
 *
 * <p>
 * Extends {@link OutputStreamSerializer} and delegates RDF metadata to an internal {@link RdfSerializer}.
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
public class RdfStreamSerializer extends OutputStreamSerializer implements RdfMetaProvider {

	// Argument name constants
	private static final String ARG_builder = "builder";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends OutputStreamSerializer.Builder {

		private static final Cache<HashKey,RdfStreamSerializer> CACHE = Cache.of(HashKey.class, RdfStreamSerializer.class).build();

		private String language;
		private RdfSerializer rdfSerializer;

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
			rdfSerializer = copyFrom.rdfSerializer;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(RdfStreamSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			language = copyFrom.language;
			rdfSerializer = copyFrom.rdfSerializer;
		}

		/**
		 * RDF language (e.g. {@link Constants#LANG_RDFTHRIFT}, {@link Constants#LANG_RDFPROTO}).
		 *
		 * @param value The RDF language.
		 * @return This object.
		 */
		public Builder language(String value) {
			language = value;
			rdfSerializer = null; // Rebuild on next get
			return this;
		}

		@Override
		public Builder produces(String value) {
			super.produces(value);
			return this;
		}

		@Override
		public Builder accept(String value) {
			super.accept(value);
			return this;
		}

		@Override
		public RdfStreamSerializer build() {
			return cache(CACHE).build(RdfStreamSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), language);
		}

		RdfSerializer getRdfSerializer() {
			if (rdfSerializer == null)
				rdfSerializer = RdfSerializer.create().language(language).build();
			return rdfSerializer;
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
	private final RdfSerializer rdfSerializer;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public RdfStreamSerializer(Builder builder) {
		super(assertArgNotNull(ARG_builder, builder).produces(getProduces(builder)).accept(getAccept(builder)));
		language = builder.language;
		rdfSerializer = builder.getRdfSerializer();
	}

	private static String getAccept(Builder builder) {
		if (builder.getAccept() != null)
			return builder.getAccept();
		return getProduces(builder);
	}

	private static String getProduces(Builder builder) {
		if (builder.getProduces() != null)
			return builder.getProduces();
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
	public RdfStreamSerializerSession.Builder createSession() {
		return RdfStreamSerializerSession.create(this);
	}

	@Override
	public RdfBeanMeta getRdfBeanMeta(BeanMeta<?> bm) {
		return rdfSerializer.getRdfBeanMeta(bm);
	}

	@Override
	public RdfBeanPropertyMeta getRdfBeanPropertyMeta(BeanPropertyMeta bpm) {
		return rdfSerializer.getRdfBeanPropertyMeta(bpm);
	}

	@Override
	public RdfClassMeta getRdfClassMeta(ClassMeta<?> cm) {
		return rdfSerializer.getRdfClassMeta(cm);
	}

	@Override
	public XmlBeanMeta getXmlBeanMeta(BeanMeta<?> bm) {
		return rdfSerializer.getXmlBeanMeta(bm);
	}

	@Override
	public XmlBeanPropertyMeta getXmlBeanPropertyMeta(BeanPropertyMeta bpm) {
		return rdfSerializer.getXmlBeanPropertyMeta(bpm);
	}

	@Override
	public XmlClassMeta getXmlClassMeta(ClassMeta<?> cm) {
		return rdfSerializer.getXmlClassMeta(cm);
	}

	/**
	 * Returns the RDF language.
	 *
	 * @return The RDF language identifier.
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Returns the delegated RdfSerializer.
	 *
	 * @return The delegated RdfSerializer.
	 */
	protected RdfSerializer getRdfSerializer() {
		return rdfSerializer;
	}

	/**
	 * Collection format - delegates to RdfSerializer.
	 *
	 * @return The RDF collection format.
	 */
	public RdfCollectionFormat getCollectionFormat() {
		return rdfSerializer.getCollectionFormat();
	}

	/**
	 * Juneau namespace - delegates to RdfSerializer.
	 *
	 * @return The Juneau namespace.
	 */
	public Namespace getJuneauNs() {
		return rdfSerializer.getJuneauNs();
	}

	/**
	 * Juneau BP namespace - delegates to RdfSerializer.
	 *
	 * @return The Juneau BP namespace.
	 */
	public Namespace getJuneauBpNs() {
		return rdfSerializer.getJuneauBpNs();
	}

	/**
	 * Namespaces - delegates to RdfSerializer.
	 *
	 * @return The RDF namespaces.
	 */
	public Namespace[] getNamespaces() {
		return rdfSerializer.getNamespaces();
	}

	/**
	 * Add literal types - delegates to RdfSerializer.
	 *
	 * @return True if literal types are added.
	 */
	public boolean isAddLiteralTypes() {
		return rdfSerializer.isAddLiteralTypes();
	}

	/**
	 * Add root property - delegates to RdfSerializer.
	 *
	 * @return True if root property is added.
	 */
	public boolean isAddRootProp() {
		return rdfSerializer.isAddRootProp();
	}

	/**
	 * Loose collections - delegates to RdfSerializer.
	 *
	 * @return True if loose collections are enabled.
	 */
	public boolean isLooseCollections() {
		return rdfSerializer.isLooseCollections();
	}

	/**
	 * Use XML namespaces - delegates to RdfSerializer.
	 *
	 * @return True if XML namespaces are used.
	 */
	public boolean isUseXmlNamespaces() {
		return rdfSerializer.isUseXmlNamespaces();
	}

	/**
	 * Auto-detect namespaces - delegates to RdfSerializer.
	 *
	 * @return True if auto-detect namespaces is enabled.
	 */
	public boolean isAutoDetectNamespaces() {
		return rdfSerializer.isAutoDetectNamespaces();
	}

	@Override
	protected boolean isAddBeanTypes() {
		return rdfSerializer.isAddBeanTypes();
	}
}
