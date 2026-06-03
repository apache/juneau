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
package org.apache.juneau.html;

import org.apache.juneau.commons.collections.*;

/**
 * Serializes POJOs to HTTP responses as stripped HTML.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/html+stripped</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/html</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Produces the same output as {@link HtmlDocSerializer}, but without the header and body tags and page title and
 * description.
 * Used primarily for JUnit testing the {@link HtmlDocSerializer} class.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/HtmlBasics">HTML Basics</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for HtmlStrippedDocSerializer hierarchy
})
public class HtmlStrippedDocSerializer extends HtmlSerializer {

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends HtmlSerializer.Builder<SELF> {

		private static final Cache<HashKey,HtmlStrippedDocSerializer> CACHE = Cache.of(HashKey.class, HtmlStrippedDocSerializer.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			produces("text/html");
			accept("text/html+stripped");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(HtmlStrippedDocSerializer copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public HtmlStrippedDocSerializer build() {
			return cache(CACHE).build(HtmlStrippedDocSerializer.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link HtmlStrippedDocSerializer#create()} / {@link HtmlStrippedDocSerializer#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(HtmlStrippedDocSerializer copyFrom) {
			super(copyFrom);
		}

		DefaultBuilder(Builder<?> copyFrom) {
			super(copyFrom);
		}

		@Override /* Overridden from Context.Builder<?> */
		public DefaultBuilder copy() {
			return new DefaultBuilder(this);
		}
	}

	/** Default serializer, all default settings. */
	public static final HtmlStrippedDocSerializer DEFAULT = new HtmlStrippedDocSerializer(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	@SuppressWarnings({
		"java:S1452" // Builder<?> wildcard return intentional; callers chain via fluent API without needing the concrete type
	})
	public static Builder<?> create() {
		return new DefaultBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public HtmlStrippedDocSerializer(Builder<?> builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder<?> copy() {
		return new DefaultBuilder(this);
	}

	@Override /* Overridden from Context */
	public HtmlStrippedDocSerializerSession.Builder<?> createSession() {
		return HtmlStrippedDocSerializerSession.create(this);
	}

	@Override /* Overridden from Context */
	public HtmlStrippedDocSerializerSession getSession() { return createSession().build(); }
}