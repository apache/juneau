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

// region money
import java.math.*;
import java.util.*;

/**
 * An amount of money in one currency.
 *
 * <p>
 * Stands in for a type you don't control: no public constructor, no getters or setters, and a
 * debugging-only toString(), so Juneau has no way to turn it into JSON and back.
 */
public final class Money {

	private final BigDecimal amount;
	private final Currency currency;

	private Money(BigDecimal amount, Currency currency) {
		this.amount = amount;
		this.currency = currency;
	}

	public static Money of(String amount, String currencyCode) {
		return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
	}

	public BigDecimal amount() { return amount; }

	public Currency currency() { return currency; }

	@Override
	public String toString() {
		return "Money(amount=" + amount + ", currency=" + currency + ")";
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Money m && amount.equals(m.amount) && currency.equals(m.currency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(amount, currency);
	}
}
// endregion money
