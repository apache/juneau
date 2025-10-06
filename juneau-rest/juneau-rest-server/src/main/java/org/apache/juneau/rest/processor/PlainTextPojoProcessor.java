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
package org.apache.juneau.rest.processor;

import java.io.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.util.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.response.*;

/**
 * Response handler for plain-old Java objects when a serializer match is not found and they're asking for plain/text or anything.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public final class PlainTextPojoProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException, NotAcceptable, BasicHttpException {
		RestRequest req = opSession.getRequest();
		RestResponse res = opSession.getResponse();
		String accept = req.getHeaderParam("Accept").orElse("*/*");

		if (res.getSerializerMatch().isPresent() || ! (Utils.isEmpty(accept) || accept.startsWith("text/plain") || accept.contains("*/*")))
			return NEXT;

		Object o = res.getContent(Object.class);

		if (Utils.isEmpty(res.getContentType()))
			res.setHeader(ContentType.TEXT_PLAIN);

		FinishablePrintWriter w = res.getNegotiatedWriter();
		if (o == null)
			w.append("null");
		else
			w.append(req.getBeanSession().getClassMetaForObject(o).toString(o));
		w.flush();
		w.finish();

		return FINISHED;
	}
}