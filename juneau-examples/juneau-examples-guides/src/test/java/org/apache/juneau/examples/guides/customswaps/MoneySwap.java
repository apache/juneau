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

// region swap
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.spi.*;

/**
 * Serializes {@link Money} as a string such as "12.50 EUR", and parses it back.
 */
public class MoneySwap extends StringSwap<Money> {

	@Override
	public String swap(MarshallingSession session, Money money) {
		return money.amount().toPlainString() + " " + money.currency().getCurrencyCode();
	}

	@Override
	public Money unswap(MarshallingSession session, String value, ClassMeta<?> hint) {
		var parts = value.trim().split("\\s+");
		return Money.of(parts[0], parts[1]);
	}
}
// endregion swap
