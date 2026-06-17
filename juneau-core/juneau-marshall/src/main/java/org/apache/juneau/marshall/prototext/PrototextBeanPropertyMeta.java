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
package org.apache.juneau.marshall.prototext;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.apache.juneau.commons.utils.StringUtils.emptyIfNull;
import static org.apache.juneau.commons.utils.Utils.*;

import org.apache.juneau.commons.bean.*;
import org.apache.juneau.marshall.*;

/**
 * Metadata on bean properties specific to the Prototext serializers and parsers pulled from the {@link Prototext @Prototext}
 * annotation on the bean property.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/ProtobufBasics">Protobuf Text Format Basics</a>
 * </ul>
 */
public class PrototextBeanPropertyMeta extends ExtendedBeanPropertyMeta {

	/**
	 * Default instance.
	 */
	public static final PrototextBeanPropertyMeta DEFAULT = new PrototextBeanPropertyMeta();

	private final String comment;

	/**
	 * Constructor.
	 *
	 * @param bpm The metadata of the bean property of this additional metadata.
	 * @param mp Prototext metadata provider (for finding information about other artifacts).
	 */
	public PrototextBeanPropertyMeta(BeanPropertyMeta bpm, PrototextMetaProvider mp) {
		super(bpm);
		var first = getBeanPropertyMeta().getAnnotations(Prototext.class).findFirst();
		var c = first.map(ai -> nullIfEmpty(trim(ai.inner().comment()))).orElse("");
		this.comment = emptyIfNull(c);
	}

	private PrototextBeanPropertyMeta() {
		super(null);
		this.comment = "";
	}

	/**
	 * Returns the comment to emit before this field (from {@link Prototext#comment()}).
	 *
	 * @return The comment, or empty string if none.
	 */
	public String getComment() {
		return comment;
	}
}
