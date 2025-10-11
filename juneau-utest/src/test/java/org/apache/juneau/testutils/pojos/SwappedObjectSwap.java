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
package org.apache.juneau.testutils.pojos;

import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.utest.utils.Constants.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.swap.*;

public class SwappedObjectSwap extends ObjectSwap<SwappedObject,String> {
	@Override
	public String swap(BeanSession session, SwappedObject c) throws SerializeException {
		return SWAP;
	}

	@Override
	public SwappedObject unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
		var c = new SwappedObject();
		if (eq(f, SWAP))
			c.wasUnswapped = true;
		return c;
	}
}