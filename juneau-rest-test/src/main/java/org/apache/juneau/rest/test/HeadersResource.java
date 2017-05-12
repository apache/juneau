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
	encoders=HeadersResource.IdentityAnythingEncoder.class,
	paramResolvers=HeadersResource.CustomHeaderParam.class
)
public class HeadersResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// HTTP 1.1 headers
	//====================================================================================================

	@RestMethod(name="GET", path="/accept")
	public String accept(Accept accept) {
		return accept.toString();
	}
	@RestMethod(name="GET", path="/acceptCharset")
	public String acceptCharset(AcceptCharset acceptCharset) {
		return acceptCharset.toString();
	}
	@RestMethod(name="GET", path="/acceptEncoding")
	public String acceptEncoding(AcceptEncoding acceptEncoding) {
		return acceptEncoding.toString();
	}
	@RestMethod(name="GET", path="/acceptLanguage")
	public String acceptLanguage(AcceptLanguage acceptLanguage) {
		return acceptLanguage.toString();
	}
	@RestMethod(name="GET", path="/authorization")
	public String authorization(Authorization authorization) {
		return authorization.toString();
	}
	@RestMethod(name="GET", path="/cacheControl")
	public String cacheControl(CacheControl cacheControl) {
		return cacheControl.toString();
	}
	@RestMethod(name="GET", path="/connection")
	public String connection(Connection connection) {
		return connection.toString();
	}
	@RestMethod(name="GET", path="/contentLength")
	public String contentLength(ContentLength contentLength) {
		return contentLength.toString();
	}
	@RestMethod(name="GET", path="/contentType")
	public String contentType(ContentType contentType) {
		return contentType.toString();
	}
	@RestMethod(name="GET", path="/date")
	public String date(org.apache.juneau.http.Date date) {
		return date.toString();
	}
	@RestMethod(name="GET", path="/expect")
	public String expect(Expect expect) {
		return expect.toString();
	}
	@RestMethod(name="GET", path="/from")
	public String from(From from) {
		return from.toString();
	}
	@RestMethod(name="GET", path="/host")
	public String host(Host host) {
		return host.toString();
	}
	@RestMethod(name="GET", path="/ifMatch")
	public String IfMatch(IfMatch ifMatch) {
		return ifMatch.toString();
	}
	@RestMethod(name="GET", path="/ifModifiedSince")
	public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
		return ifModifiedSince.toString();
	}
	@RestMethod(name="GET", path="/ifNoneMatch")
	public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
		return ifNoneMatch.toString();
	}
	@RestMethod(name="GET", path="/ifRange")
	public String ifRange(IfRange ifRange) {
		return ifRange.toString();
	}
	@RestMethod(name="GET", path="/ifUnmodifiedSince")
	public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
		return ifUnmodifiedSince.toString();
	}
	@RestMethod(name="GET", path="/maxForwards")
	public String maxForwards(MaxForwards maxForwards) {
		return maxForwards.toString();
	}
	@RestMethod(name="GET", path="/pragma")
	public String pragma(Pragma pragma) {
		return pragma.toString();
	}
	@RestMethod(name="GET", path="/proxyAuthorization")
	public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
		return proxyAuthorization.toString();
	}
	@RestMethod(name="GET", path="/range")
	public String range(Range range) {
		return range.toString();
	}
	@RestMethod(name="GET", path="/referer")
	public String referer(Referer referer) {
		return referer.toString();
	}
	@RestMethod(name="GET", path="/te")
	public String te(TE te) {
		return te.toString();
	}
	@RestMethod(name="GET", path="/upgrade")
	public String upgrade(Upgrade upgrade) {
		return upgrade.toString();
	}
	@RestMethod(name="GET", path="/userAgent")
	public String userAgent(UserAgent userAgent) {
		return userAgent.toString();
	}
	@RestMethod(name="GET", path="/warning")
	public String warning(Warning warning) {
		return warning.toString();
	}
	@RestMethod(name="GET", path="/customHeader")
	public String customHeader(CustomHeader customHeader) {
		return customHeader.toString();
	}

	public static class CustomHeaderParam extends RestParam {
		public CustomHeaderParam() {
			super(RestParamType.HEADER, "Custom", CustomHeader.class);
		}
		@Override
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return new CustomHeader(req.getHeader("Custom"));
		}
	}

	public static class CustomHeader {
		public String value;
		public CustomHeader(String value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return value;
		}
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

	//====================================================================================================
	// Default values.
	//====================================================================================================

	@RestMethod(name="GET", path="/defaultRequestHeaders", defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
	public ObjectMap defaultRequestHeaders(RequestHeaders headers) {
		return new ObjectMap()
			.append("h1", headers.getFirst("H1"))
			.append("h2", headers.getFirst("H2"))
			.append("h3", headers.getFirst("H3"));
	}

	@RestMethod(name="GET", path="/defaultRequestHeadersCaseInsensitive", defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
	public ObjectMap defaultRequestHeadersCaseInsensitive(RequestHeaders headers) {
		return new ObjectMap()
			.append("h1", headers.getFirst("h1"))
			.append("h2", headers.getFirst("h2"))
			.append("h3", headers.getFirst("h3"));
	}

	@RestMethod(name="GET", path="/annotatedHeaders")
	public ObjectMap annotatedHeaders(@Header("H1") String h1, @Header("H2") String h2, @Header("H3") String h3) {
		return new ObjectMap()
			.append("h1", h1)
			.append("h2", h2)
			.append("h3", h3);
	}

	@RestMethod(name="GET", path="/annotatedHeadersCaseInsensitive")
	public ObjectMap annotatedHeadersCaseInsensitive(@Header("h1") String h1, @Header("h2") String h2, @Header("h3") String h3) {
		return new ObjectMap()
			.append("h1", h1)
			.append("h2", h2)
			.append("h3", h3);
	}

	@RestMethod(name="GET", path="/annotatedHeadersDefault")
	public ObjectMap annotatedHeadersDefault(@Header(value="h1",def="1") String h1, @Header(value="h2",def="2") String h2, @Header(value="h3",def="3") String h3) {
		return new ObjectMap()
			.append("h1", h1)
			.append("h2", h2)
			.append("h3", h3);
	}

	@RestMethod(name="GET", path="/annotatedAndDefaultHeaders", defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
	public ObjectMap annotatedAndDefaultHeaders(@Header(value="h1",def="4") String h1, @Header(value="h2",def="5") String h2, @Header(value="h3",def="6") String h3) {
		return new ObjectMap()
			.append("h1", h1)
			.append("h2", h2)
			.append("h3", h3);
	}
}
