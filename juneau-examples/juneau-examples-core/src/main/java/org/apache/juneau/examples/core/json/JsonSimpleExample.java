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

import java.util.*;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.json.*;

/**
 * Sample class which shows the simple usage of JsonSerializer and JsonParser.
 *
 */
public class JsonSimpleExample {

	/**
	 * Serializing Pojo bean into Json format and Deserialize back to Pojo instance type.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	public static void main(String[] args) throws Exception {
		// Juneau provides static constants with the most commonly used configurations
		// Get a reference to a serializer - converting POJO to flat format
		// Produces
		// {"name":"name","id":"id"}
		var jsonSerializer = JsonSerializer.DEFAULT;
		// Get a reference to a parser - converts that flat format back into the POJO
		var jsonParser = JsonParser.DEFAULT;

		var pojo = new Pojo("id", "name");

		var flat = jsonSerializer.serialize(pojo);
		// Print out the created POJO in JSON format.
		System.out.println(flat);

		var parse = jsonParser.parse(flat, Pojo.class);

		assert parse.getId().equals(pojo.getId());
		assert parse.getName().equals(pojo.getName());

		// Produces
		// {name:'name',id:'id'}
		var json5 = Json5Serializer.DEFAULT.serialize(pojo);
		System.out.println(json5);

		// Parse a JSON object (creates a generic JsonMap).
		var json = "{name:'John Smith',age:21}";
		Map m1 = jsonParser.parse(json, Map.class);

		// Parse a JSON string.
		json = "'foobar'";
		var s2 = jsonParser.parse(json, String.class);

		// Parse a JSON number as a Long or Float.
		json = "123";
		var l3 = jsonParser.parse(json, Long.class);
		var f3 = jsonParser.parse(json, Float.class);

		// The object above can be parsed thanks to the @Beanc(properties = id,name) annotation on Pojo
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.
	}
}