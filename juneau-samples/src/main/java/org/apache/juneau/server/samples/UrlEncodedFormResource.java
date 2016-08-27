/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.server.samples;

import java.io.*;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.transforms.*;

/**
 * Sample REST resource for loading URL-Encoded form posts into POJOs.
 */
@RestResource(
	path="/urlEncodedForm",
	messages="nls/UrlEncodedFormResource"
)
public class UrlEncodedFormResource extends Resource {
	private static final long serialVersionUID = 1L;

	/** GET request handler */
	@RestMethod(name="GET", path="/")
	public ReaderResource doGet(RestRequest req) throws IOException {
		return req.getReaderResource("UrlEncodedForm.html", true);
	}

	/** POST request handler */
	@RestMethod(name="POST", path="/")
	public Object doPost(@Content FormInputBean input) throws Exception {
		// Just mirror back the request
		return input;
	}

	public static class FormInputBean {
		public String aString;
		public int aNumber;
		@BeanProperty(transform=CalendarSwap.ISO8601DT.class)
		public Calendar aDate;
	}
}
