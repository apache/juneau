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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.rest.testutils.TestUtils.*;
import org.apache.juneau.*;
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

public class Swagger_RestOp_Parameters extends SimpleTestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Swagger on default headers.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@RestGet
		public String accept(Accept accept) {
			return accept.getValue();
		}
		@RestPut
		public String acceptCharset(AcceptCharset acceptCharset) {
			return acceptCharset.getValue();
		}
		@RestPost
		public String acceptEncoding(AcceptEncoding acceptEncoding) {
			return acceptEncoding.getValue();
		}
		@RestDelete
		public String acceptLanguage(AcceptLanguage acceptLanguage) {
			return acceptLanguage.getValue();
		}
		@RestOp
		public String authorization(Authorization authorization) {
			return authorization.getValue();
		}
		@RestOp
		public String cacheControl(CacheControl cacheControl) {
			return cacheControl.getValue();
		}
		@RestOp
		public String connection(Connection connection) {
			return connection.getValue();
		}
		@RestOp
		public String contentLength(ContentLength contentLength) {
			return contentLength.getValue();
		}
		@RestOp
		public String contentType(ContentType contentType) {
			return contentType.getValue();
		}
		@RestOp
		public String date(org.apache.juneau.http.header.Date date) {
			return date.getValue();
		}
		@RestOp
		public String expect(Expect expect) {
			return expect.getValue();
		}
		@RestOp
		public String from(From from) {
			return from.getValue();
		}
		@RestOp
		public String host(Host host) {
			return host.getValue();
		}
		@RestOp
		public String ifMatch(IfMatch ifMatch) {
			return ifMatch.getValue();
		}
		@RestOp
		public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
			return ifModifiedSince.getValue();
		}
		@RestOp
		public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
			return ifNoneMatch.getValue();
		}
		@RestOp
		public String ifRange(IfRange ifRange) {
			return ifRange.getValue();
		}
		@RestOp
		public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
			return ifUnmodifiedSince.getValue();
		}
		@RestOp
		public String maxForwards(MaxForwards maxForwards) {
			return maxForwards.getValue();
		}
		@RestOp
		public String pragma(Pragma pragma) {
			return pragma.getValue();
		}
		@RestOp
		public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
			return proxyAuthorization.getValue();
		}
		@RestOp
		public String range(Range range) {
			return range.getValue();
		}
		@RestOp
		public String referer(Referer referer) {
			return referer.getValue();
		}
		@RestOp
		public String te(TE te) {
			return te.getValue();
		}
		@RestOp
		public String upgrade(Upgrade upgrade) {
			return upgrade.getValue();
		}
		@RestOp
		public String userAgent(UserAgent userAgent) {
			return userAgent.getValue();
		}
		@RestOp
		public String warning(Warning warning) {
			return warning.getValue();
		}
	}

	@Test void a01_headerParameters() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/accept","get","header","Accept");
		assertJson(x, "{'in':'header',name:'Accept',type:'string'}");

		x = s.getParameterInfo("/acceptCharset","put","header","Accept-Charset");
		assertJson(x, "{'in':'header',name:'Accept-Charset',type:'string'}");

		x = s.getParameterInfo("/acceptEncoding","post","header","Accept-Encoding");
		assertJson(x, "{'in':'header',name:'Accept-Encoding',type:'string'}");

		x = s.getParameterInfo("/acceptLanguage","delete","header","Accept-Language");
		assertJson(x, "{'in':'header',name:'Accept-Language',type:'string'}");

		x = s.getParameterInfo("/authorization","get","header","Authorization");
		assertJson(x, "{'in':'header',name:'Authorization',type:'string'}");

		x = s.getParameterInfo("/cacheControl","get","header","Cache-Control");
		assertJson(x, "{'in':'header',name:'Cache-Control',type:'string'}");

		x = s.getParameterInfo("/connection","get","header","Connection");
		assertJson(x, "{'in':'header',name:'Connection',type:'string'}");

		x = s.getParameterInfo("/contentLength","get","header","Content-Length");
		assertJson(x, "{'in':'header',name:'Content-Length',type:'integer',format:'int64'}");

		x = s.getParameterInfo("/contentType","get","header","Content-Type");
		assertJson(x, "{'in':'header',name:'Content-Type',type:'string'}");

		x = s.getParameterInfo("/date","get","header","Date");
		assertJson(x, "{'in':'header',name:'Date',type:'string'}");

		x = s.getParameterInfo("/expect","get","header","Expect");
		assertJson(x, "{'in':'header',name:'Expect',type:'string'}");

		x = s.getParameterInfo("/from","get","header","From");
		assertJson(x, "{'in':'header',name:'From',type:'string'}");

		x = s.getParameterInfo("/host","get","header","Host");
		assertJson(x, "{'in':'header',name:'Host',type:'string'}");

		x = s.getParameterInfo("/ifMatch","get","header","If-Match");
		assertJson(x, "{'in':'header',name:'If-Match',type:'string'}");

		x = s.getParameterInfo("/ifModifiedSince","get","header","If-Modified-Since");
		assertJson(x, "{'in':'header',name:'If-Modified-Since',type:'string'}");

		x = s.getParameterInfo("/ifNoneMatch","get","header","If-None-Match");
		assertJson(x, "{'in':'header',name:'If-None-Match',type:'string'}");

		x = s.getParameterInfo("/ifRange","get","header","If-Range");
		assertJson(x, "{'in':'header',name:'If-Range',type:'string'}");

		x = s.getParameterInfo("/ifUnmodifiedSince","get","header","If-Unmodified-Since");
		assertJson(x, "{'in':'header',name:'If-Unmodified-Since',type:'string'}");

		x = s.getParameterInfo("/maxForwards","get","header","Max-Forwards");
		assertJson(x, "{'in':'header',name:'Max-Forwards',type:'integer',format:'int32'}");

		x = s.getParameterInfo("/pragma","get","header","Pragma");
		assertJson(x, "{'in':'header',name:'Pragma',type:'string'}");

		x = s.getParameterInfo("/proxyAuthorization","get","header","Proxy-Authorization");
		assertJson(x, "{'in':'header',name:'Proxy-Authorization',type:'string'}");

		x = s.getParameterInfo("/range","get","header","Range");
		assertJson(x, "{'in':'header',name:'Range',type:'string'}");

		x = s.getParameterInfo("/referer","get","header","Referer");
		assertJson(x, "{'in':'header',name:'Referer',type:'string'}");

		x = s.getParameterInfo("/te","get","header","TE");
		assertJson(x, "{'in':'header',name:'TE',type:'string'}");

		x = s.getParameterInfo("/upgrade","get","header","Upgrade");
		assertJson(x, "{'in':'header',name:'Upgrade',type:'string'}");

		x = s.getParameterInfo("/userAgent","get","header","User-Agent");
		assertJson(x, "{'in':'header',name:'User-Agent',type:'string'}");

		x = s.getParameterInfo("/warning","get","header","Warning");
		assertJson(x, "{'in':'header',name:'Warning',type:'string'}");
	}
}