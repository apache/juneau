// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.html.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.BeanContext.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link HtmlLink @HtmlLink} annotation.
 */
public class HtmlLinkAnnotation {

	/** Default value */
	public static final HtmlLink DEFAULT = create().build();

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

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static HtmlLink copy(HtmlLink a, VarResolverSession r) {
		return
			create()
			.nameProperty(r.resolve(a.nameProperty()))
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
			.uriProperty(r.resolve(a.uriProperty()))
			.build();
	}

	/**
	 * Builder class for the {@link HtmlLink} annotation.
	 *
	 * <ul class='seealso'>
	 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTBuilder {

		String nameProperty="", uriProperty="";

		/**
		 * Constructor.
		 */
		public Builder() {
			super(HtmlLink.class);
		}

		/**
		 * Instantiates a new {@link HtmlLink @HtmlLink} object initialized with this builder.
		 *
		 * @return A new {@link HtmlLink @HtmlLink} object.
		 */
		public HtmlLink build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link HtmlLink#nameProperty()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder nameProperty(String value) {
			this.nameProperty = value;
			return this;
		}

		/**
		 * Sets the {@link HtmlLink#uriProperty()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object (for method chaining).
		 */
		public Builder uriProperty(String value) {
			this.uriProperty = value;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - TargetedAnnotationBuilder */
		public Builder on(String...values) {
			super.on(values);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder on(java.lang.Class<?>...value) {
			super.on(value);
			return this;
		}

		@Override /* GENERATED - TargetedAnnotationTBuilder */
		public Builder onClass(java.lang.Class<?>...value) {
			super.onClass(value);
			return this;
		}

		// </FluentSetters>
	}

	private static class Impl extends TargetedAnnotationTImpl implements HtmlLink {

		private final String nameProperty, uriProperty;

		Impl(Builder b) {
			super(b);
			this.nameProperty = b.nameProperty;
			this.uriProperty = b.uriProperty;
			postConstruct();
		}

		@Override /* HtmlLink */
		public String nameProperty() {
			return nameProperty;
		}

		@Override /* HtmlLink */
		public String uriProperty() {
			return uriProperty;
		}
	}

	/**
	 * Applies targeted {@link HtmlLink} annotations to a {@link ContextPropertiesBuilder}.
	 */
	public static class Apply extends ConfigApply<HtmlLink> {

		/**
		 * Constructor.
		 *
		 * @param c The annotation class.
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(Class<HtmlLink> c, VarResolverSession vr) {
			super(c, vr);
		}

		@Override
		public void apply(AnnotationInfo<HtmlLink> ai, ContextPropertiesBuilder b) {
			HtmlLink a = ai.getAnnotation();

			if (isEmpty(a.on()) && isEmpty(a.onClass()))
				return;

			b.prependTo(BEAN_annotations, copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link HtmlLink @HtmlLink annotations}.
	 */
	@Documented
	@Target({METHOD,TYPE})
	@Retention(RUNTIME)
	@Inherited
	public static @interface Array {

		/**
		 * The child annotations.
		 */
		HtmlLink[] value();
	}
}