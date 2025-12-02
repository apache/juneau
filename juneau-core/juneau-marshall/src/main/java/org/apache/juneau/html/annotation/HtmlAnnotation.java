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
package org.apache.juneau.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.commons.annotation.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link Html @Html} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
@SuppressWarnings("rawtypes")
public class HtmlAnnotation {
	/**
	 * Applies targeted {@link Html} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<Html,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(Html.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<Html> ai, Context.Builder b) {
			Html a = ai.inner();
			if (isEmptyArray(a.on()) && isEmptyArray(a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link Html @Html annotations}.
	 */
	@Documented
	@Target({ METHOD, TYPE })
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 *
		 * @return The annotation value.
		 */
		Html[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderTMF {

		private String[] description = {};
		private String anchorText = "", link = "", style = "";
		private HtmlFormat format = HtmlFormat.HTML;
		private boolean noTableHeaders, noTables;
		private Class<? extends HtmlRender> render = HtmlRender.class;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Html.class);
		}

		/**
		 * Sets the {@link Html#anchorText()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder anchorText(String value) {
			this.anchorText = value;
			return this;
		}

		/**
		 * Instantiates a new {@link Html @Html} object initialized with this builder.
		 *
		 * @return A new {@link Html @Html} object.
		 */
		public Html build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Html#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(HtmlFormat value) {
			this.format = value;
			return this;
		}

		/**
		 * Sets the {@link Html#link()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder link(String value) {
			this.link = value;
			return this;
		}

		/**
		 * Sets the {@link Html#noTableHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noTableHeaders(boolean value) {
			this.noTableHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Html#noTables()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noTables(boolean value) {
			this.noTables = value;
			return this;
		}

		/**
		 * Sets the {@link Html#render()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder render(Class<? extends HtmlRender> value) {
			this.render = value;
			return this;
		}

		/**
		 * Sets the {@link Html#style()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder style(String value) {
			this.style = value;
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedOnClassAnnotationObject.Builder */
		public Builder onClass(Class<?>...value) {
			super.onClass(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(Method...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderMF */
		public Builder on(Field...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder on(ClassInfo...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderT */
		public Builder onClass(ClassInfo...value) {
			super.onClass(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderTMF */
		public Builder on(FieldInfo...value) {
			super.on(value);
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.BuilderTMF */
		public Builder on(MethodInfo...value) {
			super.on(value);
			return this;
		}

	}

	private static class Object extends AppliedOnClassAnnotationObject implements Html {

		private final String[] description;
		private final boolean noTableHeaders, noTables;
		private final Class<? extends HtmlRender> render;
		private final String anchorText, link, style;
		private final HtmlFormat format;

		Object(HtmlAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			anchorText = b.anchorText;
			format = b.format;
			link = b.link;
			noTableHeaders = b.noTableHeaders;
			noTables = b.noTables;
			render = b.render;
			style = b.style;
		}

		@Override /* Overridden from Html */
		public String anchorText() {
			return anchorText;
		}

		@Override /* Overridden from Html */
		public HtmlFormat format() {
			return format;
		}

		@Override /* Overridden from Html */
		public String link() {
			return link;
		}

		@Override /* Overridden from Html */
		public boolean noTableHeaders() {
			return noTableHeaders;
		}

		@Override /* Overridden from Html */
		public boolean noTables() {
			return noTables;
		}

		@Override /* Overridden from Html */
		public Class<? extends HtmlRender> render() {
			return render;
		}

		@Override /* Overridden from Html */
		public String style() {
			return style;
		}

		@Override /* Overridden from Html */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Html DEFAULT = create().build();

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static Html copy(Html a, VarResolverSession r) {
		// @formatter:off
		return
			create()
			.anchorText(r.resolve(a.anchorText()))
			.format(a.format())
			.link(r.resolve(a.link()))
			.noTableHeaders(a.noTableHeaders())
			.noTables(a.noTables())
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.render(a.render())
			.build();
		// @formatter:on
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(Class<?>...on) {
		return create().on(on);
	}

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @param on The targets this annotation applies to.
	 * @return A new builder object.
	 */
	public static Builder create(String...on) {
		return create().on(on);
	}
}