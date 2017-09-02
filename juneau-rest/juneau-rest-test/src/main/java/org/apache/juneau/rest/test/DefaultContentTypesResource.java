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
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testDefaultContentTypes",
	defaultRequestHeaders={" Accept : text/s2 "," Content-Type : text/p2 "},
	parsers={DefaultContentTypesResource.P1.class,DefaultContentTypesResource.P2.class}, serializers={DefaultContentTypesResource.S1.class,DefaultContentTypesResource.S2.class}
)
@SuppressWarnings("synthetic-access")
public class DefaultContentTypesResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	public static class P1 extends DummyParser { public P1(PropertyStore ps) {super(ps, "p1", "text/p1");}}

	public static class P2 extends DummyParser { public P2(PropertyStore ps) {super(ps, "p2", "text/p2");}}

	public static class P3 extends DummyParser { public P3(PropertyStore ps) {super(ps, "p3", "text/p3");}}

	public static class S1 extends DummySerializer { public S1(PropertyStore ps) {super(ps, "s1", "text/s1");}}

	public static class S2 extends DummySerializer { public S2(PropertyStore ps) {super(ps, "s2", "text/s2");}}

	public static class S3 extends DummySerializer { public S3(PropertyStore ps) {super(ps, "s3", "text/s3");}}

	/**
	 * Test that default Accept and Content-Type headers on servlet annotation are picked up.
	 */
	@RestMethod(name="PUT", path="/testDefaultHeadersOnServletAnnotation")
	public String testDefaultHeadersOnServletAnnotation(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodParsersSerializers", parsers=P3.class, serializers=S3.class)
	public String testRestMethodParsersSerializers(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on servlet annotation are picked up
	// when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodAddParsersSerializers", parsers=P3.class, parsersInherit=PARSERS, serializers=S3.class, serializersInherit=SERIALIZERS)
	public String testRestMethodAddParsersSerializers(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Various Accept incantations.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testAccept")
	public String testAccept(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// when @RestMethod.parsers/serializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodParserSerializerAnnotations", defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, serializers=S3.class)
	public String testRestMethodParserSerializerAnnotations(@Body String in) {
		return in;
	}

	//====================================================================================================
	// Test that default Accept and Content-Type headers on method annotation are picked up
	// 	when @RestMethod.addParsers/addSerializers annotations are used.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testRestMethodAddParsersSerializersAnnotations", defaultRequestHeaders={"Accept: text/s3","Content-Type: text/p3"}, parsers=P3.class, parsersInherit=PARSERS, serializers=S3.class, serializersInherit=SERIALIZERS)
	public String testRestMethodAddParsersSerializersAnnotations(@Body String in) {
		return in;
	}

	public static class DummyParser extends ReaderParser {

		private String name;

		private DummyParser(PropertyStore propertyStore, String name, String...consumes) {
			super(propertyStore, consumes);
			this.name = name;
		}

		@Override /* Parser */
		public ReaderParserSession createSession(ParserSessionArgs args) {
			return new ReaderParserSession(args) {

				@Override /* ParserSession */
				@SuppressWarnings("unchecked")
				protected <T> T doParse(ParserPipe pipe, ClassMeta<T> type) throws Exception {
					return (T)name;
				}
			};
		}
	}

	public static class DummySerializer extends WriterSerializer {

		private String name;

		private DummySerializer(PropertyStore propertyStore, String name, String produces) {
			super(propertyStore, produces);
			this.name = name;
		}

		@Override /* Serializer */
		public WriterSerializerSession createSession(SerializerSessionArgs args) {
			return new WriterSerializerSession(args) {

				@Override /* SerializerSession */
				protected void doSerialize(SerializerPipe out, Object o) throws Exception {
					out.getWriter().write(name + "/" + o);
				}
			};
		}
	}
}
