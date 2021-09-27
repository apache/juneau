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
package org.apache.juneau.serializer;

import static org.apache.juneau.internal.ExceptionUtils.*;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.internal.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 * {@review}
 */
@ConfigurableContext
public abstract class WriterSerializer extends Serializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "WriterSerializer";

	/**
	 * Configuration property:  File charset.
	 *
	 * <p>
	 * The character set to use for writing <c>Files</c> to the file system.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.WriterSerializer#WSERIALIZER_fileCharset WSERIALIZER_fileCharset}
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.fileCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>WriterSerializer.fileCharset</c>
	 * 	<li><b>Environment variable:</b>  <c>WRITERSERIALIZER_FILECHARSET</c>
	 * 	<li><b>Default:</b>  <js>"DEFAULT"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#fileCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#fileCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String WSERIALIZER_fileCharset = PREFIX + ".fileCharset.s";

	/**
	 * Configuration property:  Maximum indentation.
	 *
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.WriterSerializer#WSERIALIZER_maxIndent WSERIALIZER_maxIndent}
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.maxIndent.i"</js>
	 * 	<li><b>Data type:</b>  <jk>int</jk>
	 * 	<li><b>System property:</b>  <c>WriterSerializer.maxIndent</c>
	 * 	<li><b>Environment variable:</b>  <c>WRITERSERIALIZER_MAXINDENT</c>
	 * 	<li><b>Default:</b>  <c>100</c>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#maxIndent()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#maxIndent(int)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String WSERIALIZER_maxIndent = PREFIX + ".maxIndent.i";

	/**
	 * Configuration property:  Quote character.
	 *
	 * <p>
	 * Specifies the character to use for quoting attributes and values.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.WriterSerializer#WSERIALIZER_quoteChar WSERIALIZER_quoteChar}
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.quoteChar.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>WriterSerializer.quoteChar</c>
	 * 	<li><b>Environment variable:</b>  <c>WRITERSERIALIZER_QUOTECHAR</c>
	 * 	<li><b>Default:</b>  <js>"\""</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#quoteChar()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#quoteChar(char)}
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#sq()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String WSERIALIZER_quoteChar = PREFIX + ".quoteChar.s";

	/**
	 * Allows you to override the quote-char setting.
	 */
	public static final String WSERIALIZER_quoteCharOverride = PREFIX + ".quoteCharOverride.s";

	/**
	 * Configuration property:  Output stream charset.
	 *
	 * <p>
	 * The character set to use when writing to <c>OutputStreams</c>.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.WriterSerializer#WSERIALIZER_streamCharset WSERIALIZER_streamCharset}
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.streamCharset.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>System property:</b>  <c>WriterSerializer.streamCharset</c>
	 * 	<li><b>Environment variable:</b>  <c>WRITERSERIALIZER_STREAMCHARSET</c>
	 * 	<li><b>Default:</b>  <js>"UTF-8"</js>
	 * 	<li><b>Session property:</b>  <jk>false</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#streamCharset()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#streamCharset(Charset)}
	 * 		</ul>
	 * </ul>
	 */
	public static final String WSERIALIZER_streamCharset = PREFIX + ".streamCharset.s";

	/**
	 * Configuration property:  Use whitespace.
	 *
	 * <p>
	 * When enabled, whitespace is added to the output to improve readability.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><b>ID:</b>  {@link org.apache.juneau.serializer.WriterSerializer#WSERIALIZER_useWhitespace WSERIALIZER_useWhitespace}
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.useWhitespace.b"</js>
	 * 	<li><b>Data type:</b>  <jk>boolean</jk>
	 * 	<li><b>System property:</b>  <c>WriterSerializer.useWhitespace</c>
	 * 	<li><b>Environment variable:</b>  <c>WRITERSERIALIZER_USEWHITESPACE</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session property:</b>  <jk>true</jk>
	 * 	<li><b>Annotations:</b>
	 * 		<ul>
	 * 			<li class='ja'>{@link org.apache.juneau.serializer.annotation.SerializerConfig#useWhitespace()}
	 * 		</ul>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#useWhitespace()}
	 * 			<li class='jm'>{@link org.apache.juneau.serializer.WriterSerializerBuilder#ws()}
	 * 		</ul>
	 * </ul>
	 */
	public static final String WSERIALIZER_useWhitespace = PREFIX + ".useWhitespace.b";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Charset fileCharset;
	private final int maxIndent;
	private final char quoteChar;
	private final Charset streamCharset;
	private final boolean useWhitespace;

	private final Character quoteCharRaw;

	/**
	 * Constructor.
	 *
	 * @param builder
	 * 	The builder for this object.
	 */
	protected WriterSerializer(WriterSerializerBuilder builder) {
		super(builder);

		ContextProperties cp = getContextProperties();
		maxIndent = cp.getInteger(WSERIALIZER_maxIndent).orElse(100);
		quoteChar = cp.getString(WSERIALIZER_quoteCharOverride).orElse(cp.getString(WSERIALIZER_quoteChar).orElse("\"")).charAt(0);
		String _quoteCharRaw = cp.getString(WSERIALIZER_quoteCharOverride).orElse(cp.getString(WSERIALIZER_quoteChar).orElse(null));
		quoteCharRaw = _quoteCharRaw == null ? null : _quoteCharRaw.charAt(0);
		streamCharset = cp.get(WSERIALIZER_streamCharset, Charset.class).orElse(IOUtils.UTF8);
		fileCharset = cp.get(WSERIALIZER_fileCharset, Charset.class).orElse(Charset.defaultCharset());
		useWhitespace = cp.getBoolean(WSERIALIZER_useWhitespace).orElse(false);
	}

	@Override
	public abstract WriterSerializerBuilder copy();

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract WriterSerializerSession createSession(SerializerSessionArgs args);

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public WriterSerializerSession createSession() {
		return createSession(createDefaultSessionArgs());
	}

	@Override /* Serializer */
	public final boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Convenience method for serializing an object to a <c>String</c>.
	 *
	 * @param o The object to serialize.
	 * @return The output serialized to a string.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	@Override /* Serializer */
	public final String serialize(Object o) throws SerializeException {
		return createSession(createDefaultSessionArgs()).serialize(o);
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
			throw runtimeException(e);
		}
	}

	/**
	 * Convenience method for serializing an object and sending it to STDOUT.
	 *
	 * @param o The object to serialize.
	 * @return This object (for method chaining).
	 */
	public final WriterSerializer println(Object o) {
		System.out.println(toString(o));  // NOT DEBUG
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Properties
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * File charset.
	 *
	 * @see #WSERIALIZER_fileCharset
	 * @return
	 * 	The character set to use when writing to <c>Files</c> on the file system.
	 */
	protected final Charset getFileCharset() {
		return fileCharset;
	}

	/**
	 * Maximum indentation.
	 *
	 * @see #WSERIALIZER_maxIndent
	 * @return
	 * 	The maximum indentation level in the serialized document.
	 */
	protected final int getMaxIndent() {
		return maxIndent;
	}

	/**
	 * Quote character.
	 *
	 * @see #WSERIALIZER_quoteChar
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected char getQuoteChar() {
		return quoteChar;
	}

	/**
	 * Quote character.
	 *
	 * @see #WSERIALIZER_quoteChar
	 * @return
	 * 	The character used for quoting attributes and values.
	 */
	protected Character quoteChar() {
		return quoteCharRaw;
	}

	/**
	 * Output stream charset.
	 *
	 * @see #WSERIALIZER_streamCharset
	 * @return
	 * 	The character set to use when writing to <c>OutputStreams</c> and byte arrays.
	 */
	protected final Charset getStreamCharset() {
		return streamCharset;
	}

	/**
	 * Trim strings.
	 *
	 * @see #WSERIALIZER_useWhitespace
	 * @return
	 * 	When enabled, whitespace is added to the output to improve readability.
	 */
	protected final boolean isUseWhitespace() {
		return useWhitespace;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Context */
	public OMap toMap() {
		return super.toMap()
			.a(
				"WriterSerializer",
				OMap
					.create()
					.filtered()
					.a("fileCharset", fileCharset)
					.a("maxIndent", maxIndent)
					.a("quoteChar", quoteChar)
					.a("streamCharset", streamCharset)
					.a("useWhitespace", useWhitespace)
			);
	}
}
