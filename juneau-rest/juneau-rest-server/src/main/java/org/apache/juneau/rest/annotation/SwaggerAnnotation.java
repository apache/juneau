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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.internal.ArrayUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;

/**
 * Utility classes and methods for the {@link Swagger @Swagger} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Swagger">Swagger</a>
 * </ul>
 */
public class SwaggerAnnotation {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Default value */
	public static final Swagger DEFAULT = create().build();

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
	public static boolean empty(Swagger a) {
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

		Contact contact = ContactAnnotation.DEFAULT;
		ExternalDocs externalDocs = ExternalDocsAnnotation.DEFAULT;
		License license = LicenseAnnotation.DEFAULT;
		String version="";
		String[] description={}, termsOfService={}, title={}, value={};
		Tag[] tags={};

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Swagger.class);
		}

		/**
		 * Instantiates a new {@link Swagger @Swagger} object initialized with this builder.
		 *
		 * @return A new {@link Swagger @Swagger} object.
		 */
		public Swagger build() {
			return new Impl(this);
		}

		/**
		 * Sets the {@link Swagger#contact()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder contact(Contact value) {
			this.contact = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#description()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			this.description = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#externalDocs()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder externalDocs(ExternalDocs value) {
			this.externalDocs = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#license()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder license(License value) {
			this.license = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#tags()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder tags(Tag...value) {
			this.tags = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#termsOfService()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder termsOfService(String...value) {
			this.termsOfService = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#title()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder title(String...value) {
			this.title = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#value()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder value(String...value) {
			this.value = value;
			return this;
		}

		/**
		 * Sets the {@link Swagger#version()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder version(String value) {
			this.version = value;
			return this;
		}

		// <FluentSetters>

		// </FluentSetters>
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Implementation
	//-----------------------------------------------------------------------------------------------------------------

	private static class Impl extends AnnotationImpl implements Swagger {

		private final Contact contact;
		private final ExternalDocs externalDocs;
		private final License license;
		private final String version;
		private final String[] description, termsOfService, title, value;
		private final Tag[] tags;

		Impl(Builder b) {
			super(b);
			this.contact = b.contact;
			this.description = copyOf(b.description);
			this.externalDocs = b.externalDocs;
			this.license = b.license;
			this.tags = copyOf(b.tags);
			this.termsOfService = copyOf(b.termsOfService);
			this.title = copyOf(b.title);
			this.value = copyOf(b.value);
			this.version = b.version;
			postConstruct();
		}

		@Override /* Swagger */
		public Contact contact() {
			return contact;
		}

		@Override /* Swagger */
		public String[] description() {
			return description;
		}

		@Override /* Swagger */
		public ExternalDocs externalDocs() {
			return externalDocs;
		}

		@Override /* Swagger */
		public License license() {
			return license;
		}

		@Override /* Swagger */
		public Tag[] tags() {
			return tags;
		}

		@Override /* Swagger */
		public String[] termsOfService() {
			return termsOfService;
		}

		@Override /* Swagger */
		public String[] title() {
			return title;
		}

		@Override /* Swagger */
		public String[] value() {
			return value;
		}

		@Override /* Swagger */
		public String version() {
			return version;
		}
	}
}