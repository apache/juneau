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
package org.apache.juneau.httppart;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.utils.*;

/**
 * An implementation of {@link HttpPartParser} that takes in the strings and tries to convert them to POJOs using constructors and static create methods.
 *
 * <p>
 * The class being created must be one of the following in order to convert it from a string:
 *
 * <ul>
 * 	<li>
 * 		An <jk>enum</jk>.
 * 	<li>
 * 		Have a public constructor with a single <c>String</c> parameter.
 * 	<li>
 * 		Have one of the following public static methods that takes in a single <c>String</c> parameter:
 * 		<ul>
 * 			<li><c>fromString</c>
 * 			<li><c>fromValue</c>
 * 			<li><c>valueOf</c>
 * 			<li><c>parse</c>
 * 			<li><c>parseString</c>
 * 			<li><c>forName</c>
 * 			<li><c>forString</c>
 * 	</ul>
 * </ul>
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#jm.HttpPartSerializersParsers">HTTP Part Serializers and Parsers</a>
 * </ul>
 */
public class SimplePartParser extends BaseHttpPartParser {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

	/** Reusable instance of {@link SimplePartParser}, all default settings. */
	public static final SimplePartParser DEFAULT = create().build();

	/** Reusable instance of {@link SimplePartParser}, all default settings. */
	public static final SimplePartParserSession DEFAULT_SESSION = DEFAULT.getPartSession();

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends BaseHttpPartParser.Builder {

		private static final Cache<HashKey,SimplePartParser> CACHE = Cache.of(HashKey.class, SimplePartParser.class).build();

		/**
		 * Constructor.
		 */
		protected Builder() {
			super();
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
		}

		@Override
		public SimplePartParser build() {
			return cache(CACHE).build(SimplePartParser.class);
		}

		@Override
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends Context> value) {
			super.cache(value);
			return this;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Constructor
	 *
	 * @param builder The builder for this object.
	 */
	public SimplePartParser(Builder builder) {
		super(builder);
	}

	@Override
	public SimplePartParserSession getPartSession() {
		return new SimplePartParserSession();
	}
}
