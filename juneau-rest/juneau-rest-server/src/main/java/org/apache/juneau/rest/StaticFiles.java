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
package org.apache.juneau.rest;

import java.io.*;
import java.util.*;

import javax.activation.*;

import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;

/**
 * The static file resource resolver for a single {@link StaticFileMapping}.
 */
class StaticFiles {
	private final String path, location;
	private final Map<String,Object> responseHeaders;

	private final ResourceManager staticResourceManager;
	private final MimetypesFileTypeMap mimetypesFileTypeMap;

	StaticFiles(StaticFileMapping sfm, ResourceManager staticResourceManager, MimetypesFileTypeMap mimetypesFileTypeMap, Map<String,Object> staticFileResponseHeaders) {
		this.path = sfm.path;
		this.location = sfm.location;
		this.responseHeaders = sfm.responseHeaders != null ? sfm.responseHeaders : staticFileResponseHeaders;
		this.staticResourceManager = staticResourceManager;
		this.mimetypesFileTypeMap = mimetypesFileTypeMap;
	}

	String getPath() {
		return path;
	}

	StaticFile resolve(String p) throws IOException {
		if (p.startsWith(path)) {
			String remainder = (p.equals(path) ? "" : p.substring(path.length()));
			if (remainder.isEmpty() || remainder.startsWith("/")) {
				String p2 = location + remainder;
				InputStream is = staticResourceManager.getStream(p2, null);
				try {
					if (is != null) {
						int i = p2.lastIndexOf('/');
						String name = (i == -1 ? p2 : p2.substring(i+1));
						String mediaType = mimetypesFileTypeMap.getContentType(name);
						return new StaticFile(IOUtils.readBytes(is), mediaType, responseHeaders);
					}
				}
			}
		}
		return null;
	}
}
