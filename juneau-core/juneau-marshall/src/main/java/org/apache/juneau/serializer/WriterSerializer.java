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

import org.apache.juneau.*;
import org.apache.juneau.utils.*;

/**
 * Subclass of {@link Serializer} for character-based serializers.
 */
public abstract class WriterSerializer extends Serializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	private static final String PREFIX = "WriterSerializer.";

	/**
	 * Configuration property:  Maximum indentation.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.maxIndent.i"</js>
	 * 	<li><b>Data type:</b>  <code>Integer</code>
	 * 	<li><b>Default:</b>  <code>100</code>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link WriterSerializerBuilder#maxIndent(int)}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the maximum indentation level in the serialized document.
	 * 
	 * <p>
	 * This setting does not apply to the RDF serializers.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that indents a maximum of 20 tabs.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.maxIndent(20)
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>SERIALIZER_maxIndent</jsf>, 20)
	 * 		.build();
	 * </p>
	 */
	public static final String WSERIALIZER_maxIndent = PREFIX + "maxIndent.i";

	/**
	 * Configuration property:  Quote character.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.quoteChar.s"</js>
	 * 	<li><b>Data type:</b>  <code>String</code>
	 * 	<li><b>Default:</b>  <js>"\""</js>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link WriterSerializerBuilder#quoteChar(char)}
	 * 			<li class='jm'>{@link WriterSerializerBuilder#sq()}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * This is the character used for quoting attributes and values.
	 * 
	 * <p>
	 * This setting does not apply to the RDF serializers.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer that uses single quotes.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.sq()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>WSERIALIZER_quoteChar</jsf>, <js>'\''</js>)
	 * 		.build();
	 * </p>
	 */
	public static final String WSERIALIZER_quoteChar = PREFIX + "quoteChar.s";

	/**
	 * Configuration property:  Use whitespace.
	 * 
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"WriterSerializer.useWhitespace.b"</js>
	 * 	<li><b>Data type:</b>  <code>Boolean</code>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Session-overridable:</b>  <jk>true</jk>
	 * 	<li><b>Methods:</b> 
	 * 		<ul>
	 * 			<li class='jm'>{@link WriterSerializerBuilder#useWhitespace(boolean)}
	 * 			<li class='jm'>{@link WriterSerializerBuilder#useWhitespace()}
	 * 			<li class='jm'>{@link WriterSerializerBuilder#ws()}
	 * 		</ul>
	 * </ul>
	 * 
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * If <jk>true</jk>, whitespace is added to the output to improve readability.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jc>// Create a serializer with whitespace enabled.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.ws()
	 * 		.build();
	 * 	
	 * 	<jc>// Same, but use property.</jc>
	 * 	WriterSerializer s = JsonSerializer
	 * 		.<jsm>create</jsm>()
	 * 		.set(<jsf>WSERIALIZER_useWhitespace</jsf>, <jk>true</jk>)
	 * 		.build();
	 * 
	 * 	<jc>// Produces "\{\n\t'foo': 'bar'\n\}\n"</jc>
	 * 	String json = s.serialize(<jk>new</jk> MyBean());
	 * </p>
	 */
	public static final String WSERIALIZER_useWhitespace = PREFIX + "useWhitespace.b";

	static final WriterSerializer DEFAULT = new WriterSerializer(PropertyStore.create().build(), "", "") {
		@Override
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			throw new NoSuchMethodError();
		}
	};

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------
	
	final int maxIndent;
	final boolean useWhitespace;
	final char quoteChar;

	/**
	 * Constructor.
	 * 
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @param produces
	 * 	The media type that this serializer produces.
	 * @param accept
	 * 	The accept media types that the serializer can handle.
	 * 	<p>
	 * 	Can contain meta-characters per the <code>media-type</code> specification of
	 * 	<a class="doclink" href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1">RFC2616/14.1</a>
	 * 	<p>
	 * 	If empty, then assumes the only media type supported is <code>produces</code>.
	 * 	<p>
	 * 	For example, if this serializer produces <js>"application/json"</js> but should handle media types of
	 * 	<js>"application/json"</js> and <js>"text/json"</js>, then the arguments should be:
	 * 	<p class='bcode'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"application/json,text/json"</js>);
	 * 	</p>
	 * 	<br>...or...
	 * 	<p class='bcode'>
	 * 	<jk>super</jk>(ps, <js>"application/json"</js>, <js>"*&#8203;/json"</js>);
	 * 	</p>
	 * <p>
	 * The accept value can also contain q-values.
	 */
	protected WriterSerializer(PropertyStore ps, String produces, String accept) {
		super(ps, produces, accept);
		
		useWhitespace = getBooleanProperty(WSERIALIZER_useWhitespace, false);
		maxIndent = getIntegerProperty(WSERIALIZER_maxIndent, 100);
		quoteChar = getStringProperty(WSERIALIZER_quoteChar, "\"").charAt(0);
	}


	//--------------------------------------------------------------------------------
	// Abstract methods
	//--------------------------------------------------------------------------------

	@Override /* SerializerSession */
	public abstract WriterSerializerSession createSession(SerializerSessionArgs args);


	//--------------------------------------------------------------------------------
	// Other methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public final boolean isWriterSerializer() {
		return true;
	}

	/**
	 * Convenience method for serializing an object to a <code>String</code>.
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
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wraps the specified object inside a {@link StringObject}.
	 * 
	 * @param o The object to wrap.
	 * @return The wrapped object.
	 */
	public final StringObject toStringObject(Object o) {
		return new StringObject(this, o);
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
	
	@Override /* Context */
	public ObjectMap asMap() {
		return super.asMap()
			.append("WriterSerializer", new ObjectMap()
				.append("useWhitespace", useWhitespace)
				.append("maxIndent", maxIndent)
				.append("quoteChar", quoteChar)
			);
	}
}
