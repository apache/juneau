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
package org.apache.juneau.testutils.pojos;

import static org.apache.juneau.testutils.pojos.Constants.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;

public class SwappedPojoSwap extends PojoSwap<SwappedPojo,String> {
	@Override
	public String swap(BeanSession session, SwappedPojo c) throws SerializeException {
		return SWAP;
	}

	@Override
	public SwappedPojo unswap(BeanSession session, String f, ClassMeta<?> hint) throws ParseException {
		SwappedPojo c = new SwappedPojo();
		if (f.equals(SWAP))
			c.wasUnswapped = true;
		return c;
	}
}