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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.annotation.*;
import org.apache.juneau.svl.*;
import org.junit.*;

/**
 * Tests the @SerializerConfig annotation.
 */
@FixMethodOrder(NAME_ASCENDING)
public class SerializerConfigAnnotationTest {

	private static void check(String expected, Object o) {
		assertEquals(expected, TO_STRING.apply(o));
	}

	private static final Function<Object,String> TO_STRING = new Function<Object,String>() {
		@Override
		public String apply(Object t) {
			if (t == null)
				return null;
			if (t instanceof AA)
				return "AA";
			return t.toString();
		}
	};

	static VarResolverSession sr = VarResolver.create().vars(XVar.class).build().createSession();

	//-----------------------------------------------------------------------------------------------------------------
	// Basic tests
	//-----------------------------------------------------------------------------------------------------------------

	public static class AA extends SerializerListener {}

	@SerializerConfig(
		addBeanTypes="$X{true}",
		addRootType="$X{true}",
		binaryFormat="$X{HEX}",
		detectRecursions="$X{true}",
		ignoreRecursions="$X{true}",
		initialDepth="$X{1}",
		listener=AA.class,
		maxDepth="$X{1}",
		maxIndent="$X{1}",
		quoteChar="$X{'}",
		sortCollections="$X{true}",
		sortMaps="$X{true}",
		trimEmptyCollections="$X{true}",
		trimEmptyMaps="$X{true}",
		keepNullProperties="$X{false}",
		trimStrings="$X{true}",
		uriContext="{}",
		uriRelativity="$X{RESOURCE}",
		uriResolution="$X{ABSOLUTE}",
		useWhitespace="$X{true}"
	)
	static class A {}
	static ClassInfo a = ClassInfo.of(A.class);

	@Test
	public void basicWriterSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("true", ((SerializerSession)x).isAddBeanTypes());
		check("true", x.isAddRootType());
		check("true", x.isDetectRecursions());
		check("true", x.isIgnoreRecursions());
		check("1", x.getInitialDepth());
		check("AA", x.getListener());
		check("1", x.getMaxDepth());
		check("1", x.getMaxIndent());
		check("'", x.getQuoteChar());
		check("true", x.isSortCollections());
		check("true", x.isSortMaps());
		check("true", x.isTrimEmptyCollections());
		check("true", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("true", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("ABSOLUTE", x.getUriResolution());
		check("true", x.isUseWhitespace());
	}

	@Test
	public void basicOutputStreamSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, a.getAnnotationList());
		MsgPackSerializerSession x = MsgPackSerializer.create().apply(al).build().getSession();
		check("true", ((SerializerSession)x).isAddBeanTypes());
		check("true", x.isAddRootType());
		check("HEX", x.getBinaryFormat());
		check("true", x.isDetectRecursions());
		check("true", x.isIgnoreRecursions());
		check("1", x.getInitialDepth());
		check("AA", x.getListener());
		check("1", x.getMaxDepth());
		check("true", x.isSortCollections());
		check("true", x.isSortMaps());
		check("true", x.isTrimEmptyCollections());
		check("true", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("true", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("ABSOLUTE", x.getUriResolution());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Annotation with no values.
	//-----------------------------------------------------------------------------------------------------------------

	@SerializerConfig()
	static class B {}
	static ClassInfo b = ClassInfo.of(B.class);

	@Test
	public void noValuesWriterSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("false", ((SerializerSession)x).isAddBeanTypes());
		check("false", x.isAddRootType());
		check(null, x.getListener());
		check("100", x.getMaxIndent());
		check("\"", x.getQuoteChar());
		check("false", x.isSortCollections());
		check("false", x.isSortMaps());
		check("false", x.isTrimEmptyCollections());
		check("false", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("false", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("NONE", x.getUriResolution());
		check("false", x.isUseWhitespace());
	}

	@Test
	public void noValuesOutputStreamSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, b.getAnnotationList());
		MsgPackSerializerSession x = MsgPackSerializer.create().apply(al).build().getSession();
		check("false", ((SerializerSession)x).isAddBeanTypes());
		check("false", x.isAddRootType());
		check("HEX", x.getBinaryFormat());
		check(null, x.getListener());
		check("false", x.isSortCollections());
		check("false", x.isSortMaps());
		check("false", x.isTrimEmptyCollections());
		check("false", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("false", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("NONE", x.getUriResolution());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// No annotation.
	//-----------------------------------------------------------------------------------------------------------------

	static class C {}
	static ClassInfo c = ClassInfo.of(C.class);

	@Test
	public void noAnnotationWriterSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		JsonSerializerSession x = JsonSerializer.create().apply(al).build().getSession();
		check("false", ((SerializerSession)x).isAddBeanTypes());
		check("false", x.isAddRootType());
		check(null, x.getListener());
		check("100", x.getMaxIndent());
		check("\"", x.getQuoteChar());
		check("false", x.isSortCollections());
		check("false", x.isSortMaps());
		check("false", x.isTrimEmptyCollections());
		check("false", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("false", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("NONE", x.getUriResolution());
		check("false", x.isUseWhitespace());
	}

	@Test
	public void noAnnotationOutputStreamSerializer() throws Exception {
		AnnotationWorkList al = AnnotationWorkList.of(sr, c.getAnnotationList());
		MsgPackSerializerSession x = MsgPackSerializer.create().apply(al).build().getSession();
		check("false", ((SerializerSession)x).isAddBeanTypes());
		check("false", x.isAddRootType());
		check("HEX", x.getBinaryFormat());
		check(null, x.getListener());
		check("false", x.isSortCollections());
		check("false", x.isSortMaps());
		check("false", x.isTrimEmptyCollections());
		check("false", x.isTrimEmptyMaps());
		check("false", x.isKeepNullProperties());
		check("false", x.isTrimStrings());
		check("{absoluteAuthority:'/',absoluteContextRoot:'/',absolutePathInfo:'/',absolutePathInfoParent:'/',absoluteServletPath:'/',absoluteServletPathParent:'/',rootRelativeContextRoot:'/',rootRelativePathInfo:'/',rootRelativePathInfoParent:'/',rootRelativeServletPath:'/',rootRelativeServletPathParent:'/'}", x.getUriContext());
		check("RESOURCE", x.getUriRelativity());
		check("NONE", x.getUriResolution());
	}
}
