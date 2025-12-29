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
package org.apache.juneau.rest.annotation;

import static org.apache.juneau.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.junit.jupiter.api.*;

class Swagger_RestOp_Parameters extends TestBase {

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
		var s = getSwagger(A.class);
		var x = s.getParameterInfo("/accept","get","header","Accept");

		assertJson("{'in':'header',name:'Accept',type:'string'}", x);

		x = s.getParameterInfo("/acceptCharset","put","header","Accept-Charset");
		assertJson("{'in':'header',name:'Accept-Charset',type:'string'}", x);

		x = s.getParameterInfo("/acceptEncoding","post","header","Accept-Encoding");
		assertJson("{'in':'header',name:'Accept-Encoding',type:'string'}", x);

		x = s.getParameterInfo("/acceptLanguage","delete","header","Accept-Language");
		assertJson("{'in':'header',name:'Accept-Language',type:'string'}", x);

		x = s.getParameterInfo("/authorization","get","header","Authorization");
		assertJson("{'in':'header',name:'Authorization',type:'string'}", x);

		x = s.getParameterInfo("/cacheControl","get","header","Cache-Control");
		assertJson("{'in':'header',name:'Cache-Control',type:'string'}", x);

		x = s.getParameterInfo("/connection","get","header","Connection");
		assertJson("{'in':'header',name:'Connection',type:'string'}", x);

		x = s.getParameterInfo("/contentLength","get","header","Content-Length");
		assertJson("{format:'int64','in':'header',name:'Content-Length',type:'integer'}", x);

		x = s.getParameterInfo("/contentType","get","header","Content-Type");
		assertJson("{'in':'header',name:'Content-Type',type:'string'}", x);

		x = s.getParameterInfo("/date","get","header","Date");
		assertJson("{'in':'header',name:'Date',type:'string'}", x);

		x = s.getParameterInfo("/expect","get","header","Expect");
		assertJson("{'in':'header',name:'Expect',type:'string'}", x);

		x = s.getParameterInfo("/from","get","header","From");
		assertJson("{'in':'header',name:'From',type:'string'}", x);

		x = s.getParameterInfo("/host","get","header","Host");
		assertJson("{'in':'header',name:'Host',type:'string'}", x);

		x = s.getParameterInfo("/ifMatch","get","header","If-Match");
		assertJson("{'in':'header',name:'If-Match',type:'string'}", x);

		x = s.getParameterInfo("/ifModifiedSince","get","header","If-Modified-Since");
		assertJson("{'in':'header',name:'If-Modified-Since',type:'string'}", x);

		x = s.getParameterInfo("/ifNoneMatch","get","header","If-None-Match");
		assertJson("{'in':'header',name:'If-None-Match',type:'string'}", x);

		x = s.getParameterInfo("/ifRange","get","header","If-Range");
		assertJson("{'in':'header',name:'If-Range',type:'string'}", x);

		x = s.getParameterInfo("/ifUnmodifiedSince","get","header","If-Unmodified-Since");
		assertJson("{'in':'header',name:'If-Unmodified-Since',type:'string'}", x);

		x = s.getParameterInfo("/maxForwards","get","header","Max-Forwards");
		assertJson("{format:'int32','in':'header',name:'Max-Forwards',type:'integer'}", x);

		x = s.getParameterInfo("/pragma","get","header","Pragma");
		assertJson("{'in':'header',name:'Pragma',type:'string'}", x);

		x = s.getParameterInfo("/proxyAuthorization","get","header","Proxy-Authorization");
		assertJson("{'in':'header',name:'Proxy-Authorization',type:'string'}", x);

		x = s.getParameterInfo("/range","get","header","Range");
		assertJson("{'in':'header',name:'Range',type:'string'}", x);

		x = s.getParameterInfo("/referer","get","header","Referer");
		assertJson("{'in':'header',name:'Referer',type:'string'}", x);

		x = s.getParameterInfo("/te","get","header","TE");
		assertJson("{'in':'header',name:'TE',type:'string'}", x);

		x = s.getParameterInfo("/upgrade","get","header","Upgrade");
		assertJson("{'in':'header',name:'Upgrade',type:'string'}", x);

		x = s.getParameterInfo("/userAgent","get","header","User-Agent");
		assertJson("{'in':'header',name:'User-Agent',type:'string'}", x);

		x = s.getParameterInfo("/warning","get","header","Warning");
		assertJson("{'in':'header',name:'Warning',type:'string'}", x);
	}
}