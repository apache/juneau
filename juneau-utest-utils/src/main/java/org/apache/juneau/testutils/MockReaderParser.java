//***************************************************************************************************************************
//* Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
//* distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
//* to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
//* with the License.  You may obtain a copy of the License at                                                              *
//*                                                                                                                         *
//*  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
//*                                                                                                                         *
//* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
//* specific language governing permissions and limitations under the License.                                              *
//***************************************************************************************************************************

package org.apache.juneau.testutils;

import java.io.*;
import java.lang.reflect.*;

import org.apache.juneau.*;
import org.apache.juneau.httppart.*;
import org.apache.juneau.parser.*;

/**
 * Utility class for creating mocked reader parsers.
 */
public class MockReaderParser extends ReaderParser implements HttpPartParser {

	//-------------------------------------------------------------------------------------------------------------------
	// Static
	//-------------------------------------------------------------------------------------------------------------------

	public static Builder create() {
		return new Builder();
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	public static class Builder extends ReaderParser.Builder {
		MockReaderParserFunction function;
		MockReaderParserPartFunction partFunction;

		public Builder function(MockReaderParserFunction value) {
			function = value;
			return this;
		}

		public Builder partFunction(MockReaderParserPartFunction value) {
			partFunction = value;
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

	private final MockReaderParserFunction function;
	private final MockReaderParserPartFunction partFunction;

	public MockReaderParser(Builder builder) {
		super(builder);
		this.function = builder.function;
		this.partFunction = builder.partFunction;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T doParse(ParserSession session, ParserPipe pipe, ClassMeta<T> type) throws IOException, ParseException, ExecutableException {
		if (function != null)
			return (T)function.apply((ReaderParserSession)session, pipe.asString(), type);
		return null;
	}

	@Override
	public HttpPartParserSession getPartSession() {
		return new HttpPartParserSession() {
			@Override
			@SuppressWarnings("unchecked")
			public <T> T parse(HttpPartType partType, HttpPartSchema schema, String in, ClassMeta<T> toType) throws ParseException, SchemaValidationException {
				return (T)partFunction.apply(partType, schema, in, toType);
			}
		};
	}

	@Override
	public <T> ClassMeta<T> getClassMeta(Class<T> c) {
		return this.getBeanContext().getClassMeta(c);
	}

	@Override
	public <T> ClassMeta<T> getClassMeta(Type t, Type... args) {
		return this.getBeanContext().getClassMeta(t, args);
	}
}
