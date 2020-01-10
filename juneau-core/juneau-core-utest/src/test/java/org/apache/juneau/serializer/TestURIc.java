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
package org.apache.juneau.serializer;

import java.net.URI;

import org.apache.juneau.annotation.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;

@Bean(sort=true)
@BeanConfig(
	applyURI={
		@org.apache.juneau.annotation.URI(on="TestURIc.f0,TestURIc.f2a,TestURIc.f2b,TestURIc.f2c,TestURIc.f2d,TestURIc.f2e,TestURIc.f2f,TestURIc.f2g,TestURIc.f2h,TestURIc.f2i,TestURIc.f2j,TestURIc.f2k,TestURIc.f2l,TestURIc.f2m,TestURIc.f2n,TestURIc.f2o,TestURIc.f3a,,TestURIc.f3b,TestURIc.f3c,TestURIc.getF5,TestURIbc")
	}
)
public class TestURIc {

	// String annotated as a URI
	@Rdf(beanUri=true)
	@Xml(format=XmlFormat.ATTR)
	public String f0 = "f0/x0";

	// URI properties
	public URI
		f1a = URI.create("http://www.apache.org/f1a"),
		f1b = URI.create("/f1b"),
		f1c = URI.create("/f1c/x/y"),
		f1d = URI.create("f1d"),
		f1e = URI.create("f1e/x/y"),
		f1f = URI.create(""),
		f1g = URI.create("servlet:/f1g/x"),
		f1h = URI.create("servlet:/f1h"),
		f1i = URI.create("servlet:/"),
		f1j = URI.create("servlet:/.."),
		f1k = URI.create("context:/f1j/x"),
		f1l = URI.create("context:/f1k"),
		f1m = URI.create("context:/"),
		f1n = URI.create("context:/.."),
		fio = null;

	// Strings annotated with @URI properties
	public String
		f2a = "http://www.apache.org/f2a",
		f2b = "/f2b",
		f2c = "/f2c/x/y",
		f2d = "f2d",
		f2e = "f2e/x/y",
		f2f = "",
		f2g = "servlet:/f2g/x",
		f2h = "servlet:/f2h",
		f2i = "servlet:/",
		f2j = "servlet:/..",
		f2k = "context:/f2j/x",
		f2l = "context:/f2k",
		f2m = "context:/",
		f2n = "context:/..",
		f2o = null;

	// Strings with labels
	public String
		f3a = "http://www.apache.org/f3a/x?label=MY_LABEL&foo=bar",
		f3b = StringUtils.urlEncode("<>&'\""),
		f3c = "<>&'\"";  // Invalid URI, but should produce parsable output.

	// @URI on bean
	public TestURIbc f4 = new TestURIbc();

	// @URI on bean property method.
	public String getF5() {
		return "f5/x";
	}

	public static class TestURIbc {
		@Override /* Object */
		public String toString() {
			return "test/uri/b";
		}
	}
}