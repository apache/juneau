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
package org.apache.juneau.json;

/**
 * Serializes POJO models to Simplified JSON.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>application/json, text/json</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>application/json5</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * 	This is a JSON serializer that uses simplified notation:
 * <ul class='spaced-list'>
 * 	<li>Lax quoting of JSON attribute names.
 * 	<li>Single quotes.
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.JsonDetails">JSON Details</a>
 * </ul>
 */
public class Json5Serializer extends JsonSerializer {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes, {@link JsonSerializer.Builder#simpleAttrs() simple mode}. */
	public static final Json5Serializer DEFAULT = new Json5Serializer(create());

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static final Json5Serializer DEFAULT_READABLE = new Readable(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static JsonSerializer.Builder create() {
		return JsonSerializer.create().simpleAttrs().quoteChar('\'').produces("application/json5").accept("application/json5,text/json5,application/json;q=0.9,text/json;q=0.9").type(Json5Serializer.class);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Static subclasses
	//-------------------------------------------------------------------------------------------------------------------

	/** Default serializer, single quotes, simple mode, with whitespace. */
	public static class Readable extends Json5Serializer {

		/**
		 * Constructor.
		 *
		 * @param builder The builder for this object.
		 */
		public Readable(JsonSerializer.Builder builder) {
			super(builder.simpleAttrs().quoteChar('\'').useWhitespace());
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public Json5Serializer(JsonSerializer.Builder builder) {
		super(builder.simpleAttrs().quoteChar('\''));
	}

	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}
}