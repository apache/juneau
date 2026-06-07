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
package org.apache.juneau.marshall.parser;

import static org.apache.juneau.commons.utils.AssertionUtils.*;

import org.apache.juneau.marshall.*;

/**
 * Subclass of {@link Parser} for byte-based parsers.
 *
 * <h5 class='topic'>Description</h5>
 * <p>
 * This class is typically the parent class of all character-based parsers.
 * It has 1 abstract method to implement on the session object...
 * <ul>
 * 	<li><c>parse(ParserSession, ClassMeta)</c>
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/SerializersAndParsers">Serializers and Parsers</a>

 * </ul>
 */
@SuppressWarnings({
	"java:S115" // ARG_xxx constants use camelCase after prefix intentionally (constructor arg name keys, not enum-style constants)
})
public class InputStreamParser extends Parser {

	private static final String ARG_copyFrom = "copyFrom";

	/**
	 * Builder class.
	 */
	@SuppressWarnings({
		"java:S119" // 'SELF' (CRTP self-type) is intentional and clearer than a single-letter name.
	})
	public abstract static class Builder<SELF extends Builder<SELF>> extends Parser.Builder<SELF> {

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(Builder<?> copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 * 	<br>Cannot be <jk>null</jk>.
		 */
		protected Builder(InputStreamParser copyFrom) {
			super(assertArgNotNull(ARG_copyFrom, copyFrom));
		}

		@Override /* Overridden from Context.Builder<?> */
		public InputStreamParser build() {
			return build(InputStreamParser.class);
		}

		@Override /* Overridden from Context.Builder<?> */
		public abstract SELF copy();


	}

	/**
	 * Concrete default builder leaf for the non-subclassed {@link InputStreamParser#create()} / {@link InputStreamParser#copy()} path.
	 */
	public static final class DefaultBuilder extends Builder<DefaultBuilder> {

		DefaultBuilder() {}

		DefaultBuilder(InputStreamParser copyFrom) {
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
	protected InputStreamParser(Builder<?> builder) {
		super(builder);
	}

	@Override /* Overridden from Context */
	public InputStreamParserSession.Builder<?> createSession() {
		return InputStreamParserSession.create(this);
	}

	@Override /* Overridden from Context */
	public InputStreamParserSession getSession() { return createSession().build(); }

	@Override /* Overridden from Parser */
	public final boolean isReaderParser() { return false; }

	/**
	 * Binary input format.
	 *
	 * <p>
	 * Resolves from the configured {@link MarshallingContext#getBinaryFormat()}.
	 *
	 * @return The binary wire format used by this parser.
	 */
	protected final BinaryFormat getBinaryFormat() { return getMarshallingContext().getBinaryFormat(); }
}
