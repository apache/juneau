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
package org.apache.juneau.server.converters;

import javax.servlet.http.*;

import org.apache.juneau.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.server.*;
import org.apache.juneau.utils.*;

/**
 * Converter for enablement of {@link PojoRest} support on response objects returned by a <code>@RestMethod</code> method.
 * <p>
 * 	When enabled, objects in a POJO tree returned by the REST method can be addressed through additional URL path information.
 *
 * <p class='bcode'>
 * 	<jc>// Resource method on resource "http://localhost:8080/sample/addressBook"</jc>
 * 	<ja>@RestMethod</ja>(name=<js>"GET"</js>, converters=Traversable.<jk>class</jk>)
 * 	<jk>public void</jk> doGet(RestRequest req, RestResponse res) {
 * 		<jk>return new</jk> AddressBook();
 * 	}
 *
 * 	<jc>// Sample usage</jc>
 * 	<jk>public static void</jk> main(String[] args) {
 * 		<jc>// Get the zip code of the 2nd address of the first person in the address book.</jc>
 * 		RestCall r = <jk>new</jk> RestClient().doGet(<js>"http://localhost:8080/sample/addressBook/0/addresses/1/zip"</js>);
 * 		<jk>int</jk> zip = r.getResponse(Integer.<jk>class</jk>);
 * 	}
 * </p>
 * <p>
 * 	See {@link PojoRest} for additional information on addressing elements in a POJO tree using URL notation.
 */
public final class Traversable implements RestConverter {

	@Override /* RestConverter */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object convert(RestRequest req, Object o, ClassMeta cm) throws RestException {
		if (o == null)
			return null;

		if (req.getPathRemainder() != null) {
			try {
				if (cm.getPojoSwap() != null)
					o = cm.getPojoSwap().swap(o, req.getBeanContext());
				PojoRest p = new PojoRest(o, req.getReaderParser());
				o = p.get(req.getPathRemainder());
			} catch (SerializeException e) {
				throw new RestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			} catch (PojoRestException e) {
				throw new RestException(e.getStatus(), e);
			}
		}

		return o;
	}
}
