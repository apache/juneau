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
package org.apache.juneau.rest.test;

import static org.apache.juneau.rest.annotation.Inherit.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 * Validates correct parser is used.
 */
@RestResource(
	path="/testParsers",
	parsers=ParsersResource.TestParserA.class,
	serializers=PlainTextSerializer.class
)
public class ParsersResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@Consumes("text/a")
	public static class TestParserA extends ReaderParser {

		public TestParserA(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)("text/a - " + IOUtils.read(session.getReader()).trim());
		}
	}

	//====================================================================================================
	// Parser defined on class.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOnClass")
	public String testParserOnClass(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Parser defined on method.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOnMethod", parsers=TestParserB.class)
	public String testParserOnMethod(@Body String in) {
		return in;
	}

	@Consumes("text/b")
	public static class TestParserB extends ReaderParser {

		public TestParserB(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)("text/b - " + IOUtils.read(session.getReader()).trim());
		}
	}

	//====================================================================================================
	// Parser overridden on method.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserOverriddenOnMethod", parsers={TestParserB.class,TestParserC.class}, parsersInherit=PARSERS)
	public String testParserOverriddenOnMethod(@Body String in) {
		return in;
	}

	@Consumes("text/c")
	public static class TestParserC extends ReaderParser {

		public TestParserC(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)("text/c - " + IOUtils.read(session.getReader()).trim());
		}
	}

	//====================================================================================================
	// Parser with different Accept than Content-Type.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testParserWithDifferentMediaTypes", parsers={TestParserD.class}, parsersInherit=PARSERS)
	public String testParserWithDifferentMediaTypes(@Body String in) {
		return in;
	}

	@Consumes("text/a,text/d")
	public static class TestParserD extends ReaderParser {

		public TestParserD(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)("text/d - " + IOUtils.read(session.getReader()).trim());
		}
	}

	//====================================================================================================
	// Check for valid error response.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testValidErrorResponse")
	public String testValidErrorResponse(@Body String in) {
		return in;
	}
}
