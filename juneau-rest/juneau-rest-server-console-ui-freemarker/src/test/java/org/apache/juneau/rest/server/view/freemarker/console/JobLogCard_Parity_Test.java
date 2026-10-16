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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import freemarker.template.*;

/**
 * A job-log card built with {@link PageSpec} and one authored as {@code <@card type="job-log">} reject {@code src=}
 * and {@code template=} with the same message.
 */
class JobLogCard_Parity_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	/** The directive's rendered error for a fixture must contain exactly the builder's message for the same attributes. */
	@ParameterizedTest(name = "{0}")
	@CsvSource({"jl-card-src,/a,", "jl-card-template,,t1", "jl-card-both,/a,t1"})
	void a01_srcAndTemplateRejectedLikeTheDirective(String fixture, String src, String template) {
		var card = CardSpec.of("job-log", "rebuild").body("submit", Map.of("endpoint", "/a"));
		if (src != null)
			card.src(src);
		if (template != null)
			card.template(template);
		var spec = PageSpec.create().card(card);
		var ex = assertThrows(TemplateModelException.class, () -> spec.applyTo(new PageCapture(), MODEL));
		var body = C1Fixtures.renderError(fixture);
		assertTrue(body.contains(ex.getMessage()), () -> "expected '" + ex.getMessage() + "' in:\n" + body);
	}
}
