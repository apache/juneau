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
 * Parses any valid JSON text into a POJO model.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Content-Type</c> types:  <bc>application/json+simple, text/json+simple</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Identical to {@link JsonParser} but with the media type <bc>application/json+simple</bc>.
 *
 * <ul class='notes'>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jm.JsonDetails}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class SimpleJsonParser extends JsonParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	/** Default parser, Accept=application/json+simple. */
	public static final SimpleJsonParser DEFAULT = new SimpleJsonParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static JsonParser.Builder create() {
		return JsonParser.create().consumes("application/json+simple,text/json+simple,application/json,text/json");
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public SimpleJsonParser(JsonParser.Builder builder) {
		super(builder);
	}

	@Override /* Context */
	public JsonParser.Builder copy() {
		return new JsonParser.Builder(this);
	}
}