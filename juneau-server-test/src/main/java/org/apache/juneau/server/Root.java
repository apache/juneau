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
package org.apache.juneau.server;

import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.server.labels.*;

@RestResource(
	path="/",
	children={
		AcceptCharsetResource.class,
		BeanContextPropertiesResource.class,
		CallbackStringsResource.class,
		CharsetEncodingsResource.class,
		ClientVersionResource.class,
		ConfigResource.class,
		ContentResource.class,
		DefaultContentTypesResource.class,
		ErrorConditionsResource.class,
		TransformsResource.class,
		GroupsResource.class,
		GzipResource.TestGzipOff.class,
		GzipResource.TestGzipOn.class,
		InheritanceResource.TestEncoders.class,
		InheritanceResource.TestTransforms.class,
		InheritanceResource.TestParsers.class,
		InheritanceResource.TestProperties.class,
		InheritanceResource.TestSerializers.class,
		LargePojosResource.class,
		MessagesResource.Messages2Resource.class,
		MessagesResource.class,
		NlsResource.class,
		NlsPropertyResource.class,
		NoParserInputResource.class,
		OnPostCallResource.class,
		OnPreCallResource.class,
		OptionsWithoutNlsResource.class,
		OverlappingMethodsResource.class,
		ParamsResource.class,
		ParsersResource.class,
		PathResource.class,
		PathsResource.class,
		PropertiesResource.class,
		RestClient2Resource.class,
		SerializersResource.class,
		StaticFilesResource.class,
		UrisResource.class,
		UrlContentResource.class,
		ShutdownResource.class
	}
)
public class Root extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	@RestMethod(name="GET", path="/")
	public ChildResourceDescriptions doGet(RestRequest req) {
		return new ChildResourceDescriptions(this, req);
	}
}