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

import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.helper.*;

@RestResource(
	path="/",
	children={
		DebugResource.class,
		LogsResource.class,
		AcceptCharsetResource.class,
		BeanContextPropertiesResource.class,
		BpiResource.class,
		CallbackStringsResource.class,
		CharsetEncodingsResource.class,
		ClientFuturesResource.class,
		ClientVersionResource.class,
		ConfigResource.class,
		ErrorConditionsResource.class,
		TransformsResource.class,
		FormDataResource.class,
		GroupsResource.class,
		GzipResource.TestGzipOff.class,
		GzipResource.TestGzipOn.class,
		HeadersResource.class,
		HtmlDocResource.class,
		HtmlDocLinksResource.class,
		InheritanceResource.TestEncoders.class,
		InheritanceResource.TestTransforms.class,
		InheritanceResource.TestParsers.class,
		InheritanceResource.TestProperties.class,
		InheritanceResource.TestSerializers.class,
		InterfaceProxyResource.class,
		LargePojosResource.class,
		MessagesResource.Messages2Resource.class,
		MessagesResource.class,
		NlsResource.class,
		NlsPropertyResource.class,
		OnPostCallResource.class,
		OnPreCallResource.class,
		OptionsWithoutNlsResource.class,
		OverlappingMethodsResource.class,
		ParsersResource.class,
		PathResource.class,
		PathsResource.class,
		PathVariablesResource.class,
		PropertiesResource.class,
		QueryResource.class,
		RequestBeanProxyResource.class,
		RestClient2Resource.class,
		RestHooksInitResource.class,
		RestHooksResource.class,
		SerializersResource.class,
		StaticFilesResource.class,
		ThirdPartyProxyResource.class,
		UrisResource.class,
		ShutdownResource.class,
	}
)
public class Root extends BasicRestServlet {
	private static final long serialVersionUID = 1L;

	@RestMethod(name=GET, path="/")
	public ChildResourceDescriptions doGet(RestRequest req) throws Exception {
		return new ChildResourceDescriptions(getContext(), req);
	}
}