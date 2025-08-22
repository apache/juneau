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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class Classes_RoundTripTest extends RoundTripTest_Base {

	@ParameterizedTest
	@MethodSource("testers")
	void a01_classObjects(RoundTripTester t) throws Exception {
		Object o = String.class;
		o = t.roundTrip(o);
		assertSame(o, String.class);

		o = new Class[]{String.class};
		o = t.roundTrip(o);
		assertJson(o, "['java.lang.String']");

		o = alist(String.class, Integer.class);
		o = t.roundTrip(o);
		assertJson(o, "['java.lang.String','java.lang.Integer']");

		o = map(String.class, String.class);
		o = t.roundTrip(o);
		assertJson(o, "{'java.lang.String':'java.lang.String'}");
	}
}