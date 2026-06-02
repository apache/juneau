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
package org.apache.juneau.oapi;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.annotation.Schema;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.httppart.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.commons.bean.BeanPropertyMeta;

/**
 * OpenAPI part parser.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/OpenApiBasics">OpenApi Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for OpenApiParser hierarchy
})
public class OpenApiParser extends UonParser implements OpenApiMetaProvider {

	/**
	 * Builder class.
	 */
	public static class Builder extends UonParser.Builder<Builder> {

		private static final Cache<HashKey,OpenApiParser> CACHE = Cache.of(HashKey.class, OpenApiParser.class).build();

		HttpPartFormat format;
		HttpPartCollectionFormat collectionFormat;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("text/openapi");
			format = HttpPartFormat.NO_FORMAT;
			collectionFormat = HttpPartCollectionFormat.NO_COLLECTION_FORMAT;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			format = copyFrom.format;
			collectionFormat = copyFrom.collectionFormat;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(OpenApiParser copyFrom) {
			super(copyFrom);
			format = copyFrom.format;
			collectionFormat = copyFrom.collectionFormat;
		}

		@Override /* Overridden from Context.Builder<?> */
		public OpenApiParser build() {
			return cache(CACHE).build(OpenApiParser.class);
		}

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default collection format for HTTP parts.
		 *
		 * <p>
		 * Specifies the collection format to use for HTTP parts when not otherwise specified via {@link Schema#collectionFormat()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a parser using CSV for collections.</jc>
		 * 	OpenApiParser <jv>parser1</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>CSV</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Create a serializer using UON for collections.</jc>
		 * 	OpenApiParser <jv>parser2</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.collectionFormat(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Parse CSV.</jc>
		 * 	JsonList <jv>list1</jv> = <jv>parser1</jv>.parse(<js>"foo=bar,baz=qux\,true\,123"</js>, JsonList.<jk>class</jk>)
		 *
		 * 	<jc>// Parse UON.</jc>
		 * 	JsonList <jv>list2</jv> = <jv>parser2</jv>.parse(<js>"(foo=bar,baz=@(qux,true,123))"</js>, JsonList.<jk>class</jk>)
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#CSV CSV} - (default) Comma-separated values (e.g. <js>"foo,bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#SSV SSV} - Space-separated values (e.g. <js>"foo bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#TSV TSV} - Tab-separated values (e.g. <js>"foo\tbar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#PIPES PIPES} - Pipe-separated values (e.g. <js>"foo|bar"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#MULTI MULTI} - Corresponds to multiple parameter instances instead of multiple values for a single instance (e.g. <js>"foo=bar&amp;foo=baz"</js>).
		 * 		<li class='jf'>{@link HttpPartCollectionFormat#UONC UONC} - UON collection notation (e.g. <js>"@(foo,bar)"</js>).
		 * 	</ul>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder collectionFormat(HttpPartCollectionFormat value) {
			collectionFormat = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * <i><l>OpenApiCommon</l> configuration property:&emsp;</i>  Default format for HTTP parts.
		 *
		 * <p>
		 * Specifies the format to use for HTTP parts when not otherwise specified via {@link Schema#format()}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a plain-text parser.</jc>
		 * 	OpenApiParser <jv>parser1</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.build();
		 *
		 * 	<jc>// Create a UON parser.</jc>
		 * 	OpenApiParser <jv>parser2</jv> = OpenApiParser
		 * 		.<jsm>create</jsm>()
		 * 		.format(<jsf>UON</jsf>)
		 * 		.build();
		 *
		 * 	<jc>// Parse a plain-text string.</jc>
		 * 	String <jv>value1</jv> = <jv>parser1</jv>.parse(<js>"foo bar"</js>);
		 *
		 * 	<jc>// Parse a UON string.</jc>
		 * 	String <jv>value2</jv> = <jv>parser1</jv>.parse(<js>"'foo bar'"</js>);
		 * </p>
		 *
		 * <ul class='values javatree'>
		 * 	<li class='jc'>{@link HttpPartFormat}
		 * 	<ul>
		 * 		<li class='jf'>{@link HttpPartFormat#UON UON} - UON notation (e.g. <js>"'foo bar'"</js>).
		 * 		<li class='jf'>{@link HttpPartFormat#INT32 INT32} - Signed 32 bits.
		 * 		<li class='jf'>{@link HttpPartFormat#INT64 INT64} - Signed 64 bits.
		 * 		<li class='jf'>{@link HttpPartFormat#FLOAT FLOAT} - 32-bit floating point number.
		 * 		<li class='jf'>{@link HttpPartFormat#DOUBLE DOUBLE} - 64-bit floating point number.
		 * 		<li class='jf'>{@link HttpPartFormat#BYTE BYTE} - BASE-64 encoded characters.
		 * 		<li class='jf'>{@link HttpPartFormat#BINARY BINARY} - Hexadecimal encoded octets (e.g. <js>"00FF"</js>).
		 * 		<li class='jf'>{@link HttpPartFormat#BINARY_SPACED BINARY_SPACED} - Spaced-separated hexadecimal encoded octets (e.g. <js>"00 FF"</js>).
		 * 		<li class='jf'>{@link HttpPartFormat#DATE DATE} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 full-date</a>.
		 * 		<li class='jf'>{@link HttpPartFormat#DATE_TIME DATE_TIME} - An <a href='http://xml2rfc.ietf.org/public/rfc/html/rfc3339.html#anchor14'>RFC3339 date-time</a>.
		 * 		<li class='jf'>{@link HttpPartFormat#PASSWORD PASSWORD} - Used to hint UIs the input needs to be obscured.
		 * 		<li class='jf'>{@link HttpPartFormat#NO_FORMAT NO_FORMAT} - (default) Not specified.
		 * 	</ul>
		 * </ul>
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(HttpPartFormat value) {
			format = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				format,
				collectionFormat
			);
			// @formatter:on
		}


	}

	/** Reusable instance of {@link OpenApiParser}. */
	public static final OpenApiParser DEFAULT = new OpenApiParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final HttpPartFormat format;
	final HttpPartCollectionFormat collectionFormat;

	private final Map<ClassMeta<?>,OpenApiClassMeta> openApiClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,OpenApiBeanPropertyMeta> openApiBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public OpenApiParser(Builder builder) {
		super(builder);
		format = builder.format;
		collectionFormat = builder.collectionFormat;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public OpenApiParserSession.Builder createSession() {
		return OpenApiParserSession.create(this);
	}

	@Override /* Overridden from OpenApiMetaProvider */
	public OpenApiBeanPropertyMeta getOpenApiBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return OpenApiBeanPropertyMeta.DEFAULT;
		return openApiBeanPropertyMetas.computeIfAbsent(bpm, k -> new OpenApiBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from OpenApiMetaProvider */
	public OpenApiClassMeta getOpenApiClassMeta(ClassMeta<?> cm) {
		return openApiClassMetas.computeIfAbsent(cm, k -> new OpenApiClassMeta(k, this));
	}

	@Override /* Overridden from HttpPartParser */
	public OpenApiParserSession getPartSession() { return OpenApiParserSession.create(this).build(); }

	@Override /* Overridden from Context */
	public OpenApiParserSession getSession() { return createSession().build(); }

	/**
	 * Returns the default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 *
	 * @return The default collection format to use when not otherwise specified via {@link Schema#collectionFormat()}
	 */
	protected final HttpPartCollectionFormat getCollectionFormat() { return collectionFormat; }

	/**
	 * Returns the default format to use when not otherwise specified via {@link Schema#format()}
	 *
	 * @return The default format to use when not otherwise specified via {@link Schema#format()}
	 */
	protected final HttpPartFormat getFormat() { return format; }
}