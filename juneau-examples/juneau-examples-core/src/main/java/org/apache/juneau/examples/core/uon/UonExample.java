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
package org.apache.juneau.examples.core.uon;

import org.apache.juneau.examples.core.pojo.Pojo;
import org.apache.juneau.uon.UonParser;
import org.apache.juneau.uon.UonSerializer;

/**
 *	Sample class which shows the simple usage of UONSerializer.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class UonExample {

	/**
	 * Serializing SimplePojo bean into UON type
	 * and Deserialize back to Pojo instance type.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {

		// Fill some data to a Pojo bean
		Pojo pojo = new Pojo("id","name");

		/**
		 * Produces
		 * (name=name,id=id)
		 */
		String serial = UonSerializer.DEFAULT.serialize(pojo);
		System.out.println(serial);

		// Deserialize back to Pojo instance
		Pojo obj = UonParser.DEFAULT.parse(serial, Pojo.class);

		assert obj.getId().equals(pojo.getId());
		assert obj.getName().equals(pojo.getName());

		// The object above can be parsed thanks to the @Beanc annotation on PojoComplex
		// Using this approach, you can keep your POJOs immutable, and still serialize and deserialize them.

	}
}
