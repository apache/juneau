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

import static org.apache.juneau.internal.IOUtils.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testFormData"
)
public class FormDataResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@RestMethod(name="POST", path="/*")
	public Reader test(RestRequest req) throws IOException {
		return new StringReader("Content-Type=["+req.getContentType()+"], contents=["+read(req.getReader())+"]");
	}

	//====================================================================================================
	// Default values.
	//====================================================================================================

	@RestMethod(name="POST", path="/defaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
	public ObjectMap defaultFormData(RequestFormData formData) {
		return new ObjectMap()
			.append("f1", formData.getString("f1"))
			.append("f2", formData.getString("f2"))
			.append("f3", formData.getString("f3"));
	}

	@RestMethod(name="POST", path="/annotatedFormData")
	public ObjectMap annotatedFormData(@FormData("f1") String f1, @FormData("f2") String f2, @FormData("f3") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}

	@RestMethod(name="POST", path="/annotatedFormDataDefault")
	public ObjectMap annotatedFormDataDefault(@FormData(value="f1",def="1") String f1, @FormData(value="f2",def="2") String f2, @FormData(value="f3",def="3") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}

	@RestMethod(name="POST", path="/annotatedAndDefaultFormData", defaultFormData={"f1:1","f2=2"," f3 : 3 "})
	public ObjectMap annotatedAndDefaultFormData(@FormData(value="f1",def="4") String f1, @FormData(value="f2",def="5") String f2, @FormData(value="f3",def="6") String f3) {
		return new ObjectMap()
			.append("f1", f1)
			.append("f2", f2)
			.append("f3", f3);
	}
}
