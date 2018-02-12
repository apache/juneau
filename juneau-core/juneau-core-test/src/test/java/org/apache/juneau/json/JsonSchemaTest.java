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

import static org.junit.Assert.*;

import org.junit.*;

public class JsonSchemaTest {

	//====================================================================================================
	// Primitive objects
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {

		JsonSchemaSerializer s = JsonSerializer.DEFAULT_LAX.getSchemaSerializer();

		Object o = new String();
		assertEquals("{type:'string',description:'java.lang.String'}", s.serialize(o));

		o = new Integer(123);
		assertEquals("{type:'number',description:'java.lang.Integer'}", s.serialize(o));

		o = new Float(123);
		assertEquals("{type:'number',description:'java.lang.Float'}", s.serialize(o));

		o = new Double(123);
		assertEquals("{type:'number',description:'java.lang.Double'}", s.serialize(o));

		o = Boolean.TRUE;
		assertEquals("{type:'boolean',description:'java.lang.Boolean'}", s.serialize(o));
	}
}