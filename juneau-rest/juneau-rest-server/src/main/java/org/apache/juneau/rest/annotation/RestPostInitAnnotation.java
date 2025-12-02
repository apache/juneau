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
package org.apache.juneau.rest.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link RestPostInit @RestPostInit} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/LifecycleHooks">Lifecycle Hooks</a>
 * </ul>
 */
public class RestPostInitAnnotation {
	/**
	 * A collection of {@link RestPostInit @RestPostInit annotations}.
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
		RestPostInit[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderM {

		private String[] description = {};
		private boolean childFirst;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestPostInit.class);
		}

		/**
		 * Instantiates a new {@link RestPostInit @RestPostInit} object initialized with this builder.
		 *
		 * @return A new {@link RestPostInit @RestPostInit} object.
		 */
		public RestPostInit build() {
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
		 * Sets the {@link RestPostInit#childFirst()} property on this annotation.
		 *
		 * @return This object.
		 */
		public Builder childFirst() {
			this.childFirst = true;
			return this;
		}

		@Override /* Overridden from AppliedAnnotationObject.Builder */
		public Builder on(String...value) {
			super.on(value);
			return this;
		}
		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(java.lang.reflect.Method...value) {
			super.on(value);
			return this;
		}

	
		@Override /* Overridden from AppliedAnnotationObject.BuilderM */
		public Builder on(org.apache.juneau.commons.reflect.MethodInfo...value) {
			super.on(value);
			return this;
		}

	}

	private static class Object extends AppliedAnnotationObject implements RestPostInit {

		private final String[] description;
		private final boolean childFirst;

		Object(RestPostInitAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			childFirst = b.childFirst;
		}

		@Override /* Overridden from RestHook */
		public boolean childFirst() {
			return childFirst;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final RestPostInit DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}