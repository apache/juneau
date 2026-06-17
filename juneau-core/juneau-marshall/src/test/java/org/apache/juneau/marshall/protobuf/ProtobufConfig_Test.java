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
package org.apache.juneau.marshall.protobuf;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link ProtobufConfig}, {@link ProtobufApply}, and builder settings (incl. hashKey).
 */
class ProtobufConfig_Test extends TestBase {

	@ProtobufConfig(addBeanTypes="true")
	public static class G01 {}

	@Test
	void g01_addBeanTypes() {
		var s = ProtobufSerializer.create().applyAnnotations(G01.class).build();
		assertNotNull(s);
	}

	@ProtobufConfig(nativeTypes="true")
	public static class G02 {}

	@Test
	void g02_nativeTypesSerializer() {
		var s = ProtobufSerializer.create().applyAnnotations(G02.class).build();
		assertNotNull(s);
	}

	@ProtobufConfig(nativeTypes="true")
	public static class G03 {}

	@Test
	void g03_parserApply() {
		var p = ProtobufParser.create().applyAnnotations(G03.class).build();
		assertNotNull(p);
	}

	@Test
	void h01_builderSettingsAffectHashKey() {
		var def = ProtobufSerializer.DEFAULT;
		var withBeanTypes = ProtobufSerializer.create().addBeanTypesProtobuf().build();
		var withNative = ProtobufSerializer.create().nativeTypes().build();
		assertNotSame(def, withBeanTypes);
		assertNotSame(def, withNative);
		assertNotSame(withBeanTypes, withNative);
	}

	@Test
	void h02_parserNativeTypesHashKey() {
		var def = ProtobufParser.DEFAULT;
		var withNative = ProtobufParser.create().nativeTypes().build();
		assertNotSame(def, withNative);
	}

	@Test
	void h03_sameSettingsShareCachedInstance() {
		var a = ProtobufSerializer.create().addBeanTypesProtobuf().build();
		var b = ProtobufSerializer.create().addBeanTypesProtobuf().build();
		assertSame(a, b);
	}

	public static class F02 { public String name; }

	@Test
	void f01_beanPropertyMetaDefault() {
		assertNotNull(ProtobufBeanPropertyMeta.DEFAULT);
	}

	@Test
	void f02_metaLookup() {
		var s = ProtobufSerializer.DEFAULT;
		var bm = MarshallingContext.DEFAULT.getBeanMeta(F02.class);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(s.getProtobufBeanPropertyMeta(bpm));
		assertNotNull(s.getProtobufBeanPropertyMeta(null));
		assertNotNull(s.getProtobufClassMeta(MarshallingContext.DEFAULT.getClassMeta(F02.class)));
	}

	@Test
	void e01_applyAnnotationCreate() {
		assertNotNull(ProtobufApplyAnnotation.DEFAULT);
		assertNotNull(ProtobufApplyAnnotation.create().build());
		assertNotNull(ProtobufAnnotation.create().fieldNumber(3).type(ProtobufScalarType.SINT32).description("x").build());
	}
}
