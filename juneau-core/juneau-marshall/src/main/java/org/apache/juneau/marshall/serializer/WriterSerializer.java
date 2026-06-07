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
package org.apache.juneau.marshall.serializer;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.nio.charset.*;

import org.apache.juneau.commons.collections.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.marshall.json5.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class WriterSerializer extends Serializer {

	// Property name constants
	private static final String PROP_fileCharset = "fileCharset";
	private static final String PROP_maxIndent = "maxIndent";
	private static final String PROP_quoteChar = "quoteChar";
	private static final String PROP_streamCharset = "streamCharset";
	private static final String PROP_useWhitespace = "useWhitespace";

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends Serializer.Builder<SELF> {

		private boolean useWhitespace;
		private Character quoteChar;
		private Character quoteCharOverride;
		private Charset fileCharset;
		private Charset streamCharset;
		private int maxIndent;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			fileCharset = Charset.defaultCharset();
			streamCharset = UTF8;
			maxIndent = env("WriterSerializer.maxIndent", 100);
			quoteChar = env("WriterSerializer.quoteChar").map(x -> (!x.isEmpty() ? x.charAt(0) : null)).orElse(null);
			quoteCharOverride = env("WriterSerializer.quoteCharOverride").map(x -> (!x.isEmpty() ? x.charAt(0) : null)).orElse(null);
			useWhitespace = env("WriterSerializer.useWhitespace", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			fileCharset = copyFrom.fileCharset;
			streamCharset = copyFrom.streamCharset;
			maxIndent = copyFrom.maxIndent;
			quoteChar = copyFrom.quoteChar;
			quoteCharOverride = copyFrom.quoteCharOverride;
			useWhitespace = copyFrom.useWhitespace;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(WriterSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
			fileCharset = copyFrom.getFileCharset();
			streamCharset = copyFrom.getStreamCharset();
			maxIndent = copyFrom.maxIndent;
			quoteChar = copyFrom.quoteChar;
			quoteCharOverride = copyFrom.quoteCharOverride;
			useWhitespace = copyFrom.useWhitespace;
		}

		@Override /* Overridden from Context.Builder<?> */
		public WriterSerializer build() {
			return build(WriterSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();

		/**
		 * File charset.
		 *
		 * <p>
		 * The character set to use for writing <c>Files</c> to the file system.
		 *
		 * <p>
		 * Used when passing in files to {@link Serializer#serialize(Object, Object)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that writes UTF-8 files.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.fileCharset(Charset.<jsm>forName</jsm>(<js>"UTF-8"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Use it to read a UTF-8 encoded file.</jc>
		 * 	<jv>serializer</jv>.serialize(<jk>new</jk> File(<js>"MyBean.txt"</js>), <jv>myBean</jv>);
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the system JVM setting.
		 * 	<br>Can be <jk>null</jk> (defaults to system default).
		 * @return This object.
		 */
		public SELF fileCharset(Charset value) {
			fileCharset = value;
			return self();
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				fileCharset,
				streamCharset,
				maxIndent,
				quoteChar,
				quoteCharOverride,
				useWhitespace
			);
			// @formatter:on
		}

		/**
		 * Maximum indentation.
		 *
		 * <p>
		 * Specifies the maximum indentation level in the serialized document.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that indents a maximum of 20 tabs.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ws()  <jc>// Enable whitespace</jc>
		 * 		.maxIndent(20)
		 * 		.build();
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <c>100</c>.
		 * @return This object.
		 */
		public SELF maxIndent(int value) {
			maxIndent = value;
			return self();
		}

		/**
		 *  Quote character.
		 *
		 * <p>
		 * Specifies the character to use for quoting attributes and values.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that uses single quotes.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.quoteChar(<js>'\''</js>)
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Produces {'foo':'bar'}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.toString(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <js>'"'</js>.
		 * @return This object.
		 */
		public SELF quoteChar(char value) {
			quoteChar = value;
			return self();
		}

		/**
		 * Quote character override.
		 *
		 * <p>
		 * Similar to {@link #quoteChar(char)} but takes precedence over that setting.
		 *
		 * <p>
		 * Allows you to override the quote character even if it's set by a subclass such as {@link Json5Serializer}.
		 *
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is <jk>null</jk>.
		 * @return This object.
		 */
		public SELF quoteCharOverride(char value) {
			quoteCharOverride = value;
			return self();
		}

		/**
		 *  Quote character.
		 *
		 * <p>
		 * Specifies to use single quotes for quoting attributes and values.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>This setting does not apply to the RDF serializers.
		 * </ul>
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that uses single quotes.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.sq()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Produces {'foo':'bar'}</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.toString(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF sq() {
			return quoteChar('\'');
		}

		/**
		 * Output stream charset.
		 *
		 * <p>
		 * The character set to use when writing to <c>OutputStreams</c>.
		 *
		 * <p>
		 * Used when passing in output streams and byte arrays to {@link WriterSerializer#serialize(Object, Object)}.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer that writes UTF-8 files.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.streamCharset(Charset.<jsm>forName</jsm>(<js>"UTF-8"</js>))
		 * 		.build();
		 *
		 * 	<jc>// Use it to write to a UTF-8 encoded output stream.</jc>
		 * 	<jv>serializer</jv>.serializer(<jk>new</jk> FileOutputStreamStream(<js>"MyBean.txt"</js>), <jv>myBean</jv>);
		 * </p>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the system JVM setting.
		 * 	<br>Can be <jk>null</jk> (defaults to UTF-8).
		 * @return This object.
		 */
		public SELF streamCharset(Charset value) {
			streamCharset = value;
			return self();
		}


		/**
		 *  Use whitespace.
		 *
		 * <p>
		 * When enabled, whitespace is added to the output to improve readability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with whitespace enabled.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.useWhitespace()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Produces "\{\n\t"foo": "bar"\n\}\n"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF useWhitespace() {
			return useWhitespace(true);
		}

		/**
		 * Same as {@link #useWhitespace()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public SELF useWhitespace(boolean value) {
			useWhitespace = value;
			return self();
		}

		/**
		 *  Use whitespace.
		 *
		 * <p>
		 * When enabled, whitespace is added to the output to improve readability.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Create a serializer with whitespace enabled.</jc>
		 * 	WriterSerializer <jv>serializer</jv> = JsonSerializer
		 * 		.<jsm>create</jsm>()
		 * 		.ws()
		 * 		.build();
		 *
		 * 	<jc>// A bean with a single property</jc>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String <jf>foo</jf> = <js>"bar"</js>;
		 * 	}
		 *
		 * 	<jc>// Produces "\{\n\t"foo": "bar"\n\}\n"</jc>
		 * 	String <jv>json</jv> = <jv>serializer</jv>.serialize(<jk>new</jk> MyBean());
		 * </p>
		 *
		 * @return This object.
		 */
		public SELF ws() {
			return useWhitespace();
		}
	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link WriterSerializer#create()} / {@link WriterSerializer#copy()} path.
	 */
	@SuppressWarnings({
		"java:S110" // Inheritance depth follows the serializer builder chain; intentional layered design
	})
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(WriterSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	protected final boolean useWhitespace;
	protected final Character quoteChar;
	protected final Character quoteCharOverride;
	private final Charset fileCharset;
	private final Charset streamCharset;
	protected final int maxIndent;

	private final char quoteCharValue;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected WriterSerializer(Builder<?> builder) {
		super(builder);

		fileCharset = builder.fileCharset;
		maxIndent = builder.maxIndent;
		quoteChar = builder.quoteChar;
		quoteCharOverride = builder.quoteCharOverride;
		streamCharset = builder.streamCharset;
		useWhitespace = builder.useWhitespace;

		if (nn(quoteCharOverride)) {
			quoteCharValue = quoteCharOverride;
		} else if (nn(quoteChar)) {
			quoteCharValue = quoteChar;
		} else {
			quoteCharValue = '"';
		}
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public WriterSerializerSession.Builder<?> createSession() {
		return WriterSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public WriterSerializerSession getSession() { return createSession().build(); }

	@Override /* Overridden from Serializer */
	public final boolean isWriterSerializer() { return true; }

	/**
	 * Convenience method for serializing an object and sending it to STDOUT.
	 *
	 * @param o The object to serialize.
	 * @return This object.
	 */
	public final WriterSerializer println(Object o) {
		Logger.getLogger(WriterSerializer.class).info(toString(o));  // NOT DEBUG
		return this;
	}

	/**
	 * Convenience method for serializing an object to a <c>String</c>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Overridden from Serializer */
	public final String serialize(Object o) throws SerializeException {
		return getSession().serialize(o);
	}

	/**
	 * Identical to {@link #serialize(Object)} except throws a {@link RuntimeException} instead of a {@link SerializeException}.
	 *
	 * <p>
	 * This is typically good enough for debugging purposes.
	 *
	 * @param o The object to serialize.
	 * @return The serialized object.
	 */
	public final String toString(Object o) {
		try {
			return serialize(o);
		} catch (Exception e) {
			throw toRex(e);
		}
	}

	/**
	 * File charset.
	 *
	 * @see Builder#fileCharset(Charset)
	 * @return
	 * 	The character set to use when writing to <c>Files</c> on the file system.
	 */
	protected final Charset getFileCharset() { return fileCharset; }

	/**
	 * Maximum indentation.
	 *
	 * @see Builder#maxIndent(int)
	 * @return
	 * 	The maximum indentation level in the serialized document.
	 */
	protected final int getMaxIndent() { return maxIndent; }

	/**
	 * Quote character.
	 *
	 * @see Builder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected char getQuoteChar() { return quoteCharValue; }

	/**
	 * Output stream charset.
	 *
	 * @see Builder#streamCharset(Charset)
	 * @return
	 * 	The character set to use when writing to <c>OutputStreams</c> and byte arrays.
	 */
	protected final Charset getStreamCharset() { return streamCharset; }

	/**
	 * Trim strings.
	 *
	 * @see Builder#useWhitespace()
	 * @return
	 * 	When enabled, whitespace is added to the output to improve readability.
	 */
	protected final boolean isUseWhitespace() { return useWhitespace; }

	@Override /* Overridden from Serializer */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_fileCharset, fileCharset)
			.a(PROP_maxIndent, maxIndent)
			.a(PROP_quoteChar, quoteChar)
			.a(PROP_streamCharset, streamCharset)
			.a(PROP_useWhitespace, useWhitespace);
	}

	/**
	 * Quote character.
	 *
	 * @see Builder#quoteChar(char)
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	@SuppressWarnings({
		"java:S1845" // Method name intentionally matches field name
	})
	protected Character quoteChar() {
		return nn(quoteCharOverride) ? quoteCharOverride : quoteChar;
	}
}