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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;

import org.apache.juneau.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Card grammar errors (spec §6 E-4, E-8, E-9, E-10; P9; P17), each asserted verbatim.
 *
 * @since 10.0.0
 */
class CardDirective_RemovedTypes_Test extends TestBase {

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"removed-js|<@card id='probes'> type='js' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
		"removed-json|<@card id='j'> type='json' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
		"removed-calendar|<@card id='cal'> type='calendar' was removed in 10.0.0; use type='html' with a <template>, or a registered card type.",
		"removed-js-noid|<@card id=''> type='js' was removed in 10.0.0;",
		"bad-card-none|<@card id='e'> type='html' requires a body or src=.",
		"bad-card-both|<@card id='e'> type='html' takes exactly one of a body, template= or src=.",
		"bad-card-tpl-src|<@card id='e'> type='html' takes exactly one of a body, template= or src=.",
		"bad-card-id|<@card> id '1x' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.",
		"bad-card-dup|<@card id='a'> duplicates an existing card id.",
		"bad-card-nested|<@card> cannot be nested inside another <@card>.",
		"bad-card-orphan|<@card> must be nested inside <@page>.",
		"bad-card-dt-src|<@card id='t'> type='datatables' takes src= or a body, not both."
	})
	void a01_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
