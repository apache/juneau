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
package org.apache.juneau.examples.guides.customswaps;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.examples.guides.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

class CustomSwaps_Test extends TestBase {

	@Test void a01_runOutputMatchesGuide() throws Exception {
		assertEquals(GuideRegion.read("CustomSwaps/run-output.txt", "output"), GuideRegion.normalize(InvoiceDemo.run()));
	}

	@Test void a02_roundTripPreservesMoney() throws Exception {
		var s = JsonSerializer.create().swaps(MoneySwap.class).build();
		var p = JsonParser.create().swaps(MoneySwap.class).build();
		var in = new Invoice().number("X").total(Money.of("1.05", "GBP")).payment(Money.of("1.05", "GBP"));
		var out = p.read(s.write(in), Invoice.class);
		assertEquals(in.total, out.total);
		assertEquals(in.payments, out.payments);
	}

	@Test void a03_annotatedSwapParsesWithoutRegistration() throws Exception {
		var out = JsonParser.DEFAULT.read("{\"number\":\"N\",\"total\":\"3.00 JPY\"}", InvoiceAnnotated.class);
		assertEquals(Money.of("3.00", "JPY"), out.total);
	}
}
