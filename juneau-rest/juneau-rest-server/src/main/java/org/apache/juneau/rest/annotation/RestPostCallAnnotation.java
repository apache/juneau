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

import static org.apache.juneau.common.utils.CollectionUtils.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

import org.apache.juneau.common.annotation.*;

/**
 * Utility classes and methods for the {@link RestPostCall @RestPostCall} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/LifecycleHooks">Lifecycle Hooks</a>
 * </ul>
 */
public class RestPostCallAnnotation {
	/**
	 * A collection of {@link RestPostCall @RestPostCall annotations}.
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
		RestPostCall[] value();
	}

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AppliedAnnotationObject.BuilderM<Builder> {

		String[] description = {};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(RestPostCall.class);
		}

		/**
		 * Instantiates a new {@link RestPostCall @RestPostCall} object initialized with this builder.
		 *
		 * @return A new {@link RestPostCall @RestPostCall} object.
		 */
		public RestPostCall build() {
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

	}

	private static class Object extends AppliedAnnotationObject implements RestPostCall {

		private final String[] description;

		Object(RestPostCallAnnotation.Builder b) {
			super(b);
			this.description = copyOf(b.description);
			postConstruct();
		}

		@Override /* Overridden from RestPostCall */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final RestPostCall DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}