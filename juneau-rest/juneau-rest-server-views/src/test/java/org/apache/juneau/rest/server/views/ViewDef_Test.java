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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** Covers {@link ViewDef}, including a compile-checked run of its class Javadoc {@code Example:}. */
class ViewDef_Test extends TestBase {

	@Test void a01_classJavadocExample_runsAndColumnInheritsTableLevelOperators() {
		var view = ViewDef.create("releases")
			.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq")))
			.column(Column.create("name").label("Name").searchType(SearchType.TEXT))
			.column(Column.create("status").label("Status").searchType(SearchType.ID)
				.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$in"))));
		assertList(view.columns().stream().map(Column::name).toList(), "name", "status");
		// "name" has no column-level set, so it inherits the table-level set (just $eq).
		assertList(view.findColumn("name").effectiveOperators(view.operators()).stream().map(SearchOperator::name).toList(), "$eq");
		// "status" has its own set, which entirely replaces the table-level set.
		assertList(view.findColumn("status").effectiveOperators(view.operators()).stream().map(SearchOperator::name).toList(), "$in");
	}
}
