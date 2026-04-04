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
package org.apache.juneau.rest;

/**
 * Static constants shared across Juneau REST modules (for example {@code juneau-rest-server},
 * {@code juneau-rest-client}, and {@code juneau-rest-mock}) so wire names and other cross-tier
 * strings stay aligned.
 *
 * <p>
 * Add more {@code public static final} fields here when server and client (or other REST modules)
 * must agree on the same literal values. Server-only constants belong in
 * {@code RestServerConstants} in {@code juneau-rest-server}.
 */
@SuppressWarnings({
	"java:S115", // Names use HEADER_/QUERY_ + camelCase to mirror wire identifiers; not strict UPPER_SNAKE_CASE
})
public final class RestSharedConstants {

	private RestSharedConstants() {}

	/**
	 * Serializer session options request header (JSON5 object format).
	 *
	 * <p>
	 * HTTP header names are case-insensitive; use this spelling in OpenAPI and examples.
	 * Example value: <js>{escapeSolidus:true,maxIndent:4}</js>
	 */
	public static final String HEADER_JuneauSerializerOptions = "X-Juneau-Serializer-Options";

	/**
	 * Parser session options request header (JSON5 object format).
	 *
	 * <p>
	 * HTTP header names are case-insensitive; use this spelling in OpenAPI and examples.
	 * Example value: <js>{trimStrings:true}</js>
	 */
	public static final String HEADER_JuneauParserOptions = "X-Juneau-Parser-Options";

	/** Serializer session options query parameter (UON-encoded map format). Example: <js>(maxIndent=4,sortMaps=true)</js> */
	public static final String QUERY_juneauSerializerOptions = "juneauSerializerOptions";

	/** Parser session options query parameter (UON-encoded map format). Example: <js>(trimStrings=true)</js> */
	public static final String QUERY_juneauParserOptions = "juneauParserOptions";
}
