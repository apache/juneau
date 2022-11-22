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
package org.apache.juneau.examples.core.xml;

import java.util.*;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.xml.*;

/**
 * Sample class which shows the complex usage of XmlSerializer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class XmlComplexExample {

	/**
	 * Serializing PojoComplex bean into human readable XML
	 * and Deserialize back to PojoComplex instance type.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {

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

		// Serialize to human readable XML and print
		String serial = XmlSerializer.DEFAULT_SQ_READABLE.serialize(pojoc);
		System.out.println(serial);

		// Deserialize back to PojoComplex instance
		PojoComplex obj = XmlParser.DEFAULT.parse(serial, PojoComplex.class);

		assert obj.getClass().equals(pojoc.getClass());
		assert obj.getInnerPojo().getId().equals(pojoc.getInnerPojo().getId());

		// The object above can be parsed thanks to the @Beanc annotation on PojoComplex
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.

	}
}
