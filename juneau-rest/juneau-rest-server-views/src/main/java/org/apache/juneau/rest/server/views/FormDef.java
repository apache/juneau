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

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.commons.bean.*;

/**
 * The declarative source of a row-action modal's input form (the form half of {@code TODO-416}; {@code TODO-399}
 * Decision&nbsp;8's "form inside the dialog").
 *
 * <h5 class='section'>FreeMarker-first</h5>
 * <p>
 * The form's fields are sourced <b>FreeMarker-template-first</b>, consistent with the rest of this module's
 * server-render story: a {@link #template} names the server-side template that produces the form's field markup,
 * rendered against the current record.  The alternative bean&rarr;form generator ({@code FormDef.of(beanType)}) is a
 * deliberately deferred follow-on ({@code TODO-399} Decision&nbsp;8's "bean&rarr;form later"), so this MVP exposes
 * only the template source; the {@code beanType} field is reserved and omitted from the wire until then.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FormDef <jv>form</jv> = FormDef.<jsm>ofTemplate</jsm>(<js>"servlet:/incidents/ack-form.ftl"</js>);
 * </p>
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link ModalDef}
 * 	<li class='jc'>{@link RowAction}
 * </ul>
 *
 * @since 10.0.0
 */
@BeanType(properties="template")
@SuppressWarnings("java:S1845") // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
public class FormDef {

	/** The FreeMarker template reference that renders the form's field markup against the current record. */
	public String template;

	/**
	 * Creates a form sourced from the specified FreeMarker template reference.
	 *
	 * @param template The template reference (a resolvable URL/name).  Must not be <jk>null</jk> or blank.
	 * @return A new {@link FormDef}.
	 * @throws IllegalArgumentException If {@code template} is <jk>null</jk> or blank.
	 */
	public static FormDef ofTemplate(String template) {
		if (template == null || template.isBlank())
			throw iaex("FormDef template must not be null or blank.");
		var f = new FormDef();
		f.template = template;
		return f;
	}
}
