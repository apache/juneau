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
package org.apache.juneau.http.annotation;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Utility classes and methods for the {@link Contact @Contact} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class ContactAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Contact DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Returns <jk>true</jk> if the specified annotation contains all default values.
	 *
	 * @param a The annotation to check.
	 * @return <jk>true</jk> if the specified annotation contains all default values.
	 */
	public static boolean empty(Contact a) {
		return a == null || DEFAULT.equals(a);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationBuilder {

		String email="", name="", url="";

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Contact.class);
		}

		/**
		 * Instantiates a new {@link Contact @Contact} object initialized with this builder.
		 *
		 * @return A new {@link Contact @Contact} object.
		 */
		public Contact build() {
			return new Impl(this);
		}


		/**
		 * Sets the {@link Contact#email} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder email(String value) {
			this.email = value;
			return this;
		}

		/**
		 * Sets the {@link Contact#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		/**
		 * Sets the {@link Contact#url} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder url(String value) {
			this.url = value;
			return this;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements Contact {

		private final String email, name, url;

		Impl(Builder b) {
			super(b);
			this.email = b.email;
			this.name = b.name;
			this.url = b.url;
			postConstruct();
		}

		@Override /* Contact */
		public String email() {
			return email;
		}

		@Override /* Contact */
		public String name() {
			return name;
		}

		@Override /* Contact */
		public String url() {
			return url;
		}
	}
}