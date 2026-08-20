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

import java.util.*;

import org.apache.juneau.commons.bean.*;

/**
 * The declarative source of a row-action modal's input form (the form half of a {@code present=dialog} action).
 *
 * <h5 class='section'>Typed inputs, not markup</h5>
 * <p>
 * The client paints {@link #fields} as native {@code <input>} / {@code <textarea>} elements via
 * {@code createElement}.  Labels use {@code textContent}; prefills use {@code .value}.  The runtime never assigns
 * {@code innerHTML} from this payload.  Only {@code text} and {@code textarea} types are legal &mdash; that is the
 * XSS bound: hostile prefill cannot become an element.
 * </p>
 * <p>
 * {@link #template} may name a server-side template for authors.  It is <b>not</b> a client HTML sink; the
 * shipped {@code juneau-views.js} ignores it.
 * </p>
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	FormDef <jv>form</jv> = FormDef.<jsm>create</jsm>()
 * 		.field(FormDef.Input.<jsm>of</jsm>(<js>"resolution"</js>, <js>"Resolution comment"</js>, <js>"textarea"</js>)
 * 			.required()
 * 			.value(<js>""</js>));
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
@BeanType(properties="template,fields")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class FormDef {

	/**
	 * A single typed form input painted client-side with {@code createElement} (never {@code innerHTML}).
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="name,label,type,required,value")
	@SuppressWarnings({
		"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
	})
	public static class Input {

		/** The submit-body key for this field (e.g. {@code resolution}). */
		public String name;

		/** The label shown next to the control. */
		public String label;

		/** {@code text} or {@code textarea}. */
		public String type;

		/** When {@link Boolean#TRUE}, the control is required.  Omitted from the wire otherwise. */
		public Boolean required;

		/** Optional prefill, applied via {@code .value} (never {@code innerHTML}). */
		public String value;

		/**
		 * Creates a typed form input.
		 *
		 * @param name The submit-body key.  Must not be <jk>null</jk> or blank.
		 * @param label The visible label.  Must not be <jk>null</jk> or blank.
		 * @param type {@code text} or {@code textarea}.  <jk>null</jk> or blank defaults to {@code text}.
		 * @return A new {@link Input}.
		 * @throws IllegalArgumentException If {@code name} or {@code label} is blank, or {@code type} is not an
		 * 	allowed token.
		 */
		public static Input of(String name, String label, String type) {
			if (name == null || name.isBlank())
				throw iaex("FormDef.Input name must not be null or blank.");
			if (label == null || label.isBlank())
				throw iaex("FormDef.Input label must not be null or blank.");
			var t = (type == null || type.isBlank()) ? "text" : type;
			if (! "text".equals(t) && ! "textarea".equals(t))
				throw iaex("FormDef.Input type must be 'text' or 'textarea', not '%s'.", t);
			var i = new Input();
			i.name = name;
			i.label = label;
			i.type = t;
			return i;
		}

		/**
		 * Marks this input required.
		 *
		 * @return This object.
		 */
		public Input required() {
			required = Boolean.TRUE;
			return this;
		}

		/**
		 * Sets whether this input is required.
		 *
		 * @param value <jk>true</jk> to require a value; <jk>false</jk> omits the flag from the wire.
		 * @return This object.
		 */
		public Input required(boolean value) {
			required = value ? Boolean.TRUE : null;
			return this;
		}

		/**
		 * Sets the optional prefill.
		 *
		 * @param value The prefill.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public Input value(String value) {
			this.value = value;
			return this;
		}
	}

	/** Optional FreeMarker template reference for server authors; ignored by the client. */
	public String template;

	/** Typed inputs in display order; omitted from the wire when none are declared. */
	public List<Input> fields;

	/**
	 * Starts an empty form (add {@link #field(Input) fields} and/or a {@link #template(String) template}).
	 *
	 * @return A new {@link FormDef}.
	 */
	public static FormDef create() {
		return new FormDef();
	}

	/**
	 * Creates a form sourced from the specified FreeMarker template reference.
	 *
	 * @param template The template reference (a resolvable URL/name).  Must not be <jk>null</jk> or blank.
	 * @return A new {@link FormDef}.
	 * @throws IllegalArgumentException If {@code template} is <jk>null</jk> or blank.
	 */
	public static FormDef ofTemplate(String template) {
		return create().template(template);
	}

	/**
	 * Sets the optional server-author template reference.  The client never treats this as HTML.
	 *
	 * @param value The template reference.  Must not be <jk>null</jk> or blank.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or blank.
	 */
	public FormDef template(String value) {
		if (value == null || value.isBlank())
			throw iaex("FormDef template must not be null or blank.");
		template = value;
		return this;
	}

	/**
	 * Adds one typed input field.
	 *
	 * @param value The input.  Must not be <jk>null</jk>.
	 * @return This object.
	 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
	 */
	public FormDef field(Input value) {
		if (value == null)
			throw iaex("FormDef field must not be null.");
		if (fields == null)
			fields = l();
		fields.add(value);
		return this;
	}
}
