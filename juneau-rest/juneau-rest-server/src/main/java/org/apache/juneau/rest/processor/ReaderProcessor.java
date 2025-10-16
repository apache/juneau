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
package org.apache.juneau.rest.processor;

import static org.apache.juneau.common.utils.IOUtils.*;

import java.io.*;

import org.apache.juneau.rest.*;

/**
 * Response processor for {@link Reader} objects.
 *
 * <p>
 * Simply pipes the contents of the {@link Reader} to {@link RestResponse#getNegotiatedWriter()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ResponseProcessors">Response Processors</a>
 * </ul>
 */
public class ReaderProcessor implements ResponseProcessor {

	@Override /* Overridden from ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		RestResponse res = opSession.getResponse();
		Reader r = res.getContent(Reader.class);

		if (r == null)
			return NEXT;

		try (Reader r2 = r; Writer w = res.getNegotiatedWriter()) {
			pipe(r2, w);
		}
		return FINISHED;
	}
}