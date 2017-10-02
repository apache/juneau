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

import java.io.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * Validates the functionality of <ja>@RequestBeans</ja>.
 */
@RestResource(
	path="/testRequestBeanProxy"
)
@SuppressWarnings("serial")
public class RequestBeanProxyResource extends ResourceJena {

	@RestMethod(name=GET, path="/echoQuery")
	public Reader echoQuery(RestRequest req) throws Exception {
		return new StringReader(req.getQuery().toString(true));
	}

	@RestMethod(name=POST, path="/echoFormData")
	public Reader echoFormData(RestRequest req) throws Exception {
		return new StringReader(req.getFormData().toString(true));
	}

	@RestMethod(name=GET, path="/echoHeaders")
	public Reader echoHeaders(RestRequest req) throws Exception {
		return new StringReader(req.getHeaders().subset("a,b,c,d,e,f,g,h,i,a1,a2,a3,a4,b1,b2,b3,b4,c1,c2,c3,c4").toString(true));
	}

	@RestMethod(name=GET, path="/echoPath/*")
	public Reader echoPath(RestRequest req) throws Exception {
		return new StringReader(req.getPathMatch().getRemainder());
	}
}
