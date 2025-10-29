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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class Classes_RoundTripTest extends RoundTripTest_Base {

	@ParameterizedTest
	@MethodSource("testers")
	void a01_classObjects(RoundTrip_Tester t) throws Exception {
		var o = String.class;
		o = t.roundTrip(o);
		assertSame(o, String.class);

		var o2 = a(String.class);
		o2 = t.roundTrip(o2);
		assertJson("['java.lang.String']", o2);

		var o3 = l(String.class, Integer.class);
		o3 = t.roundTrip(o3);
		assertJson("['java.lang.String','java.lang.Integer']", o3);

		var o4 = m(String.class, String.class);
		o4 = t.roundTrip(o4);
		assertJson("{'java.lang.String':'java.lang.String'}", o4);
	}
}