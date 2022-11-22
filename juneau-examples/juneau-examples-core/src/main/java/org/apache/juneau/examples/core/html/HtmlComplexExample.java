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
package org.apache.juneau.examples.core.html;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.examples.core.pojo.PojoComplex;
import org.apache.juneau.html.HtmlSerializer;
import org.apache.juneau.html.HtmlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Sample class which shows the complex usage of HtmlSerializer and HtmlParser.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class HtmlComplexExample {

	/**
	 * Serializing PojoComplex bean into Html type
	 * and Deserialize back to PojoComplex instance type.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {
		// Juneau provides static constants with the most commonly used configurations
		// Get a reference to a serializer - converting POJO to flat format
		/**
		 * Produces
		 * <table><tr><td>innerPojo</td><td><table><tr><td>name</td><td>name0</td></tr>
		 * <tr><td>id</td><td>1.0</td></tr></table></td></tr><tr><td>values</td><td><table>
		 * <tr><td>setOne</td><td><table _type="array"><tr><th>name</th><th>id</th></tr>
		 * <tr><td>name1</td><td>1.1</td></tr><tr><td>name2</td><td>1.1</td></tr>
		 * </table></td></tr><tr><td>setTwo</td><td><table _type="array"><tr><th>name
		 * </th><th>id</th></tr><tr><td>name1</td><td>1.2</td></tr><tr><td>name2</td><td>1.2
		 * </td></tr></table></td></tr></table></td></tr><tr><td>id</td><td>pojo</td></tr></table>
		 */
		HtmlSerializer htmlSerializer = HtmlSerializer.DEFAULT;
		// Get a reference to a parser - converts that flat format back into the POJO
		HtmlParser htmlParser = HtmlParser.DEFAULT;

		// Fill some data to a PojoComplex bean
		HashMap<String, List<Pojo>> values = new HashMap<>();
		ArrayList<Pojo> setOne = new ArrayList<>();
		setOne.add(new Pojo("1.1", "name1"));
		setOne.add(new Pojo("1.1", "name2"));
		ArrayList<Pojo> setTwo = new ArrayList<>();
		setTwo.add(new Pojo("1.2", "name1"));
		setTwo.add(new Pojo("1.2", "name2"));
		values.put("setOne", setOne);
		values.put("setTwo", setTwo);
		PojoComplex pojoc = new PojoComplex("pojo", new Pojo("1.0", "name0"), values);

		String flat = htmlSerializer.serialize(pojoc);

		// Print out the created POJO in JSON format.
		System.out.println(flat);

		PojoComplex parse = htmlParser.parse(flat, PojoComplex.class);

		assert parse.getId().equals(pojoc.getId());
		assert parse.getInnerPojo().getName().equals(pojoc.getInnerPojo().getName());
		assert parse.getInnerPojo().getId().equals(pojoc.getInnerPojo().getId());

		// The object above can be parsed thanks to the @Beanc(properties = id,name) annotation on Pojo
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.
	}
}
