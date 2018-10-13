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
import org.apache.juneau.utils.*;
import org.apache.juneau.utils.ZipFileList.*;

/**
 * @deprecated No replacement.
 */
@Deprecated
public class ZipFileListResponseHandler implements ResponseHandler {

	@Override /* ResponseHandler */
	public boolean handle(RestRequest req, RestResponse res) throws IOException, RestException {
		Object output = res.getOutput();
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

	@SuppressWarnings("javadoc")
	public boolean handle(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		return handle(req, res);
	}
}
