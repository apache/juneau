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
package org.apache.juneau.hocon;

import org.apache.juneau.*;

/**
 * Metadata on bean properties specific to HOCON serializers and parsers.
 */
public class HoconBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/** Default instance. */
	public static final HoconBeanPropertyMeta DEFAULT = new HoconBeanPropertyMeta();

	/**
	 * Constructor.
	 *
	 * @param bpm The bean property metadata.
	 * @param mp HOCON metadata provider.
	 */
	public HoconBeanPropertyMeta(BeanPropertyMeta bpm, HoconMetaProvider mp) {
		super(bpm);
	}

	private HoconBeanPropertyMeta() {
		super(null);
	}
}
