/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.juneau.examples.core.json;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.json.*;

/**
 * Json configuration example.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class JsonConfigurationExample {

	/**
	 * Examples on Json Serializers configured using properties
	 * defined in JsonSerializer class
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {
		Pojo aPojo = new Pojo("a","</pojo>");
		// Json Serializers can be configured using properties defined in JsonSerializer
		/**
		 * Produces
		 * {
		 * 	"name": "</pojo>",
		 * 	"id": "a"
		 * }
		 */
		String withWhitespace = JsonSerializer.create().ws().build().serialize(aPojo);
		// the output will be padded with spaces after format characters
		System.out.println(withWhitespace);

		/**
		 * Produces
		 * {"name":"<\/pojo>","id":"a"}
		 */
		String escaped = JsonSerializer.create().escapeSolidus().build().serialize(aPojo);
		// the output will have escaped /
		System.out.println(escaped);

		/**
		 * Produces
		 * {
		 * 	name: '</pojo>',
		 *	id: 'a'
		 * }
		 */
		String configurableJson =JsonSerializer
			.create()  // Create a JsonSerializer.Builder
			.simpleAttrs()  // Simple mode
			.ws()  // Use whitespace
			.sq()  // Use single quotes
			.build()
			.serialize(aPojo);  // Create a JsonSerializer

		System.out.println(configurableJson);


	}
}
