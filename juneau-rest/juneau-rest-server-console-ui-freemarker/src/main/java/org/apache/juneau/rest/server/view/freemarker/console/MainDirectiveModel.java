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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.util.*;

import freemarker.core.*;
import freemarker.template.*;

/**
 * The {@code <@main/>} console FreeMarker directive: the positional placeholder that renders the captured page body
 * inside exactly one {@code <main class="jc-main">}.
 *
 * <p>
 * {@code <@main/>} is a position marker. Markup authored between {@code <@console>} and {@code <@main/>} becomes the
 * {@code header.slots.banner} template, and the page's cards render into {@code <main>} in the shell. It is
 * self-closing &mdash; it takes no body of its own, and it has no attributes.
 *
 * @since 10.0.0
 */
public final class MainDirectiveModel implements TemplateDirectiveModel {

	/** The shared-variable name this directive registers under. */
	public static final String NAME = "main";

	static final Set<String> ATTRS = Set.of();

	MainDirectiveModel() {}

	@Override
	@SuppressWarnings({
		"unchecked" // FreeMarker's raw params Map is String-keyed by contract.
	})
	public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
			TemplateDirectiveBody body) throws TemplateException, IOException {
		Map<String, TemplateModel> p = params;
		if (p.containsKey("format"))
			throw FtlAttrLists.reject("<@main> has no format= attribute.");
		FtlAttrLists.rejectUnknown(p, NAME, ATTRS);
		if (nn(body))
			throw FtlAttrLists.reject("<@main/> is self-closing; the page body is the main content and takes no nested body.");

		var cap = PageCapture.get(env);
		if (n(cap) || ! cap.consoleOpen)
			throw FtlAttrLists.reject(f("<@%s> must be nested inside <@console>.", NAME));
		cap.markMain();
	}
}
