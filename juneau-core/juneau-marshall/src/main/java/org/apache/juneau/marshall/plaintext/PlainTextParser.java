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
package org.apache.juneau.marshall.plaintext;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.swap.*;

/**
 * Parsers HTTP plain text request bodies into Group 5 POJOs.
 *
 * <p>
 * See <a class="doclink" href="https://juneau.apache.org/docs/topics/PojoCategories">POJO Categories</a>.
 *
 * <h5 class='topic'>Media types</h5>
 *
 * Handles <c>Accept</c> types:  <bc>text/plain</bc>
 * <p>
 * Produces <c>Content-Type</c> types:  <bc>text/plain</bc>
 *
 * <h5 class='topic'>Description</h5>
 *
 * Essentially just converts plain text to POJOs via static <c>fromString()</c> or <c>valueOf()</c>, or
 * through constructors that take a single string argument.
 *
 * <p>
 * Also parses objects using a transform if the object class has an {@link ObjectSwap ObjectSwap&lt;?,String&gt;} transform
 * defined on it.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 */
@SuppressWarnings({
	"java:S115" // Constants use UPPER_snakeCase convention
})
public class PlainTextParser extends ReaderParser implements PlainTextMetaProvider {

	// Argument name constants for assertArgNotNull
	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	public static class Builder extends ReaderParser.Builder<Builder> {

		private static final Cache<HashKey,PlainTextParser> CACHE = Cache.of(HashKey.class, PlainTextParser.class).build();

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			consumes("text/plain");
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(PlainTextParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public PlainTextParser build() {
			return cache(CACHE).build(PlainTextParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}


	}

	/** Default parser, all default settings.*/
	public static final PlainTextParser DEFAULT = new PlainTextParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	private final Map<ClassMeta<?>,PlainTextClassMeta> plainTextClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,PlainTextBeanPropertyMeta> plainTextBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public PlainTextParser(Builder builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public PlainTextParserSession.Builder createSession() {
		return PlainTextParserSession.create(this);
	}

	@Override /* Overridden from PlainTextMetaProvider */
	public PlainTextBeanPropertyMeta getPlainTextBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return PlainTextBeanPropertyMeta.DEFAULT;
		return plainTextBeanPropertyMetas.computeIfAbsent(bpm, k -> new PlainTextBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from PlainTextMetaProvider */
	public PlainTextClassMeta getPlainTextClassMeta(ClassMeta<?> cm) {
		return plainTextClassMetas.computeIfAbsent(cm, k -> new PlainTextClassMeta(k, this));
	}

	@Override /* Overridden from Context */
	public PlainTextParserSession getSession() { return createSession().build(); }
}