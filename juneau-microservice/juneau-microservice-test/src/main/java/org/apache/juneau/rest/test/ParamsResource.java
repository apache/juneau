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

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.testutils.DTOs;
import org.apache.juneau.transforms.*;
import org.apache.juneau.urlencoding.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testParams",
	serializers=PlainTextSerializer.class,
	allowedMethodParams="*",
	pojoSwaps={CalendarSwap.DateMedium.class},
	messages="ParamsResource"
)
public class ParamsResource extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// @HasQuery annotation - GET
	//====================================================================================================
	@RestMethod(name=GET, path="/testHasParamGet/*")
	public String testHasParamGet(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
		RequestQuery q = req.getQuery();
		return "p1=["+p1+","+q.containsKey("p1")+"],p2=["+p2+","+q.containsKey("p2")+"]";
	}

	//====================================================================================================
	// @HasQuery annotation - POST
	//====================================================================================================
	@RestMethod(name=POST, path="/testHasParamPost/*")
	public String testHasParamPost(RestRequest req, @HasFormData("p1") boolean p1, @HasFormData("p2") Boolean p2) throws Exception {
		RequestFormData f = req.getFormData();
		return "p1=["+p1+","+f.containsKey("p1")+"],p2=["+p2+","+f.containsKey("p2")+"]";
	}

	//====================================================================================================
	// @HasQuery annotation - GET
	//====================================================================================================
	@RestMethod(name=GET, path="/testHasQParamGet/*")
	public String testHasQParamGet(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
		RequestQuery q = req.getQuery();
		return "p1=["+p1+","+q.containsKey("p1")+"],p2=["+p2+","+q.containsKey("p2")+"]";
	}

	//====================================================================================================
	// @HasQuery annotation - POST
	//====================================================================================================
	@RestMethod(name=POST, path="/testHasQParamPost/*")
	public String testHasQParamPost_post(RestRequest req, @HasQuery("p1") boolean p1, @HasQuery("p2") Boolean p2) throws Exception {
		RequestQuery q = req.getQuery();
		return "p1=["+p1+","+q.containsKey("p1")+"],p2=["+p2+","+q.containsKey("p2")+"]";
	}

	//====================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using URLENC_expandedParams property.
	// A simple round-trip test to verify that both serializing and parsing works.
	//====================================================================================================
	@RestMethod(name=POST, path="/testFormPostsWithMultiParamsUsingProperty",
		properties={
			@Property(name=UrlEncodingSerializer.URLENC_expandedParams, value="true"),
			@Property(name=UrlEncodingParser.URLENC_expandedParams, value="true")
		}
	)
	public DTOs.B testFormPostsWithMultiParamsViaProperty(@Body DTOs.B content) throws Exception {
		return content;
	}

	//====================================================================================================
	// Test multi-part parameter keys on bean properties of type array/Collection (i.e. &key=val1,&key=val2)
	// using @UrlEncoding(expandedParams=true) annotation.
	// A simple round-trip test to verify that both serializing and parsing works.
	//====================================================================================================
	@RestMethod(name=POST, path="/testFormPostsWithMultiParamsUsingAnnotation")
	public DTOs.C testFormPostsWithMultiParamsUsingAnnotation(@Body DTOs.C content) throws Exception {
		return content;
	}
}
