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
package org.apache.juneau.utest.utils;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.common.*;
import org.apache.juneau.common.reflect.*;
import org.apache.juneau.common.utils.*;
import org.apache.juneau.parser.*;

/**
 * Utility class for creating mocked stream parser.
 */
public class FakeStreamParser extends InputStreamParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	public static class Builder extends InputStreamParser.Builder {
		Function3<InputStreamParserSession,byte[],ClassMeta<?>,Object> function;

		public Builder function(Function3<InputStreamParserSession,byte[],ClassMeta<?>,Object> value) {
			function = value;
			return this;
		}

		@Override
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}

		@Override
		public Builder copy() {
			return this;
		}
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final Function3<InputStreamParserSession,byte[],ClassMeta<?>,Object> function;

	public FakeStreamParser(Builder builder) {
		super(builder);
		this.function = builder.function;
	}

	@Override
	public <T> T doParse(ParserSession session, ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		if (function != null)
			return type.cast(function.apply((InputStreamParserSession)session, IOUtils.readBytes(pipe.getInputStream()), type));
		return null;
	}
}