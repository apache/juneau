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

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;

/**
 * Utility classes and methods for the {@link Tag @XXX} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * </ul>
 */
public class TagAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Tag DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
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

		ExternalDocs externalDocs = ExternalDocsAnnotation.DEFAULT;
		String name="";
		String[] description={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Tag.class);
		}

		/**
		 * Instantiates a new {@link Tag @Tag} object initialized with this builder.
		 *
		 * @return A new {@link Tag @Tag} object.
		 */
		public Tag build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Tag#description} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Tag#externalDocs} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder externalDocs(ExternalDocs value) {
			this.externalDocs = value;
			return this;
		}

		/**
		 * Sets the {@link Tag#name} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder name(String value) {
			this.name = value;
			return this;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements Tag {

		private final ExternalDocs externalDocs;
		private final String name;
		private final String[] description;

		Impl(Builder b) {
			super(b);
			this.description = copyOf(b.description);
			this.externalDocs = b.externalDocs;
			this.name = b.name;
			postConstruct();
		}

		@Override /* Tag */
		public String[] description() {
			return description;
		}

		@Override /* Tag */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* Tag */
		public String name() {
			return name;
		}
	}
}