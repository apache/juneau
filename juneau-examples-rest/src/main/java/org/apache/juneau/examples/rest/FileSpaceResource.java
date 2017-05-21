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
package org.apache.juneau.examples.rest;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;

import javax.servlet.http.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * Sample REST resource that shows how to use the HtmlRender class.
 */
@RestResource(
	path="/fileSpace",
	title="Available file space resource",
	description="Shows how to use HtmlRender class to customize HTML output.",
	pageLinks="{up:'$R{requestParentURI}',options:'?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/EnhancedHtmlResource.java'}"
)
public class FileSpaceResource extends Resource {
	private static final long serialVersionUID = 1L;

	private static final FileSpace[] fileSpaces = {
		new FileSpace("C:", 1000000000, 775000000),
		new FileSpace("D:", 100000000, 87500000),
		new FileSpace("E:", 100000000, 97500000),
		new FileSpace("F:", 100000000, 5000000)
	};
	
	/**
	 * Our bean class being serialized.
	 * Properties are listed to ensure order across all JVMs.
	 */
	@Bean(properties="drive,total,available,pctFull,status")
	public static class FileSpace {

		private final String drive;
		private final long total, available;
		
		public FileSpace(String drive, long total, long available) {
			this.drive = drive;
			this.total = total;
			this.available = available;
		}

		@Html(link="drive/{drive}")
		public String getDrive() {
			return drive;
		}

		public long getTotal() {
			return total;
		}

		public long getAvailable() {
			return available;
		}

		@Html(render=FileSpacePctRender.class)
		public float getPctFull() {
			return ((100 * available) / total);
		}
		
		@Html(render=FileSpaceStatusRender.class)
		public FileSpaceStatus getStatus() {
			float pf = getPctFull();
			if (pf < 80)
				return FileSpaceStatus.OK;
			if (pf < 90)
				return FileSpaceStatus.WARNING;
			return FileSpaceStatus.SEVERE;
		}
	}
	
	public static enum FileSpaceStatus {
		OK, WARNING, SEVERE;
	}
	
	public static class FileSpacePctRender extends HtmlRender<Float> {

		@Override
		public String getStyle(SerializerSession session, Float value) {
			if (value < 80)
				return "background-color:lightgreen;text-align:center";
			if (value < 90)
				return "background-color:yellow;text-align:center";
			return "background-color:red;text-align:center;border:;animation:color_change 0.5s infinite alternate";
		}

		@Override
		public Object getContent(SerializerSession session, Float value) {
			if (value >= 90) 
				return div(
					String.format("%.0f%%", value), 
					style("@keyframes color_change { from { background-color: red; } to { background-color: yellow; }")
				);
			return String.format("%.0f%%", value);
		}
	}
	
	public static class FileSpaceStatusRender extends HtmlRender<FileSpaceStatus> {

		@Override
		public String getStyle(SerializerSession session, FileSpaceStatus value) {
			return "text-align:center";
		}

		@Override
		public Object getContent(SerializerSession session, FileSpaceStatus value) {
			String resourceUri = session.getUriContext().getRootRelativeServletPath();
			switch (value) {
				case OK:  return img().src(resourceUri + "/htdocs/ok.png");
				case WARNING:  return img().src(resourceUri + "/htdocs/warning.png");
				default: return img().src(resourceUri + "/htdocs/severe.png");
			}
		}
	}

	/** GET request handler */
	@RestMethod(name="GET", path="/")
	public FileSpace[] getFileSpaceMetrics() {
		return fileSpaces;
	}

	@RestMethod(name="GET", path="drive/{drive}")
	public FileSpace getFileSpaceMetric(String drive) throws RestException {
		for (FileSpace fc : fileSpaces)
			if (fc.drive.equals(drive))
				return fc;
		throw new RestException(HttpServletResponse.SC_NOT_FOUND, "Drive not found.");
	}
}
