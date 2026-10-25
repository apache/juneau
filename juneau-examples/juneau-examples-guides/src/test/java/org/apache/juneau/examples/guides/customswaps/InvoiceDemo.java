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

// region demo
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.json.*;

/**
 * Serializes an invoice with and without the swap, then parses it back.
 */
public class InvoiceDemo {

	public static void main(String[] args) throws Exception {
		System.out.println(run());
	}

	public static String run() throws Exception {
		var invoice = new Invoice()
			.number("INV-1001")
			.total(Money.of("12.50", "EUR"))
			.payment(Money.of("10.00", "EUR"))
			.payment(Money.of("2.50", "EUR"));

		// Without the swap, Juneau falls back to toString(), which can't be parsed back.
		var plain = JsonSerializer.DEFAULT.write(invoice);
		String plainParse;
		try {
			JsonParser.DEFAULT.read(plain, Invoice.class);
			plainParse = "parsed";
		} catch (ParseException e) {
			plainParse = "fails (" + e.getClass().getSimpleName() + ")";
		}

		// Register the swap on the serializer and the parser.
		var serializer = JsonSerializer.create().swaps(MoneySwap.class).build();
		var parser = JsonParser.create().swaps(MoneySwap.class).build();

		var json = serializer.write(invoice);
		var parsed = parser.read(json, Invoice.class);

		// The annotated bean needs no registration at all.
		var annotated = new InvoiceAnnotated();
		annotated.number = "INV-1002";
		annotated.total = Money.of("99.95", "USD");

		return String.join("\n",
			"without swap: " + plain,
			"parse it:     " + plainParse,
			"with swap:    " + json,
			"round trip:   " + parsed.total + ", equal=" + parsed.total.equals(invoice.total),
			"annotated:    " + JsonSerializer.DEFAULT.write(annotated));
	}
}
// endregion demo
