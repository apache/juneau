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
package org.apache.juneau.urlencoding.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.svl.*;

/**
 * Utility classes and methods for the {@link UrlEncoding @UrlEncoding} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingBasics">URL-Encoding Basics</a>
 * </ul>
 */
public class UrlEncodingAnnotation {
	/**
	 * Applies targeted {@link UrlEncoding} annotations to a {@link org.apache.juneau.Context.Builder}.
	 */
	public static class Apply extends AnnotationApplier<UrlEncoding,Context.Builder> {

		/**
		 * Constructor.
		 *
		 * @param vr The resolver for resolving values in annotations.
		 */
		public Apply(VarResolverSession vr) {
			super(UrlEncoding.class, Context.Builder.class, vr);
		}

		@Override
		public void apply(AnnotationInfo<UrlEncoding> ai, Context.Builder b) {
			UrlEncoding a = ai.inner();
			if (ArrayUtils2.isEmptyArray(a.on()) && ArrayUtils2.isEmptyArray(a.onClass()))
				return;
			b.annotations(copy(a, vr()));
		}
	}

	/**
	 * A collection of {@link UrlEncoding @UrlEncoding annotations}.
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
		UrlEncoding[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends TargetedAnnotationTMFBuilder<Builder> {

		boolean expandedParams;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(UrlEncoding.class);
		}

		/**
		 * Instantiates a new {@link UrlEncoding @UrlEncoding} object initialized with this builder.
		 *
		 * @return A new {@link UrlEncoding @UrlEncoding} object.
		 */
		public UrlEncoding build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link UrlEncoding#expandedParams} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder expandedParams(boolean value) {
			this.expandedParams = value;
			return this;
		}

	}

	private static class Impl extends TargetedAnnotationTImpl implements UrlEncoding {

		private final boolean expandedParams;

		Impl(Builder b) {
			super(b);
			this.expandedParams = b.expandedParams;
			postConstruct();
		}

		@Override /* Overridden from UrlEncoding */
		public boolean expandedParams() {
			return expandedParams;
		}
	}

	/** Default value */
	public static final UrlEncoding DEFAULT = create().build();

	/**
	 * Creates a copy of the specified annotation.
	 *
	 * @param a The annotation to copy.s
	 * @param r The var resolver for resolving any variables.
	 * @return A copy of the specified annotation.
	 */
	public static UrlEncoding copy(UrlEncoding a, VarResolverSession r) {
		// @formatter:off
		return
			create()
			.expandedParams(a.expandedParams())
			.on(r.resolve(a.on()))
			.onClass(a.onClass())
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