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

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.annotation.*;

/**
 * JUnit automated testcase resource.
 */
@RestResource(
	path="/testGroups"
)
public class GroupsResource extends RestServlet {
	private static final long serialVersionUID = 1L;

	@Produces({"text/s1","text/s2"})
	public static class SSerializer extends WriterSerializer {
		@Override /* Serializer */
		protected void doSerialize(SerializerSession session, Object output) throws Exception {
			session.getWriter().write("text/s," + output);
		}
	}

	@Consumes({"text/p1","text/p2"})
	public static class PParser extends ReaderParser {
		@SuppressWarnings("unchecked")
		@Override /* Parser */
		protected <T> T doParse(ParserSession session, ClassMeta<T> type) throws Exception {
			return (T)IOUtils.read(session.getReader());
		}
	}


	@Override /* RestServlet */
	public SerializerGroup createSerializers(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		return new SerializerGroup().append(SSerializer.class).setProperties(properties).addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
	}

	@Override /* RestServlet */
	public ParserGroup createParsers(ObjectMap properties, Class<?>[] beanFilters, Class<?>[] pojoSwaps) throws Exception {
		return new ParserGroup().append(PParser.class).setProperties(properties).addBeanFilters(beanFilters).addPojoSwaps(pojoSwaps);
	}

	//====================================================================================================
	// Serializer defined on class.
	//====================================================================================================
	@RestMethod(name="GET", path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_get() {
		return "GET";
	}

	@RestMethod(name="PUT", path="/testSerializerDefinedOnClass")
	public String testSerializerDefinedOnClass_put(@Content String in) {
		return in;
	}
}
