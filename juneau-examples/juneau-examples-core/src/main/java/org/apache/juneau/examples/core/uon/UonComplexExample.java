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
package org.apache.juneau.examples.core.uon;

import java.util.*;

import org.apache.juneau.examples.core.pojo.*;
import org.apache.juneau.uon.*;

/**
 * UON complex example.
 *
 */
public class UonComplexExample {
	/**
	 * Serializing PojoComplex bean into UON format.
	 *
	 * @param args Unused.
	 * @throws Exception Unused.
	 */
	public static void main(String[] args) throws Exception {

		// Fill some data to a PojoComplex bean
		var values = new HashMap<String,List<Pojo>>();
		var setOne = new ArrayList<Pojo>();
		setOne.add(new Pojo("1.1", "name1"));
		setOne.add(new Pojo("1.1", "name2"));
		var setTwo = new ArrayList<Pojo>();
		setTwo.add(new Pojo("1.2", "name1"));
		setTwo.add(new Pojo("1.2", "name2"));
		values.put("setOne", setOne);
		values.put("setTwo", setTwo);
		var pojoc = new PojoComplex("pojo", new Pojo("1.0", "name0"), values);

		// this creates an RDF serializer with the default XML structure
		/**Produces
		 * (innerPojo=(name=name0,id='1.0'),
		 * values=(setOne=@((name=name1,id='1.1'),(name=name2,id='1.1')),
		 * setTwo=@((name=name1,id='1.2'),(name=name2,id='1.2'))),id=pojo)
		 */
		var uonSerializer = UonSerializer.DEFAULT;
		// This will show the final output from the bean
		System.out.println(uonSerializer.serialize(pojoc));

		var obj = UonParser.DEFAULT.parse(uonSerializer.serialize(pojoc), PojoComplex.class);

		assert obj.getId().equals(pojoc.getId());

	}
}