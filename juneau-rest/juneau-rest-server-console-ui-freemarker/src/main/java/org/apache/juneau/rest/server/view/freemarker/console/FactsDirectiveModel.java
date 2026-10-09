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

import java.io.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json5.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@facts>} FreeMarker twin of the {@code <@console facts=>} attribute: a JSON5 object body, merged
 * last-wins with any other chrome-side facts.  Repeatable; the one strict check against Java-seeded facts happens
 * once, at {@code </@console>}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	&lt;@facts&gt;{"buildVersion": "1.0"}&lt;/@facts&gt;
 * </p>
 *
 * @since 10.0.0
 */
public final class FactsDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "facts";

	FactsDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@facts> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, Set.of());

		var cap = PageCapture.get(env);
		if (cap == null || ! cap.consoleOpen)
			throw FtlAttrLists.reject("<@facts> must be nested inside <@console>.");

		var markup = PageCapture.render(body);
		if (markup.isBlank())
			return;
		JsonMap parsed;
		try {
			parsed = Json5Parser.DEFAULT.read(markup.trim(), JsonMap.class);
		} catch (org.apache.juneau.marshall.parser.ParseException e) {
			throw FtlAttrLists.reject("<@facts> body is not valid JSON5: " + e.getMessage());
		}
		cap.mergeChromeFacts(parsed);
	}
}
