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
package org.apache.juneau.ini;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.collections.*;
import org.apache.juneau.parser.*;

/**
 * Parses INI-formatted text into POJO models.
 *
 * <p>
 * Parses INI files (section/key-value format) into Java beans and maps.
 *
 * <h5 class='topic'>Media types</h5>
 * <p>
 * Consumes: <bc>text/ini, text/x-ini</bc>
 *
 * <h5 class='topic'>Value parsing</h5>
 * <ul class='spaced-list'>
 * 	<li>Unquoted <c>null</c> → Java <jk>null</jk>
 * 	<li>Unquoted <c>true</c>/<c>false</c> → Boolean
 * 	<li>Unquoted numbers → Number (int, long, float, double as appropriate)
 * 	<li>Single-quoted strings (<c>'...'</c>) → String (with <c>''</c> unescaped to single quote)
 * 	<li>Values starting with <c>[</c> or <c>{</c> → Delegated to JSON parser
 * 	<li>ISO 8601 strings → Date, Calendar, or <c>java.time.*</c> when target requires it
 * 	<li>ISO 8601 duration strings → {@link java.time.Duration}
 * 	<li>Other unquoted tokens → String
 * </ul>
 *
 * <h5 class='topic'>Sections</h5>
 * <p>
 * Sections (e.g. <c>[address]</c>) map to nested bean properties or Map properties.
 * Section paths like <c>[employment/company]</c> map to deeply nested bean properties.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Parse INI into a bean</jc>
 * 	MyConfig <jv>config</jv> = IniParser.<jsf>DEFAULT</jsf>.parse(<jv>ini</jv>, MyConfig.<jk>class</jk>);
 *
 * 	<jc>// Parse into a Map</jc>
 * 	Map&lt;String, Object&gt; <jv>map</jv> = IniParser.<jsf>DEFAULT</jsf>.parse(<jv>ini</jv>, Map.<jk>class</jk>, String.<jk>class</jk>, Object.<jk>class</jk>);
 * </p>
 *
 * <h5 class='figure'>Example input:</h5>
 * <p class='bini'>
 * 	<ck>name</ck> = <cv>Alice</cv>
 * 	<ck>age</ck> = <cv>30</cv>
 *
 * 	<cs>[address]</cs>
 * 	<ck>street</ck> = <cv>123 Main St</cv>
 * 	<ck>city</ck> = <cv>Boston</cv>
 * 	<ck>state</ck> = <cv>MA</cv>
 * </p>
 *
 * <h5 class='topic'>Limitations</h5>
 * <p>
 * Parsing into top-level collections, arrays, or scalar types is not supported.
 * The target type must be a bean class or <c>Map&lt;String,?&gt;</c>.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * 	<li class='note'>Values starting with <c>[</c> or <c>{</c> are delegated to the JSON parser.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/IniBasics">INI Basics</a>
 * </ul>
 */
@SuppressWarnings({
	"java:S110", "java:S115"
})
public class IniParser extends ReaderParser implements IniMetaProvider {

	private final java.util.concurrent.ConcurrentHashMap<ClassMeta<?>, IniClassMeta> iniClassMetas = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ConcurrentHashMap<BeanPropertyMeta, IniBeanPropertyMeta> iniBeanPropertyMetas = new java.util.concurrent.ConcurrentHashMap<>();

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder for {@link IniParser}.
	 */
	public static class Builder extends ReaderParser.Builder {

		private static final Cache<HashKey,IniParser> CACHE = Cache.of(HashKey.class, IniParser.class).build();

		protected Builder() {
			consumes("text/ini,text/x-ini");
		}

		protected Builder(Builder copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		protected Builder(IniParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override
		public IniParser build() {
			return cache(CACHE).build(IniParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}
	}

	/** Default parser instance. */
	public static final IniParser DEFAULT = new IniParser(create());

	/**
	 * Creates a new builder.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder.
	 */
	public IniParser(Builder builder) {
		super(builder);
	}

	@Override
	public IniBeanPropertyMeta getIniBeanPropertyMeta(BeanPropertyMeta bpm) {
		if (bpm == null)
			return IniBeanPropertyMeta.DEFAULT;
		return iniBeanPropertyMetas.computeIfAbsent(bpm, k -> new IniBeanPropertyMeta(k, this));
	}

	@Override
	public IniClassMeta getIniClassMeta(ClassMeta<?> cm) {
		return iniClassMetas.computeIfAbsent(cm, k -> new IniClassMeta(k, this));
	}

	@Override
	public IniParserSession.Builder createSession() {
		return IniParserSession.create(this);
	}

	@Override
	public Builder copy() {
		return new Builder(this);
	}
}
