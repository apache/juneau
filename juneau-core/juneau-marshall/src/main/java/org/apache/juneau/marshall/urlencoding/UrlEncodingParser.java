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
package org.apache.juneau.marshall.urlencoding;

import static org.apache.juneau.commons.utils.SystemUtils.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.uon.*;

/**
 * Parses URL-encoded text into POJO models.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Handles <c>Content-Type</c> types:  <bc>application/x-www-form-urlencoded</bc>
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * Parses URL-Encoded text (e.g. <js>"foo=bar&amp;baz=bing"</js>) into POJOs.
 *
 * <p>
 * Expects parameter values to be in UON notation.
 *
 * <p>
 * This parser uses a state machine, which makes it very fast and efficient.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/UrlEncodingSupport">URL-Encoding Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", // Inheritance depth acceptable for this class hierarchy
	"java:S115", // Constants use UPPER_snakeCase naming convention
})
public class UrlEncodingParser extends UonParser implements UrlEncodingMetaProvider {

	// Property name constants
	private static final String PROP_expandedParams = "expandedParams";

	/**
	 * Builder class.
	 */
	public static class Builder extends UonParser.Builder<Builder> {

		private static final Cache<HashKey,UrlEncodingParser> CACHE = Cache.of(HashKey.class, UrlEncodingParser.class).build();

		boolean expandedParams;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			decoding();
			consumes("application/x-www-form-urlencoded");
			expandedParams = env("UrlEncoding.expandedParams", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			expandedParams = copyFrom.expandedParams;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(UrlEncodingParser copyFrom) {
			super(copyFrom);
			expandedParams = copyFrom.expandedParams;
		}

		@Override /* Overridden from Context.Builder<?> */
		public UrlEncodingParser build() {
			return cache(CACHE).build(UrlEncodingParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Serialize bean property collections/arrays as separate key/value pairs.
		 *
		 * <p>
		 * This is the parser-side equivalent of the {@link UrlEncodingSerializer.Builder#expandedParams()} setting.
		 *
		 * <p>
		 * If <jk>false</jk>, serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
		 * <br>If <jk>true</jk>, serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jk>public class</jk> MyBean {
		 * 		<jk>public</jk> String[] <jf>f1</jf>;
		 * 		<jk>public</jk> List&lt;String&gt; <jf>f2</jf>;
		 * 	}
		 *
		 * 	UrlEncodingParser <jv>parser1</jv> = UrlEncodingParser.<jsf>DEFAULT</jsf>;
		 * 	UrlEncodingParser <jv>parser2</jv> = UrlEncodingParser.<jsm>create</jsm>().expandedParams().build();
		 *
		 * 	MyBean <jv>myBean1</jv> = <jv>parser1</jv>.read(<js>"f1=@(a,b)&amp;f2=@(c,d)"</js>, A.<jk>class</jk>);
		 *
		 * 	MyBean <jv>myBean2</jv> = <jv>parser2</jv>.read(<js>"f1=a&amp;f1=b&amp;f2=c&amp;f2=d"</js>, A.<jk>class</jk>);
		 * </p>
		 *
		 * <p>
		 * This option only applies to beans.
		 *
		 * <h5 class='section'>Notes:</h5><ul>
		 * 	<li class='note'>
		 * 		If parsing multi-part parameters, it's highly recommended to use Collections or Lists
		 * 		as bean property types instead of arrays since arrays have to be recreated from scratch every time a value
		 * 		is added to it.
		 * </ul>
		 *
		 * @return This object.
		 */
		public Builder expandedParams() {
			return expandedParams(true);
		}

		/**
		 * Same as {@link #expandedParams()} but allows you to explicitly specify the value.
		 *
		 * @param value The value for this setting.
		 * @return This object.
		 */
		public Builder expandedParams(boolean value) {
			expandedParams = value;
			return this;
		}

		@Override /* Overridden from Context.Builder<?> */
		public HashKey hashKey() {
			// @formatter:off
			return HashKey.of(
				super.hashKey(),
				expandedParams
			);
			// @formatter:on
		}


	}

	/** Reusable instance of {@link UrlEncodingParser}. */
	public static final UrlEncodingParser DEFAULT = new UrlEncodingParser(create());

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	final boolean expandedParams;

	private final Map<ClassMeta<?>,UrlEncodingClassMeta> urlEncodingClassMetas = new ConcurrentHashMap<>();
	private final Map<BeanPropertyMeta,UrlEncodingBeanPropertyMeta> urlEncodingBeanPropertyMetas = new ConcurrentHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 */
	public UrlEncodingParser(Builder builder) {
		super(builder);
		expandedParams = builder.expandedParams;
	}

	@Override /* Overridden from Context */
	public Builder copy() {
		return new Builder(this);
	}

	@Override /* Overridden from Context */
	public UrlEncodingParserSession.Builder createSession() {
		return UrlEncodingParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public UrlEncodingParserSession getSession() { return createSession().build(); }

	@Override /* Overridden from UrlEncodingMetaProvider */
	public UrlEncodingBeanPropertyMeta getUrlEncodingBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return UrlEncodingBeanPropertyMeta.DEFAULT;
		return urlEncodingBeanPropertyMetas.computeIfAbsent(bpm, k -> new UrlEncodingBeanPropertyMeta(k.getDelegateFor(), this));
	}

	@Override /* Overridden from UrlEncodingMetaProvider */
	public UrlEncodingClassMeta getUrlEncodingClassMeta(ClassMeta<?> cm) {
		return urlEncodingClassMetas.computeIfAbsent(cm, k -> new UrlEncodingClassMeta(k, this));
	}

	/**
	 * Parse bean property collections/arrays as separate key/value pairs.
	 *
	 * @see Builder#expandedParams()
	 * @return
	 * <jk>false</jk> if serializing the array <c>[1,2,3]</c> results in <c>?key=$a(1,2,3)</c>.
	 * <br><jk>true</jk> if serializing the same array results in <c>?key=1&amp;key=2&amp;key=3</c>.
	 */
	protected final boolean isExpandedParams() { return expandedParams; }

	@Override /* Overridden from UonParser */
	protected FluentMap<String,Object> properties() {
		return super.properties()
			.a(PROP_expandedParams, expandedParams);
	}
}