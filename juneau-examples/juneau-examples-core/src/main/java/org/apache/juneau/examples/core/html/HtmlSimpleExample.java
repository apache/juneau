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
import org.apache.juneau.html.HtmlDocSerializer;
import org.apache.juneau.html.HtmlParser;
import org.apache.juneau.html.HtmlSerializer;

/**
 * Sample class which shows the simple usage of HtmlSerializer and HtmlParser.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class HtmlSimpleExample {
	/**
	 * Serializing Pojo bean into Html format and Deserialize back to Pojo instance type.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception{
		// Juneau provides static constants with the most commonly used configurations
		// Get a reference to a serializer - converting POJO to flat format
		// Produces
		// <table><tr><td>name</td><td>name</td></tr><tr><td>id</td><td>id</td></tr></table>
		HtmlSerializer htmlSerializer = HtmlSerializer.DEFAULT;
		// Get a reference to a parser - converts that flat format back into the POJO
		HtmlParser htmlParser = HtmlParser.DEFAULT;

		Pojo pojo = new Pojo("id","name");

		String flat = htmlSerializer.serialize(pojo);

		// Print out the created POJO in JSON format.
		System.out.println(flat);

		Pojo parse = htmlParser.parse(flat, Pojo.class);

		assert parse.getId().equals(pojo.getId());
		assert parse.getName().equals(pojo.getName());

		/**
		 *  Produces
		 *  <html><head><style></style><script></script></head><body><section><article><div class="outerdata">
		 *  <div class="data" id="data"><table><tr><td>name</td><td>name</td></tr><tr><td>id</td><td>id</td></tr>
		 *  </table></div></div></article></section></body></html>
		 */
		String docSerialized = HtmlDocSerializer.DEFAULT.serialize(pojo);
		System.out.println(docSerialized);

		// The object above can be parsed thanks to the @Beanc(properties = id,name) annotation on Pojo
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.
	}
}
