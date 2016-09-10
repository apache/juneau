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
package org.apache.juneau.transforms;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class ReaderFilterTest {

	//====================================================================================================
	// test
	//====================================================================================================
	@Test
	public void test() throws Exception {
		WriterSerializer s = new JsonSerializer.Simple().addPojoSwaps(ReaderSwap.Json.class);

		Reader r;
		Map<String,Object> m;

		r = new StringReader("{foo:'bar',baz:'quz'}");
		m = new HashMap<String,Object>();
		m.put("X", r);
		assertEquals("{X:{foo:'bar',baz:'quz'}}", s.serialize(m));

		s.addPojoSwaps(ReaderSwap.Xml.class);
		r = new StringReader("<object><foo _type='string'>bar</foo><baz _type='string'>quz</baz></object>");
		m = new HashMap<String,Object>();
		m.put("X", r);
		assertEquals("{X:{foo:'bar',baz:'quz'}}", s.serialize(m));
	}
}