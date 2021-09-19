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
package org.apache.juneau.rest.converters;

import static org.apache.juneau.rest.HttpRuntimeException.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Converter for enabling of {@link PojoRest} support on response objects returned by a <c>@RestOp</c>-annotated method.
 *
 * <p>
 * When enabled, objects in a POJO tree returned by the REST method can be addressed through additional URL path
 * information.
 *
 * <p class='bcode w800'>
 * 	<jc>// Resource method on resource "http://localhost:8080/sample/addressBook"</jc>
 * 	<ja>@RestOp</ja>(method=<jsf>GET</jsf>, converters=Traversable.<jk>class</jk>)
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		<jk>return new</jk> AddressBook();
 * 	}
 * </p>
 *
 * <ul class='seealso'>
 * 	<li class='jc'>{@link PojoRest} - Additional information on addressing elements in a POJO tree using URL notation.
 * 	<li class='jm'>{@link RestOpContextBuilder#converters()} - Registering converters with REST resources.
 * 	<li class='link'>{@doc RestConverters}
 * </ul>
 */
public final class Traversable implements RestConverter {

	@Override /* RestConverter */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public Object convert(RestRequest req, Object o) throws BasicHttpException, InternalServerError {
		if (o == null)
			return null;

		String pathRemainder = req.getPathRemainder().orElse(null);

		if (pathRemainder != null) {
			try {
				BeanSession bs = req.getBeanSession();
				PojoSwap swap = bs.getClassMetaForObject(o).getSwap(bs);
				if (swap != null)
					o = swap.swap(bs, o);
				PojoRest p = new PojoRest(o, req.getBody().getReaderParser());
				o = p.get(pathRemainder);
			} catch (PojoRestException e) {
				throw new BasicHttpException(e.getStatus(), e);
			} catch (Throwable t) {
				throw toHttpException(t, InternalServerError.class);
			}
		}

		return o;
	}
}
