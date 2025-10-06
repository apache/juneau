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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;
import org.junit.jupiter.api.*;

class ObjectSwap_Test extends TestBase {

	//====================================================================================================
	// Test same type
	// If you define a ObjectSwap<String,String> filter, then it should be invoked on all strings.
	//====================================================================================================
	@Test void a01_sameType() throws Exception {
		var s = JsonSerializer.create().json5().swaps(ASwap.class).build();
		var p = JsonParser.create().swaps(ASwap.class).build();
		var r = s.serialize("foobar");

		assertEquals("'xfoobarx'", r);
		r = p.parse(r, String.class);
		assertEquals("foobar", r);

		var m = JsonMap.ofJson("{foo:'bar'}");
		r = s.serialize(m);
		assertEquals("{xfoox:'xbarx'}", r);
	}

	public static class ASwap extends StringSwap<String> {
		@Override
		public String swap(BeanSession session, String o) throws SerializeException {
			return "x" + o + "x";
		}

		@Override
		public String unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
			return f.substring(1, f.length()-1);
		}
	}
}
