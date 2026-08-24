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
import java.util.regex.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The declarative source of a row-action modal's input form (the form half of a {@code present=dialog} action).
 *
 * <h5 class='section'>Typed inputs, not markup</h5>
 * <p>
 * The client paints {@link #fields} as native {@code <input>} / {@code <textarea>} / {@code <select>} /
 * {@code <button>} elements via {@code createElement}.  Labels and option text use {@code textContent}; prefills use
 * {@code .value} / {@code .checked}.  The runtime never assigns {@code innerHTML} from this payload.  Only the
 * types on the allowlist ({@code text}, {@code textarea}, {@code checkbox}, {@code toggle}, {@code select},
 * {@code action}) are legal &mdash; that is the XSS bound: hostile prefill cannot become an element.
 * </p>
 * <p>
 * {@link #template} may name a server-side template for authors.  It is <b>not</b> a client HTML sink; the
 * shipped {@code juneau-views.js} ignores it.
 * </p>
 *
 * <h5 class='section'>Per-widget contract version (fail-loud when a form is present)</h5>
 * <p>
 * This bean {@link Widget#validate() validates} fail-closed and carries an instance {@link #contractVersion}.  The
 * version is <b>null</b> until {@link #checked()} is invoked on the serving path (a raw builder therefore never leaks
 * {@code "1"} on the nested form while a modal top-level is still unversioned); {@code checked()} stamps
 * {@link #CONTRACT_VERSION} then validates.  The client refuses to open a form-bearing dialog whose version does not
 * match the baked-in {@code "1"}.  A confirm-only modal (no form) stays unversioned.
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
@BeanType(properties="contractVersion,template,fields")
@SuppressWarnings({
	"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
})
public class FormDef implements Widget {

	/** The frozen form contract version.  Bumped only on a breaking wire change to this form contract. */
	public static final String CONTRACT_VERSION = "1";

	/** The cap on the length of an author-supplied {@link Input#pattern} (ReDoS defense-in-depth). */
	static final int PATTERN_MAX_LENGTH = 256;

	/** The complete set of legal {@link Input#type} tokens. */
	static final Set<String> INPUT_TYPES = Set.of("text", "textarea", "checkbox", "toggle", "select", "action");

	/**
	 * A single typed form input painted client-side with {@code createElement} (never {@code innerHTML}).
	 *
	 * @since 10.0.0
	 */
	@BeanType(properties="name,label,type,required,value,options,pattern,maxLength,help,actionId")
	@SuppressWarnings({
		"java:S1845" // Fluent-builder setters intentionally mirror field names (Juneau DSL convention).
	})
	public static class Input {

		/**
		 * A single {@code select} option: a submit value and its visible label.
		 *
		 * @since 10.0.0
		 */
		@BeanType(properties="value,label")
		public static class Option {

			/** The submit value for this option.  Must not be <jk>null</jk>. */
			public String value;

			/** The visible option text, painted with {@code textContent}.  Must not be blank. */
			public String label;

			/**
			 * Creates a select option.
			 *
			 * @param value The submit value.  Must not be <jk>null</jk>.
			 * @param label The visible option text.  Must not be <jk>null</jk> or blank.
			 * @return A new {@link Option}.
			 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or {@code label} is blank.
			 */
			public static Option of(String value, String label) {
				if (value == null)
					throw iaex("FormDef.Input.Option value must not be null.");
				if (label == null || label.isBlank())
					throw iaex("FormDef.Input.Option label must not be null or blank.");
				var o = new Option();
				o.value = value;
				o.label = label;
				return o;
			}
		}

		/** The submit-body key for this field (e.g. {@code resolution}). */
		public String name;

		/** The label shown next to the control. */
		public String label;

		/** One of {@code text}, {@code textarea}, {@code checkbox}, {@code toggle}, {@code select}, {@code action}. */
		public String type;

		/** When {@link Boolean#TRUE}, the control is required.  Omitted from the wire otherwise. */
		public Boolean required;

		/** Optional prefill, applied via {@code .value} / {@code .checked} (never {@code innerHTML}). */
		public String value;

		/** The options for a {@code select} field, in display order; omitted from the wire otherwise. */
		public List<Option> options;

		/** An optional client validation regexp for {@code text}/{@code textarea}; omitted from the wire otherwise. */
		public String pattern;

		/** An optional client max-length for {@code text}/{@code textarea}; omitted from the wire otherwise. */
		public Integer maxLength;

		/** An optional help hint painted with {@code textContent}; omitted from the wire otherwise. */
		public String help;

		/** The enclosing-view {@link RowAction} id an {@code action} button opens; omitted from the wire otherwise. */
		public String actionId;

		/**
		 * Creates a typed form input.
		 *
		 * @param name The submit-body key (also the DOM identity for an {@code action} field).  Must not be
		 * 	<jk>null</jk> or blank.
		 * @param label The visible label.  Must not be <jk>null</jk> or blank.
		 * @param type One of {@code text}, {@code textarea}, {@code checkbox}, {@code toggle}, {@code select},
		 * 	{@code action}.  <jk>null</jk> or blank defaults to {@code text}.
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
			if (! INPUT_TYPES.contains(t))
				throw iaex("FormDef.Input type must be one of text, textarea, checkbox, toggle, select, action, not '%s'.", t);
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

		/**
		 * Sets the {@code select} options (replacing any existing).
		 *
		 * @param value The options.  Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or contains a <jk>null</jk> entry.
		 */
		public Input options(Option... value) {
			if (value == null)
				throw iaex("FormDef.Input options must not be null.");
			var l = new ArrayList<Option>();
			for (var o : value) {
				if (o == null)
					throw iaex("FormDef.Input option must not be null.");
				l.add(o);
			}
			options = l;
			return this;
		}

		/**
		 * Adds one {@code select} option.
		 *
		 * @param value The submit value.  Must not be <jk>null</jk>.
		 * @param label The visible option text.  Must not be <jk>null</jk> or blank.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk> or {@code label} is blank.
		 */
		public Input option(String value, String label) {
			if (options == null)
				options = new ArrayList<>();
			options.add(Option.of(value, label));
			return this;
		}

		/**
		 * Sets the client validation regexp ({@code text}/{@code textarea} only).
		 *
		 * @param value The pattern.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public Input pattern(String value) {
			pattern = value;
			return this;
		}

		/**
		 * Sets the client max-length ({@code text}/{@code textarea} only).
		 *
		 * @param value The max length.  Must be {@code > 0}.
		 * @return This object.
		 */
		public Input maxLength(int value) {
			maxLength = value;
			return this;
		}

		/**
		 * Sets the help hint (painted with {@code textContent}).
		 *
		 * @param value The hint.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public Input help(String value) {
			help = value;
			return this;
		}

		/**
		 * Sets the enclosing-view {@link RowAction} id this {@code action} button opens.
		 *
		 * @param value The action id.  Can be <jk>null</jk> to unset.
		 * @return This object.
		 */
		public Input actionId(String value) {
			actionId = value;
			return this;
		}

		/**
		 * Sets the {@code action} target from an {@link ActionRef} (stores {@link ActionRef#id}).
		 *
		 * @param value The action reference.  Must not be <jk>null</jk>.
		 * @return This object.
		 * @throws IllegalArgumentException If {@code value} is <jk>null</jk>.
		 */
		public Input action(ActionRef value) {
			if (value == null)
				throw iaex("FormDef.Input action must not be null.");
			actionId = value.id;
			return this;
		}

		/**
		 * Fail-closed per-input validation (called from {@link FormDef#validate()}).
		 *
		 * @throws IllegalArgumentException If this input is not well-formed.
		 */
		void validate() {
			if (name == null || name.isBlank())
				throw iaex("FormDef.Input name must not be null or blank.");
			var t = type;
			if (t == null || t.isBlank())
				t = "text";
			if (! INPUT_TYPES.contains(t))
				throw iaex("FormDef.Input '%s' type must be one of text, textarea, checkbox, toggle, select, action, not '%s'.", name, t);
			var isSelect = "select".equals(t);
			var isAction = "action".equals(t);
			var isTextual = "text".equals(t) || "textarea".equals(t);

			if (isSelect) {
				if (options == null || options.isEmpty())
					throw iaex("FormDef.Input '%s' select must declare at least one option.", name);
				var values = new HashSet<String>();
				for (var o : options) {
					if (o == null)
						throw iaex("FormDef.Input '%s' select option must not be null.", name);
					if (o.value == null)
						throw iaex("FormDef.Input '%s' select option value must not be null.", name);
					if (o.label == null || o.label.isBlank())
						throw iaex("FormDef.Input '%s' select option label must not be null or blank.", name);
					values.add(o.value);
				}
				if (value != null && ! values.contains(value))
					throw iaex("FormDef.Input '%s' select value '%s' does not match any option.", name, value);
			} else if (options != null) {
				throw iaex("FormDef.Input '%s' options are only allowed on a select field.", name);
			}

			if (! isTextual) {
				if (pattern != null)
					throw iaex("FormDef.Input '%s' pattern is only allowed on a text/textarea field.", name);
				if (maxLength != null)
					throw iaex("FormDef.Input '%s' maxLength is only allowed on a text/textarea field.", name);
			}

			if (pattern != null) {
				if (pattern.length() > PATTERN_MAX_LENGTH)
					throw iaex("FormDef.Input '%s' pattern must be at most %s characters.", name, PATTERN_MAX_LENGTH);
				try {
					Pattern.compile(pattern);
				} catch (PatternSyntaxException e) {
					throw iaex("FormDef.Input '%s' pattern does not compile: %s", name, e.getMessage());
				}
			}

			if (maxLength != null && maxLength <= 0)
				throw iaex("FormDef.Input '%s' maxLength must be > 0.", name);

			if (isAction && (actionId == null || actionId.isBlank()))
				throw iaex("FormDef.Input '%s' action must declare an actionId.", name);
			if (! isAction && actionId != null)
				throw iaex("FormDef.Input '%s' actionId is only allowed on an action field.", name);
		}
	}

	/**
	 * The frozen form contract version discriminator.
	 *
	 * <p>
	 * <b>Null</b> until {@link #checked()} is invoked on the serving path (so a raw builder never leaks {@code "1"} on a
	 * nested form while a modal top-level is still unversioned); omitted from the wire while null.
	 */
	public String contractVersion;

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

	/**
	 * Fail-closed bean validation.
	 *
	 * <p>
	 * Does <b>not</b> require {@link #contractVersion} to already be set &mdash; a raw-built form validated directly
	 * must not false-refuse on a null version.  Validating an empty {@link #fields} list is a no-op (a template-only /
	 * fieldless form is a shipped shape).
	 *
	 * @throws IllegalArgumentException If any field is not well-formed, or two fields share a {@code name}.
	 */
	@Override
	public void validate() {
		if (fields == null)
			return;
		var names = new HashSet<String>();
		for (var f : fields) {
			if (f == null)
				throw iaex("FormDef field must not be null.");
			f.validate();
			if (! names.add(f.name))
				throw iaex("FormDef duplicate field name '%s'.", f.name);
		}
	}

	/**
	 * The serving-path hook: stamps {@link #CONTRACT_VERSION} then {@link #validate() validates}.
	 *
	 * <p>
	 * Every app {@code @RestGet} that returns a form-bearing {@link ModalDef} must reach this (via
	 * {@link ModalDef#checked()}) so a malformed form fails at serve time, not silently on the wire.
	 *
	 * @return This object.
	 * @throws IllegalArgumentException If this form is not well-formed.
	 */
	public FormDef checked() {
		contractVersion = CONTRACT_VERSION;
		validate();
		return this;
	}
}
