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
package org.apache.juneau.examples.core.oapi;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.httppart.HttpPartParser;
import org.apache.juneau.httppart.HttpPartSchema;
import org.apache.juneau.httppart.HttpPartSerializer;
import org.apache.juneau.httppart.HttpPartType;
import org.apache.juneau.oapi.OpenApiParser;
import org.apache.juneau.oapi.OpenApiSerializer;

/**
 * Sample class which shows the simple usage of OpenApiSerializer.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class OapiExample {


	/**
	 * Get a reference to a parser and usage of oapiserializer.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) throws Exception{

		OpenApiSerializer oapiSerializer = OpenApiSerializer.DEFAULT;

		OpenApiParser oapiParser = OpenApiParser.DEFAULT;

		Pojo pojo = new Pojo("id","name");

		String flat = oapiSerializer.serialize(pojo);
		// Print out the created POJO in OpenAPI format.

		Pojo parse = oapiParser.parse(flat, Pojo.class);

		assert parse.getId().equals(pojo.getId());
		assert parse.getName().equals(pojo.getName());

		//Http part schmea
		HttpPartSchema schema = HttpPartSchema
			.create("array")
			.collectionFormat("pipes")
			.items(
				HttpPartSchema
				.create("array")
				.collectionFormat("csv")
				.items(
					HttpPartSchema.create("integer","int64")
				)
			)
			.build();
		Object value = new long[][]{{1,2,3},{4,5,6},{7,8,9}};
		String output = OpenApiSerializer.DEFAULT.serialize(HttpPartType.HEADER, schema, value);

		HttpPartSchema schemab = HttpPartSchema.create().type("string").build();
		// Convert POJO to BASE64-encoded string.
		HttpPartSerializer s = OpenApiSerializer.DEFAULT;
		String httpPart = s.serialize(schemab, pojo);
		System.out.println(httpPart);

		// Convert BASE64-encoded string back into a POJO.
		HttpPartParser p = OpenApiParser.DEFAULT;
		pojo = p.parse(schemab, httpPart, Pojo.class);

		// The object above can be parsed thanks to the @Beanc(properties = id,name) annotation on Pojo
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.
	}
}
