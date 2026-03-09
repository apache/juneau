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
package org.apache.juneau.json;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.serializer.*;

/**
 * Serializes POJO models to canonical JSON per RFC 8785 (JCS).
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * 	Produces media type: <bc>application/jcs+json</bc>
 * </p>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * 	JCS (JSON Canonicalization Scheme) produces a deterministic, byte-for-byte canonical
 * 	representation of JSON, enabling reliable cryptographic operations such as hashing and
 * 	digital signing. All canonicalization rules are defined in
 * 	<a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785</a>.
 * </p>
 * <ul class='spaced-list'>
 * 	<li>No whitespace between tokens.
 * 	<li>Object properties sorted by UTF-16 code unit order, applied recursively to all nested objects.
 * 	<li>Numbers serialized using ECMAScript-compatible rules: shortest round-trip representation,
 * 	    no trailing zeros, lowercase <code>e</code>, positive exponent sign included (e.g. <code>1e+30</code>).
 * 	<li>Negative zero serialized as <code>0</code>.
 * 	<li>Non-ASCII characters emitted as literal UTF-8 bytes (no unnecessary unicode escape sequences).
 * 	<li>Array element order is preserved (not sorted).
 * </ul>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Create a bean.</jc>
 * 	MyBean <jv>bean</jv> = <jk>new</jk> MyBean().name(<js>"Alice"</js>).age(30);
 *
 * 	<jc>// Serialize to canonical JSON.</jc>
 * 	String <jv>json</jv> = JcsSerializer.<jsf>DEFAULT</jsf>.serialize(<jv>bean</jv>);
 *
 * 	<jc>// Or use the Jcs marshaller convenience method.</jc>
 * 	String <jv>json</jv> = Jcs.<jsm>of</jsm>(<jv>bean</jv>);
 * </p>
 *
 * <h5 class='figure'>Example output (bean with name/age):</h5>
 * <p class='bjson'>
 * 	{<jok>"age"</jok>:<jov>30</jov>,<jok>"name"</jok>:<jov>"Alice"</jov>}
 * </p>
 *
 * <h5 class='figure'>Example output with nested object:</h5>
 * <p class='bjson'>
 * 	{<jok>"address"</jok>:{<jok>"city"</jok>:<jov>"Denver"</jov>,<jok>"zip"</jok>:<jov>"80201"</jov>},<jok>"name"</jok>:<jov>"Alice"</jov>}
 * </p>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * 	<li class='note'>
 * 		JCS output is valid JSON and can be parsed using the standard {@link JsonParser}.
 * 		The {@link Jcs} marshaller pairs this serializer with {@link JsonParser} for full round-trip support.
 * 	<li class='note'>
 * 		{@link java.math.BigDecimal} and {@link java.math.BigInteger} values beyond IEEE 754 double
 * 		precision range will lose precision or throw a {@link SerializeException}.
 * 		This is a spec-level constraint defined by RFC 8785.
 * 	<li class='note'>
 * 		{@link Double#NaN}, {@link Double#POSITIVE_INFINITY}, and {@link Double#NEGATIVE_INFINITY}
 * 		are not permitted and will throw a {@link SerializeException}.
 * 	<li class='note'>
 * 		Strings containing lone UTF-16 surrogate code units will throw {@link SerializeException}.
 * 	<li class='note'>
 * 		Uses <code>application/jcs+json</code> to avoid content-negotiation collision with
 * 		{@link JsonSerializer} (<code>application/json</code>). Request explicitly via
 * 		<c>Accept: application/jcs+json</c> for canonical output.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://www.rfc-editor.org/rfc/rfc8785">RFC 8785 — JSON Canonicalization Scheme</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Jcs">JCS topic</a>
 * 	<li class='jc'>{@link Jcs}
 * 	<li class='jc'>{@link JsonParser}
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class JcsSerializer extends JsonSerializer {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends JsonSerializer.Builder {

		/**
		 * Constructor, default settings for JCS canonical output.
		 */
		protected Builder() {
			super();
			produces("application/jcs+json");
			accept("application/jcs+json");
			sortProperties();
			quoteChar('"');
			simpleAttrs(false);
			useWhitespace(false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The serializer to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(JcsSerializer copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Overridden from Context.Builder */
		public JcsSerializer build() {
			return new JcsSerializer(this);
		}
	}

	/** Default serializer. */
	public static final JcsSerializer DEFAULT = new JcsSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public JcsSerializer(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from JsonSerializer */
	public JcsSerializerSession.Builder createSession() {
		return JcsSerializerSession.create(this);
	}
}
