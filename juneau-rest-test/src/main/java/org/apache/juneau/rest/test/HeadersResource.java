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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testHeaders",
	serializers=HeadersResource.PlainTextAnythingSerializer.class,
	parsers=HeadersResource.PlainTextAnythingParser.class,
	encoders=HeadersResource.IdentityAnythingEncoder.class
)
public class HeadersResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="GET", path="/accept")
	public String accept(Accept accept) {
		return accept.toString();
	}

	@RestMethod(name="GET", path="/acceptEncoding")
	public String acceptEncoding(AcceptEncoding acceptEncoding) {
		System.err.println(acceptEncoding);
		return acceptEncoding.toString();
	}

	@RestMethod(name="GET", path="/contentType")
	public String contentType(ContentType contentType) {
		return contentType.toString();
	}

	@Produces("*/*")
	public static class PlainTextAnythingSerializer extends PlainTextSerializer {
		public PlainTextAnythingSerializer(PropertyStore propertyStore) {
			super(propertyStore);
		}
	}

	@Consumes("*/*")
	public static class PlainTextAnythingParser extends PlainTextParser {
		public PlainTextAnythingParser(PropertyStore propertyStore) {
			super(propertyStore);
		}
	}

	public static class IdentityAnythingEncoder extends IdentityEncoder {
		@Override /* Encoder */
		public String[] getCodings() {
			return new String[]{"*"};
		}
	}
}
