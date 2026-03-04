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
package org.apache.juneau.ini;

import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.annotation.*;

/**
 * Metadata on classes specific to INI serializers and parsers, from {@link Ini @Ini}.
 */
public class IniClassMeta extends ExtendedClassMeta {

	private final String section;

	/**
	 * Constructor.
	 *
	 * @param cm The class metadata.
	 * @param mp INI metadata provider.
	 */
	public IniClassMeta(ClassMeta<?> cm, IniMetaProvider mp) {
		super(cm);
		var ref = new AtomicReference<Ini>();
		cm.forEachAnnotation(Ini.class, null, ref::set);
		var a = ref.get();
		section = a != null && !a.section().isEmpty() ? a.section() : "";
	}

	/**
	 * Returns the custom section name for this class.
	 *
	 * @return The section name, or empty string if not specified.
	 */
	public String getSection() {
		return section;
	}
}
