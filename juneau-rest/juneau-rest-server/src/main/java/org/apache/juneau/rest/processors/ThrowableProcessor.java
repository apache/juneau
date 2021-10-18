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
package org.apache.juneau.rest.processors;

import java.io.*;

import org.apache.juneau.http.header.*;
import org.apache.juneau.rest.*;

/**
 * Response handler for {@link Throwable} objects.
 *
 * <p>
 * Adds a <c>Thrown</c> header to the response and returns <c>0</c> so that the processor chain can continue.
 */
public final class ThrowableProcessor implements ResponseProcessor {

	@Override /* ResponseProcessor */
	public int process(RestOpSession opSession) throws IOException {

		RestResponse res = opSession.getResponse();
		Throwable t = res.getOutput(Throwable.class);

		if (t == null)
			return NEXT;

		res.addHeader(Thrown.of(t));

		return NEXT; // Continue processing as bean.
	}
}

