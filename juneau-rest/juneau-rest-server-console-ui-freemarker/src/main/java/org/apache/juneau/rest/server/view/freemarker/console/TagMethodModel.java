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

import org.apache.juneau.rest.server.console.*;

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * The FreeMarker shared-variable method backing the {@code <@tag domain=... value=.../>} macro defined in the
 * reserved {@code base.ftlh} template.
 *
 * <p>
 * Implements the design's <b>serialize-then-mark-trusted</b> insertion contract: (1) call {@link Tag#of(String,
 * String)}, (2) serialize the returned {@link org.apache.juneau.bean.html5.Span} via its own {@code toString()}
 * ({@code HtmlSerializer.DEFAULT_SIMPLE_SQ}), (3) wrap the resulting markup string via
 * {@link HTMLOutputFormat#fromMarkup(String) HTMLOutputFormat.INSTANCE.fromMarkup(...)} so a {@code .ftlh}
 * template's default {@code ${...}} auto-escaping does not double-escape it. The macro then writes the result with
 * a plain {@code ${jcTagHtml(domain, value)}} interpolation &mdash; because the returned value is already a
 * {@code TemplateHTMLOutputModel} of the template's own output format, FreeMarker recognizes it as pre-escaped
 * markup and does not re-escape it.
 *
 * <p>
 * Registered as a {@link Configuration#setSharedVariable(String, TemplateModel) shared variable} on the augmented
 * {@link Configuration} by {@link ConsoleFreemarkerMixin#resolveConfiguration}, so it is visible to {@code base.ftlh}
 * and, transitively (once included), to the consumer's own template.
 *
 * <h5 class='section'>Argument coercion:</h5>
 * <p>
 * Both {@code domain} and {@code value} accept either a plain FTL string literal or an arbitrary wrapped Java object
 * (e.g. an {@code Enum} constant passed directly, as in {@code value=SomeEnum.RELEASED}): each argument is resolved
 * via {@link DeepUnwrap#unwrap(TemplateModel)}, then an unwrapped {@link Enum} contributes its {@link Enum#name()}
 * and anything else contributes {@link String#valueOf(Object)}. {@link Tag#of(String, String)} does the actual
 * identifier validation (lowercase-then-anchored-match); this class does no validation of its own.
 *
 * @since 10.0.0
 */
final class TagMethodModel implements TemplateMethodModelEx {

	/** The shared-variable name {@code base.ftlh}'s {@code <#macro tag ...>} calls. */
	static final String NAME = "jcTagHtml";

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		if (arguments.size() != 2)
			throw new TemplateModelException(NAME + "(domain, value) requires exactly 2 arguments, got " + arguments.size() + ".");
		var domain = asPlainString(arguments.get(0));
		var value = asPlainString(arguments.get(1));
		String markup;
		try {
			markup = Tag.of(domain, value).toString();
		} catch (IllegalArgumentException ex) {
			throw new TemplateModelException(ex.getMessage(), ex);
		}
		return HTMLOutputFormat.INSTANCE.fromMarkup(markup);
	}

	private static String asPlainString(Object arg) throws TemplateModelException {
		var raw = DeepUnwrap.unwrap((TemplateModel) arg);
		if (raw instanceof Enum<?> e)
			return e.name();
		return String.valueOf(raw);
	}
}
