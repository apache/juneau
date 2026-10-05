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
 * Every server-side console/nav error, asserted verbatim (spec §6). The footer {@code text=} versus body cases that
 * succeed are in {@link PageCapture_Contract_Test} (a13-a15).
 *
 * @since 10.0.0
 */
class ConsoleDirective_Errors_Test extends TestBase {

	@ParameterizedTest
	@CsvSource(delimiter='|', value={
		"bad-slot-orphan|<@brand> must be nested inside <@console>.",
		"bad-main-orphan|<@main> must be nested inside <@console>.",
		"bad-nav-orphan|<@navigation> must be nested inside <@console>.",
		"bad-after-main|<@console> has markup after <@main/>; move it into <@footer> or <@scripts>.",
		"bad-no-main|<@console> requires exactly one <@main/>; found '0'.",
		"bad-two-main|<@console> requires exactly one <@main/>; found '2'.",
		"bad-layout|<@navigation> layout= must be horizontal or vertical; got 'diagonal'.",
		"bad-footer-both|<@footer text='T'> also has a body; use text= for plain text or the body for markup, not both.",
		"bad-theme-orphan|<@theme name='gray'> must be nested inside <@console>; the legacy pageThemeCss path was removed in 10.0.0.",
		"bad-nested-console|<@console> cannot be nested inside another <@console>.",
		"bad-leaf|<@node id='a'> leaf requires href=.",
		"bad-dup-node|<@node id='a'> duplicates a sibling id under '(root)'.",
		"bad-node-id|<@node> id '9lives' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.",
		"bad-node-orphan|<@node> must be nested inside <@navigation>."
	})
	void a01_errors(String fixture, String message) {
		assertError(fixture, message);
	}
}
