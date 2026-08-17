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
package org.apache.juneau.rest.server.view.freemarker.console.datatables;

import java.util.*;

import org.apache.juneau.bean.html5.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.rest.server.datatables.*;

import freemarker.core.*;
import freemarker.template.*;
import freemarker.template.utility.*;

/**
 * The FreeMarker shared-variable method backing the {@code <@datatable id=... rows=... rowType=.../>} macro defined
 * in the reserved {@code datatable.ftlh} template.
 *
 * <p>
 * Reuses the exact serialize-then-mark-trusted adapter {@code console-ui-freemarker}'s {@code TagMethodModel} built
 * for {@code <@tag>} (ticket 361 Phase 5) &mdash; not reinvented here: (1) call {@link DataTablesTable#of(String,
 * Collection, Class)} (which additively honors each row property's {@code @Html(render=...)} as of Phase 6), (2) add
 * {@code class="jc-table"} alongside the {@link DataTablesTable#MARKER_ATTR} marker the shipped
 * {@code juneau-datatables.js} glue already looks for, (3) serialize the returned {@link Table} via its own
 * {@code toString()} ({@code HtmlSerializer.DEFAULT_SIMPLE_SQ}), (4) wrap the resulting markup string via
 * {@link HTMLOutputFormat#fromMarkup(String) HTMLOutputFormat.INSTANCE.fromMarkup(...)} so a {@code .ftlh}
 * template's default {@code ${...}} auto-escaping does not double-escape it.
 *
 * <p>
 * Registered as a {@link freemarker.template.Configuration#setSharedVariable(String, TemplateModel) shared variable}
 * by {@link ConsoleDataTablesFreemarkerMixin#resolveConfiguration}, visible to {@code datatable.ftlh} and,
 * transitively (once included), to the consumer's own template.
 *
 * <h5 class='section'>Argument coercion:</h5>
 * <p>
 * {@code id} and {@code rowType} accept a plain FTL string literal. {@code rows} accepts an FTL sequence of beans
 * (or {@code Map}s) &mdash; e.g. a Java {@code List} bound as a template attribute &mdash; unwrapped recursively via
 * {@link DeepUnwrap#unwrap(TemplateModel)}: each element unwraps to its underlying Java bean/{@code Map}, not a
 * FreeMarker bean-model wrapper, exactly as {@code DataTablesTable.of(...)} needs.
 *
 * @since 10.0.0
 */
final class DataTableMethodModel implements TemplateMethodModelEx {

	/** The shared-variable name {@code datatable.ftlh}'s {@code <#macro datatable ...>} calls. */
	static final String NAME = "jcDataTableHtml";

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
		if (arguments.size() != 3)
			throw new TemplateModelException(NAME + "(id, rows, rowType) requires exactly 3 arguments, got " + arguments.size() + ".");
		var id = asPlainString(arguments.get(0));
		var rows = asRows(arguments.get(1));
		var rowTypeName = asPlainString(arguments.get(2));
		var rowType = resolveRowType(rowTypeName);
		Table table;
		try {
			table = DataTablesTable.of(id, rows, rowType);
		} catch (RuntimeException ex) {
			throw new TemplateModelException(ex.getMessage(), ex);
		}
		table.class_("jc-table");
		var markup = HtmlSerializer.DEFAULT_SIMPLE_SQ.toString(table);
		return HTMLOutputFormat.INSTANCE.fromMarkup(markup);
	}

	private static Class<?> resolveRowType(String rowTypeName) throws TemplateModelException {
		try {
			return Class.forName(rowTypeName);
		} catch (ClassNotFoundException ex) {
			throw new TemplateModelException("Unknown rowType class '" + rowTypeName + "'.", ex);
		}
	}

	private static String asPlainString(Object arg) throws TemplateModelException {
		var raw = DeepUnwrap.unwrap((TemplateModel) arg);
		return String.valueOf(raw);
	}

	private static Collection<?> asRows(Object arg) throws TemplateModelException {
		var raw = DeepUnwrap.unwrap((TemplateModel) arg);
		if (raw instanceof Collection<?> c)
			return c;
		throw new TemplateModelException(NAME + "(id, rows, rowType): 'rows' must unwrap to a Collection, got "
			+ (raw == null ? "null" : raw.getClass().getName()) + ".");
	}
}
