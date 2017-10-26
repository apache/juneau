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
package org.apache.juneau.rest.response;

import java.io.*;
import java.util.zip.*;

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.utils.ZipFileList.*;

/**
 * Response handler for ZipFileList objects.
 *
 * <p>
 * Can be associated with a REST resource using the {@link RestResource#responseHandlers} annotation.
 *
 * <p>
 * Sets the following headers:
 * <ul class='spaced-list'>
 * 	<li>
 * 		<code>Content-Type</code> - <code>application/zip</code>
 * 	<li>
 * 		<code>Content-Disposition=attachment;filename=X</code> - Sets X to the file name passed in through the
 * 		constructor {@link ZipFileList#ZipFileList(String)}.
 * </ul>
 */
public class ZipFileListResponseHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		if (output.getClass() == ZipFileList.class) {
			ZipFileList m = (ZipFileList)output;
			res.setContentType("application/zip");
			res.setHeader("Content-Disposition", "attachment;filename=" + m.fileName); //$NON-NLS-2$
			try (OutputStream os = res.getOutputStream()) {
				try (ZipOutputStream zos = new ZipOutputStream(os)) {
					for (ZipFileEntry e : m)
						e.write(zos);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}
}
