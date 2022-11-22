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
package org.apache.juneau.rest.converter;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.http.response.*;

/**
 * Converter for enablement of {@link ObjectIntrospector} support on response objects returned by a
 * <c>@RestOp</c>-annotated method.
 *
 * <p>
 * When enabled, public methods can be called on objects returned through the {@link RestResponse#setContent(Object)}
 * method.
 *
 * <p>
 * Note that opening up public methods for calling through a REST interface can be dangerous, and should be done with
 * caution.
 *
 * <p>
 * Java methods are invoked by passing in the following URL parameters:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<c>&amp;invokeMethod</c> - The Java method name, optionally with arguments if necessary to
 * 		differentiate between methods.
 * 	<li>
 * 		<c>&amp;invokeArgs</c> - The arguments as an array.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='jc'>{@link ObjectIntrospector} - Additional information on introspection of POJO methods.
 * 	<li class='jm'>{@link org.apache.juneau.rest.RestOpContext.Builder#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#jrs.Converters">Converters</a> * </ul>
 */
public final class Introspectable implements RestConverter {

	/**
	 * Swagger parameters for this converter.
	 */
	public static final String SWAGGER_PARAMS= ""
		+ "{in:'query',name:'invokeMethod',description:' The Java method name, optionally with arguments if necessary to differentiate between methods.',examples:{example:'toString'}},"
		+ "{in:'query',name:'invokeArgs',description:'The arguments as an array.',examples:{example:'foo,bar'}}"
	;

	@Override /* RestConverter */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public Object convert(RestRequest req, Object o) throws InternalServerError {
		String method = req.getQueryParam("invokeMethod").orElse(null);
		String args = req.getQueryParam("invokeArgs").orElse(null);
		if (method == null)
			return o;
		try {
			BeanSession bs = req.getBeanSession();
			ObjectSwap swap = bs.getClassMetaForObject(o).getSwap(bs);
			if (swap != null)
				o = swap.swap(bs, o);
			return ObjectIntrospector.create(o, JsonParser.DEFAULT).invokeMethod(method, args);
		} catch (Exception e) {
			return new InternalServerError(e,
				"Error occurred trying to invoke method: {0}",
				e.getLocalizedMessage()
			);
		}
	}
}
