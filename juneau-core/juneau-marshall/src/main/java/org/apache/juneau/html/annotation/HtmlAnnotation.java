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

import org.apache.juneau.html.*;
import org.apache.juneau.commons.annotation.*;

/**
 * Utility classes and methods for the {@link Html @Html} annotation.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"rawtypes" // Raw types necessary for annotation handler registration
})
public class HtmlAnnotation {

	/**
	 * Prevents instantiation.
	 */
	private HtmlAnnotation() {}

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
	public static class Builder extends AnnotationObject.Builder {

		private String[] description = {};
		private String anchorText = "";
		private String link = "";
		private String style = "";
		private HtmlFormat format = HtmlFormat.HTML;
		private boolean noTableHeaders;
		private boolean noTables;
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
			anchorText = value;
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
			description = value;
			return this;
		}

		/**
		 * Sets the {@link Html#format()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder format(HtmlFormat value) {
			format = value;
			return this;
		}

		/**
		 * Sets the {@link Html#link()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder link(String value) {
			link = value;
			return this;
		}

		/**
		 * Sets the {@link Html#noTableHeaders()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noTableHeaders(boolean value) {
			noTableHeaders = value;
			return this;
		}

		/**
		 * Sets the {@link Html#noTables()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder noTables(boolean value) {
			noTables = value;
			return this;
		}

		/**
		 * Sets the {@link Html#render()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder render(Class<? extends HtmlRender> value) {
			render = value;
			return this;
		}

		/**
		 * Sets the {@link Html#style()} property on this annotation.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder style(String value) {
			style = value;
			return this;
		}

	}

	@SuppressWarnings({
		"java:S2160" // equals() inherited from AnnotationObject compares all annotation interface methods; subclass fields are accessed via those methods
	})
	private static class Object extends AnnotationObject implements Html {

		private final String[] description;
		private final boolean noTableHeaders;
		private final boolean noTables;
		private final Class<? extends HtmlRender> render;
		private final String anchorText;
		private final String link;
		private final String style;
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
	 * Instantiates a new builder for this class.
	 *
	 * @return A new builder object.
	 */
	public static Builder create() {
		return new Builder();
	}
}
