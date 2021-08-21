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
package org.apache.juneau.testutils;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;

/**
 * Utility class for creating mocked stream parser.
 */
public class MockStreamParser extends InputStreamParser {

	private final MockStreamParserFunction function;

	protected MockStreamParser(Builder builder) {
		super(builder);
		this.function = builder.function;
	}

	public static Builder create() {
		return new Builder();
	}

	@Override /* Parser */
	public InputStreamParserSession createSession(ParserSessionArgs args) {
		return new InputStreamParserSession(this, args) {
			@SuppressWarnings("unchecked")
			@Override
			protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
				if (function != null)
					return (T)function.apply(this, IOUtils.readBytes(pipe.getInputStream()), type);
				return null;
			}
		};
	}

	public static class Builder extends InputStreamParserBuilder {
		MockStreamParserFunction function;

		public Builder function(MockStreamParserFunction value) {
			function = value;
			return this;
		}

		@Override
		public Builder consumes(String value) {
			super.consumes(value);
			return this;
		}
	}

	@Override
	public Builder copy() {
		throw new NoSuchMethodError("Not implemented.");
	}
}
