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
package org.apache.juneau.xml.annotation;

import static org.apache.juneau.commons.utils.CollectionUtils.*;

import java.lang.annotation.*;

import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Xml @Xml} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/XmlBasics">XML Basics</a>
 * </ul>
 */
public class XmlAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private XmlAnnotation() {}
	/**
	 * Builder class.
	 *
	 * <h5 class='section'>See Also:</h5><ul>
	 * 	<li class='jm'>{@link org.apache.juneau.BeanContext.Builder#annotations(Annotation...)}
	 * </ul>
	 */
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private String childName = "";
		private String namespace = "";
		private String prefix = "";
		private XmlFormat format = XmlFormat.DEFAULT;

		/**
		 * Constructor.
		 */
		protected Builder() {
			super(Xml.class);
		}

		/**
		 * Instantiates a new {@link Xml @Xml} object initialized with this builder.
		 *
		 * @return A new {@link Xml @Xml} object.
		 */
		public Xml build() {
			return new Object(this);
		}

		/**
		 * Sets the description property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder description(String...value) {
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#childName} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder childName(String value) {
			childName = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#format} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(XmlFormat value) {
			format = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#namespace} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder namespace(String value) {
			namespace = value;
			return this;
		}

		/**
		 * Sets the {@link Xml#prefix} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder prefix(String value) {
			prefix = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Xml {

		private final String[] description;
		private final String childName;
		private final String namespace;
		private final String prefix;
		private final XmlFormat format;

		Object(XmlAnnotation.Builder b) {
			super(b);
			description = copyOf(b.description);
			childName = b.childName;
			format = b.format;
			namespace = b.namespace;
			prefix = b.prefix;
		}

		@Override /* Overridden from Xml */
		public String childName() {
			return childName;
		}

		@Override /* Overridden from Xml */
		public XmlFormat format() {
			return format;
		}

		@Override /* Overridden from Xml */
		public String namespace() {
			return namespace;
		}

		@Override /* Overridden from Xml */
		public String prefix() {
			return prefix;
		}

		@Override /* Overridden from annotation */
		public String[] description() {
			return description;
		}
	}

	/** Default value */
	public static final Xml DEFAULT = create().build();

	/**
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
