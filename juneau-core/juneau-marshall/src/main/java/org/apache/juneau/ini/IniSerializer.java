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
package org.apache.juneau.ini;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to INI format.
 *
 * <p>
 * Converts Java POJOs to INI format (section/key-value structure).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Produces: <bc>text/ini</bc>
 * <br>Accepts: <bc>text/ini, text/x-ini</bc>
 *
 * <h5 class='topic'>Bean-to-INI mapping</h5>
 * <ul class='spaced-list'>
 * 	<li>Simple properties → key-value pairs in the default section
 * 	<li>Nested beans → <c>[sectionName]</c> sections
 * 	<li>Deeply nested beans → <c>[path/to/section]</c> section paths
 * 	<li>Collections and complex values → JSON5-encoded inline values
 * 	<li>Maps with string keys → <c>[sectionName]</c> sections
 * 	<li>Null values → unquoted <c>null</c> token
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Serialize a bean to INI</jc>
 * 	String <jv>ini</jv> = IniSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>myBean</jv>);
 *
 * 	<jc>// Create a custom serializer</jc>
 * 	IniSerializer <jv>s</jv> = IniSerializer.<jsm>create</jsm>().useComments().build();
 * 	<jv>ini</jv> = <jv>s</jv>.serialize(<jv>myBean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (Map of name/age):</h5>
 * <p class='bini'>
 * 	<ck>name</ck> = <cv>Alice</cv>
 * 	<ck>age</ck> = <cv>30</cv>
 * </p>
 *
 * <h5 class='figure'>Complex (nested object + array):</h5>
 * <p class='bini'>
 * 	<ck>name</ck> = <cv>Alice</cv>
 * 	<ck>age</ck> = <cv>30</cv>
 * 	<ck>tags</ck> = <cv>['a','b','c']</cv>
 *
 * 	<cs>[address]</cs>
 * 	<ck>street</ck> = <cv>123 Main St</cv>
 * 	<ck>city</ck> = <cv>Boston</cv>
 * 	<ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='topic'>Limitations</h5>
 * <p>
 * <ul class='spaced-list'>
 * 	<li>Top-level collections, arrays, and scalar values are not supported. The root must be a bean or
 * 		<c>Map&lt;String,?&gt;</c>. Throws {@link SerializeException} for unsupported root types.
 * 	<li>{@link java.io.Reader} and {@link java.io.InputStream} passthrough is not supported.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * 	<li class='note'>Complex values (collections, maps with complex values) are embedded as JSON5 inline
 * 		strings and remain fully round-trippable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class IniSerializer extends WriterSerializer implements IniMetaProvider {

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, IniClassMeta> iniClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, IniBeanPropertyMeta> iniBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	private static final String PROP_kvSeparator = "kvSeparator";
	private static final String PROP_spacedSeparator = "spacedSeparator";
	private static final String PROP_useComments = "useComments";
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends WriterSerializer.Builder {

		private static final Cache<HashKey,IniSerializer> CACHE = Cache.of(HashKey.class, IniSerializer.class).build();

		private char kvSeparator = '=';
		private boolean spacedSeparator = true;
		private boolean useComments = false;

		protected Builder() {
			produces("text/ini");
			accept("text/ini,text/x-ini");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			kvSeparator = copyFrom.kvSeparator;
			spacedSeparator = copyFrom.spacedSeparator;
			useComments = copyFrom.useComments;
		}

		protected Builder(IniSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			kvSeparator = copyFrom.kvSeparator;
			spacedSeparator = copyFrom.spacedSeparator;
			useComments = copyFrom.useComments;
		}

		/**
		 * Key-value separator character.
		 *
		 * @param value <c>=</c> (default) or <c>:</c>.
		 * @return This object.
		 */
		public Builder kvSeparator(char value) {
			kvSeparator = value;
			return this;
		}

		/**
		 * Whether to add spaces around the separator.
		 *
		 * @param value <jk>true</jk> for <c>key = value</c>, <jk>false</jk> for <c>key=value</c>.
		 * @return This object.
		 */
		public Builder spacedSeparator(boolean value) {
			spacedSeparator = value;
			return this;
		}

		/**
		 * Whether to emit <c>#</c> comments from bean property descriptions.
		 *
		 * @param value The flag.
		 * @return This object.
		 */
		public Builder useComments(boolean value) {
			useComments = value;
			return this;
		}

		/**
		 * Emit <c>#</c> comments from bean property descriptions.
		 *
		 * @return This object.
		 */
		public Builder useComments() {
			useComments = true;
			return this;
		}

		@Override
		public Builder useWhitespace() {
			super.useWhitespace();
			return this;
		}

		@Override
		public Builder useWhitespace(boolean value) {
			super.useWhitespace(value);
			return this;
		}

		@Override
		public Builder ws() {
			return useWhitespace();
		}

		@Override
		public IniSerializer build() {
			return cache(CACHE).build(IniSerializer.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override
		public HashKey hashKey() {
			return HashKey.of(super.hashKey(), kvSeparator, spacedSeparator, useComments);
		}
	}

	/** Default serializer. */
	public static final IniSerializer DEFAULT = new IniSerializer(create());

	/** Default serializer with blank lines between sections. */
	public static final IniSerializer DEFAULT_READABLE = new IniSerializer(create().ws());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/** Key-value separator. */
	protected final char kvSeparator;

	/** Whether to add spaces around the separator. */
	protected final boolean spacedSeparator;

	/** Whether to emit property descriptions as comments. */
	protected final boolean useComments;

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public IniSerializer(Builder builder) {
		super(builder);
		kvSeparator = builder.kvSeparator;
		spacedSeparator = builder.spacedSeparator;
		useComments = builder.useComments;
	}

	@Override
	public IniSerializerSession.Builder createSession() {
		return IniSerializerSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}

	@Override
	public IniBeanPropertyMeta getIniBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return IniBeanPropertyMeta.DEFAULT;
		return iniBeanPropertyMetas.computeIfAbsent(bpm, k -> new IniBeanPropertyMeta(k, this));
	}

	@Override
	public IniClassMeta getIniClassMeta(ClassMeta<?> cm) {
		return iniClassMetas.computeIfAbsent(cm, k -> new IniClassMeta(k, this));
	}

	@Override
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_kvSeparator, String.valueOf(kvSeparator))
			.a(PROP_spacedSeparator, spacedSeparator)
			.a(PROP_useComments, useComments);
	}
}
