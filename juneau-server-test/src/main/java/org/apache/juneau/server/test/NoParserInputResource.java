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
package org.apache.juneau.server.test;

import java.io.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testNoParserInput",
	serializers=PlainTextSerializer.class
)
public class NoParserInputResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// @Content annotated InputStream.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testInputStream")
	public String testInputStream(@Content InputStream in) throws Exception {
		return IOUtils.read(in);
	}

	//====================================================================================================
	// @Content annotated Reader.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testReader")
	public String testReader(@Content Reader in) throws Exception {
		return IOUtils.read(in);
	}

	//====================================================================================================
	// @Content annotated PushbackReader.
	// This should always fail since the servlet reader is not a pushback reader.
	//====================================================================================================
	@RestMethod(name="PUT", path="/testPushbackReader")
	public String testPushbackReader(@Content PushbackReader in) throws Exception {
		return IOUtils.read(in);
	}
}
