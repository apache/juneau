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
 * Static literals for {@code juneau-rest-server}: annotation attribute name constants used for
 * allowlist inheritance checks and other module-internal logic.
 *
 * <p>
 * HTTP wire names shared with clients belong on {@link RestSharedConstants} instead.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link RestSharedConstants}
 * </ul>
 */
@SuppressWarnings({
	"java:S115", // PROPERTY_ + camelCase property name mirrors annotation attribute name; not strict UPPER_SNAKE_CASE
})
public final class RestServerConstants {

	private RestServerConstants() {}

	/**
	 * The {@code "allowedParserOptions"} annotation attribute name — used in {@code noInherit} matching.
	 *
	 * @see org.apache.juneau.rest.annotation.Rest#allowedParserOptions()
	 */
	public static final String PROPERTY_allowedParserOptions = "allowedParserOptions";

	/**
	 * The {@code "allowedSerializerOptions"} annotation attribute name — used in {@code noInherit} matching.
	 *
	 * @see org.apache.juneau.rest.annotation.Rest#allowedSerializerOptions()
	 */
	public static final String PROPERTY_allowedSerializerOptions = "allowedSerializerOptions";

	/** The {@code "noInherit"} annotation attribute name. */
	public static final String PROPERTY_noInherit = "noInherit";
}
