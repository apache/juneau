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
package org.apache.juneau.serializer.annotation;

import static org.apache.juneau.serializer.Serializer.*;
import static org.apache.juneau.serializer.OutputStreamSerializer.*;
import static org.apache.juneau.serializer.WriterSerializer.*;

import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.reflect.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Applies {@link SerializerConfig} annotations to a {@link PropertyStoreBuilder}.
 */
public class SerializerConfigApply extends ConfigApply<SerializerConfig> {

	/**
	 * Constructor.
	 *
	 * @param c The annotation class.
	 * @param r The resolver for resolving values in annotations.
	 */
	public SerializerConfigApply(Class<SerializerConfig> c, VarResolverSession r) {
		super(c, r);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void apply(AnnotationInfo<SerializerConfig> ai, PropertyStoreBuilder psb) {
		SerializerConfig a = ai.getAnnotation();
		if (! a.addBeanTypes().isEmpty())
			psb.set(SERIALIZER_addBeanTypes, bool(a.addBeanTypes()));
		if (! a.addRootType().isEmpty())
			psb.set(SERIALIZER_addRootType, bool(a.addRootType()));
		if (! a.keepNullProperties().isEmpty())
			psb.set(SERIALIZER_keepNullProperties, bool(a.keepNullProperties()));
		if (a.listener() != SerializerListener.Null.class)
			psb.set(SERIALIZER_listener, a.listener());
		if (! a.sortCollections().isEmpty())
			psb.set(SERIALIZER_sortCollections, bool(a.sortCollections()));
		if (! a.sortMaps().isEmpty())
			psb.set(SERIALIZER_sortMaps, bool(a.sortMaps()));
		if (! a.trimEmptyCollections().isEmpty())
			psb.set(SERIALIZER_trimEmptyCollections, bool(a.trimEmptyCollections()));
		if (! a.trimEmptyMaps().isEmpty())
			psb.set(SERIALIZER_trimEmptyMaps, bool(a.trimEmptyMaps()));
		if (! a.trimNullProperties().isEmpty())
			psb.set(SERIALIZER_trimNullProperties, bool(a.trimNullProperties()));
		if (! a.trimStrings().isEmpty())
			psb.set(SERIALIZER_trimStrings, bool(a.trimStrings()));
		if (! a.uriContext().isEmpty())
			psb.set(SERIALIZER_uriContext, string(a.uriContext()));
		if (! a.uriRelativity().isEmpty())
			psb.set(SERIALIZER_uriRelativity, string(a.uriRelativity()));
		if (! a.uriResolution().isEmpty())
			psb.set(SERIALIZER_uriResolution, string(a.uriResolution()));

		if (! a.binaryFormat().isEmpty())
			psb.set(OSSERIALIZER_binaryFormat, string(a.binaryFormat()));

		if (! a.fileCharset().isEmpty())
			psb.set(WSERIALIZER_fileCharset, charset(a.fileCharset()));
		if (! a.maxIndent().isEmpty())
			psb.set(WSERIALIZER_maxIndent, integer(a.maxIndent(), "maxIndent"));
		if (! a.quoteChar().isEmpty())
			psb.set(WSERIALIZER_quoteChar, character(a.quoteChar(), "quoteChar"));
		if (! a.streamCharset().isEmpty())
			psb.set(WSERIALIZER_streamCharset, charset(a.streamCharset()));
		if (! a.useWhitespace().isEmpty())
			psb.set(WSERIALIZER_useWhitespace, bool(a.useWhitespace()));
	}

	private Object charset(String in) {
		String s = string(in);
		if ("default".equalsIgnoreCase(s))
			return Charset.defaultCharset();
		return s;
	}

	private char character(String in, String loc) {
		String s = string(in);
		if (s.length() != 1)
			throw new ConfigException("Invalid syntax for character on annotation @{0}({1}): {2}", "SerializerConfig", loc, in);
		return s.charAt(0);
	}
}
