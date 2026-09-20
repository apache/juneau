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

import java.util.*;

import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * Helpers for reading scalar and list-valued FreeMarker directive attributes.
 *
 * <p>
 * A list attribute (e.g. {@code init=} / {@code css=} / {@code toolkit=}) is first-class as either a
 * comma-separated scalar string (the one-file case) or a FreeMarker sequence literal
 * ({@code init=["one.js","two.js"]}) &mdash; both forms are supported, neither is a fallback.
 *
 * @since 10.0.0
 */
final class FtlAttrLists {

	private FtlAttrLists() {}

	/**
	 * Builds a {@link TemplateModelException} whose exact message reaches the HTTP 500 body verbatim.
	 *
	 * <p>
	 * FreeMarker's own {@link freemarker.template.TemplateException#getMessage()} rewrites an
	 * FTL-directive-looking description (it turns {@code <@card>} into {@code  @card }) and appends an FTL
	 * stack trace, and its DEBUG exception handler writes that decorated text to whatever output writer it was
	 * rendering into. For a directive that throws while a parent {@code <@page>} is capturing its body into a
	 * throwaway buffer, that decorated text is discarded. Carrying the frozen sentence on a plain-exception
	 * cause lets {@code FreemarkerViewRenderer}'s deepest-cause lookup surface it undecorated (angle brackets
	 * intact) regardless of which buffer FreeMarker happened to be writing to.
	 *
	 * @param message The exact diagnostic sentence. Must not be {@code null}.
	 * @return A {@link TemplateModelException} carrying {@code message} both as its description and on a
	 * 	plain-exception cause.
	 */
	static TemplateModelException reject(String message) {
		return new TemplateModelException(message, new IllegalArgumentException(message));
	}

	static String scalar(Map<String, TemplateModel> params, String name) throws TemplateModelException {
		var raw = params.get(name);
		if (raw == null)
			return "";
		var u = DeepUnwrap.unwrap(raw);
		return u == null ? "" : String.valueOf(u).trim();
	}

	static List<String> list(Map<String, TemplateModel> params, String directive, String name) throws TemplateModelException {
		var raw = params.get(name);
		if (raw == null)
			return List.of();
		var u = DeepUnwrap.unwrap(raw);
		if (u instanceof String s) {
			if (s.isBlank())
				return List.of();
			var out = new ArrayList<String>();
			for (var part : s.split(",")) {
				var t = part.trim();
				if (! t.isEmpty())
					out.add(t);
			}
			return List.copyOf(out);
		}
		if (u instanceof Collection<?> c) {
			var out = new ArrayList<String>();
			for (var item : c) {
				if (item == null)
					continue;
				var t = String.valueOf(item).trim();
				if (! t.isEmpty())
					out.add(t);
			}
			return List.copyOf(out);
		}
		throw reject("<@" + directive + "> " + name + "= must be a string or sequence.");
	}

	static void rejectUnknown(Map<String, TemplateModel> params, String directive, Set<String> allowed)
			throws TemplateModelException {
		for (var key : params.keySet()) {
			if (! allowed.contains(key))
				throw reject("<@" + directive + "> unknown attribute '" + key + "'.");
		}
	}
}
