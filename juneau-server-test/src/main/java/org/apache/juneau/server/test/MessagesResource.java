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

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.*;
import org.apache.juneau.server.annotation.*;
import org.apache.juneau.transform.*;

/**
 * JUnit automated testcase resource.
 * Validates that resource bundles can be defined on both parent and child classes.
 */
@RestResource(
	path="/testMessages",
	messages="MessagesResource",
	pojoSwaps={
		MessagesResource.ResourceBundleSwap.class
	}
)
public class MessagesResource extends RestServletDefault {
	private static final long serialVersionUID = 1L;

	//====================================================================================================
	// Return contents of resource bundle.
	//====================================================================================================
	@RestMethod(name="GET", path="/test")
	public Object test(@Messages ResourceBundle nls) {
		return nls;
	}


	@SuppressWarnings("serial")
	@RestResource(
		path="/testMessages2",
		messages="Messages2Resource"
	)
	public static class Messages2Resource extends MessagesResource {}

	public static class ResourceBundleSwap extends MapSwap<ResourceBundle> {
		@Override /* Transform */
		public ObjectMap swap(BeanSession session, ResourceBundle o) throws SerializeException {
			ObjectMap m = new ObjectMap();
			for (String k : o.keySet())
				m.put(k, o.getString(k));
			return m;
		}
	}
}
