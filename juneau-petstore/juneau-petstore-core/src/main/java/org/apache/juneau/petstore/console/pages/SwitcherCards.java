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
package org.apache.juneau.petstore.console.pages;

import org.apache.juneau.console.freemarker.*;
import org.apache.juneau.console.views.*;

/**
 * Builds nested table cards for switcher views.
 *
 * <p>
 * A page-level table is added with {@code PageSpec.table(...)}; a table inside a switcher view is a plain
 * {@code datatables} card whose body is the table's card body, exactly what a template authors inline.
 */
final class SwitcherCards {

	private SwitcherCards() {}

	/**
	 * A {@code datatables} card for a table, for use as a switcher view or section card.
	 *
	 * @param table The table.  Validated here.
	 * @return The card.
	 */
	static CardSpec table(TableSpec table) {
		table.validate();
		var card = CardSpec.of("datatables", table.id());
		// CardSpec.body rejects the reserved "model" key (it has its own block); copy every other key through.
		table.toCardBody().forEach((k, v) -> {
			if (! "model".equals(k))
				card.body(k, v);
		});
		return card;
	}
}
