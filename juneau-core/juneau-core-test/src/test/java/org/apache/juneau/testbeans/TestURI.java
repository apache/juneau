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
package org.apache.juneau.testbeans;

import java.net.*;
import java.net.URI;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;

@SuppressWarnings("javadoc")
@Bean(sort=true)
public class TestURI {
	@org.apache.juneau.annotation.URI
	@Rdf(beanUri=true)
	@Xml(format=XmlFormat.ATTR)
	public String f0 = "f0/x0";

	public URI f1, f2, f3;

	@org.apache.juneau.annotation.URI
	public String f4, f5, f6;

	public URL f7;

	public TestURIb f8;

	public String fa, fb, fc, fd, fe;

	@org.apache.juneau.annotation.URI
	public String getF9() {
		return "f9/x9";
	}

	public TestURI() throws Exception {
		f1 = new URI("f1/x1");
		f2 = new URI("/f2/x2");
		f3 = new URI("http://www.apache.org/f3/x3");
		f4 = "f4/x4";
		f5 = "/f5/x5";
		f6 = "http://www.apache.org/f6/x6";
		f7 = new URL("http://www.apache.org/f7/x7");
		f8 = new TestURIb();
		fa = "http://www.apache.org/fa/xa#MY_LABEL";
		fb = "http://www.apache.org/fb/xb?label=MY_LABEL&foo=bar";
		fc = "http://www.apache.org/fc/xc?foo=bar&label=MY_LABEL";
		fd = "http://www.apache.org/fd/xd?label2=MY_LABEL&foo=bar";
		fe = "http://www.apache.org/fe/xe?foo=bar&label2=MY_LABEL";
	}

	@org.apache.juneau.annotation.URI
	public static class TestURIb {
		@Override /* Object */
		public String toString() {
			return "f8/x8";
		}
	}
}

