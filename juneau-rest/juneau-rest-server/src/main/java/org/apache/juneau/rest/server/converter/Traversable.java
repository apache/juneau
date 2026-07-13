/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.server.converter;

import static org.apache.juneau.commons.utils.Shorts.*;

import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.objecttools.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.rest.server.*;

/**
 * Converter for enabling path-addressing support on response objects returned by a <c>@RestOp</c>-annotated method.
 *
 * <p>
 * When enabled, objects in a POJO tree returned by the REST method can be addressed through additional URL path
 * information.
 *
 * <p class='bjava'>
 * 	<jc>// Resource method on resource "http://localhost:8080/sample/addressBook"</jc>
 * 	<ja>@RestOp</ja>(method=<jsf>GET</jsf>, converters=Traversable.<jk>class</jk>)
 * 	<jk>public void</jk> doGet(RestRequest <jv>req</jv>, RestResponse <jv>res</jv>) {
 * 		<jk>return new</jk> AddressBook();
 * 	}
 * </p>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='ja'>{@link RestOp#converters()} - Registering converters with REST resources.
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/Converters">Converters</a>
 * </ul>
 */
public class Traversable implements RestConverter {

	@Override /* Overridden from RestConverter */
	public Object convert(RestRequest req, Object o) throws BasicHttpException, InternalServerError {
		if (o == null)
			return null;

		String pathRemainder = req.getPathRemainder().orElse(null);

		if (nn(pathRemainder)) {
			try {
				MarshallingSession bs = req.getMarshallingSession();
				var swap = bs.getClassMetaForObject(o).getSwap(bs);
				if (nn(swap))
					o = swap.swap(bs, o);
				ReaderParser rp = req.getContent().getParserMatch().map(ParserMatch::getParser).filter(ReaderParser.class::isInstance).map(ReaderParser.class::cast).orElse(null);
				var or = PathTraversal.create(o, rp);
				o = or.get(pathRemainder);
			} catch (PathTraversalException e) {
				throw new BasicHttpException(e.getStatus(), null, e);
			} catch (Exception t) {
				throw new InternalServerError(t);
			}
		}

		return o;
	}
}