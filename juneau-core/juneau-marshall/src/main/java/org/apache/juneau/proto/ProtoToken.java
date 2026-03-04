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
package org.apache.juneau.proto;

/**
 * Token produced by {@link ProtoTokenizer} for Protobuf Text Format parsing.
 *
 * <p>
 * Holds a {@link TokenType} and an optional value for tokens that carry data
 * (IDENT, STRING, DEC_INT, OCT_INT, HEX_INT, FLOAT, boolean identifiers).
 *
 * @param type The token type.
 * @param value The token value (<jk>null</jk> for structural tokens).
 */
public record ProtoToken(ProtoToken.TokenType type, Object value) {

	/**
	 * Token type enumeration.
	 */
	public enum TokenType {
		/** Bare identifier (field name or enum value). */
		IDENT,
		/** Quoted string (single or double). */
		STRING,
		/** Decimal integer. */
		DEC_INT,
		/** Octal integer (0...). */
		OCT_INT,
		/** Hexadecimal integer (0x...). */
		HEX_INT,
		/** Float literal. */
		FLOAT,
		/** Left brace { */
		LBRACE,
		/** Right brace } */
		RBRACE,
		/** Left angle bracket &lt; */
		LANGLE,
		/** Right angle bracket &gt; */
		RANGLE,
		/** Left bracket [ */
		LBRACKET,
		/** Right bracket ] */
		RBRACKET,
		/** Colon : */
		COLON,
		/** Comma , */
		COMMA,
		/** Semicolon ; */
		SEMICOLON,
		/** End of input. */
		EOF
	}

	/**
	 * Returns the string value for IDENT or STRING tokens.
	 *
	 * @return The string value.
	 */
	public String stringValue() {
		return value == null ? null : value.toString();
	}

	/**
	 * Returns the numeric value for integer tokens.
	 *
	 * @return The number.
	 */
	public Number numberValue() {
		return (Number) value;
	}

	/**
	 * Returns the boolean value (for true/false identifiers).
	 *
	 * @return The boolean.
	 */
	public Boolean booleanValue() {
		return (Boolean) value;
	}
}
