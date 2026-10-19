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
package org.apache.juneau.rest.server.view.freemarker.console;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;

/** Test-only card type whose fragment differs from its body: {@code {value, max}} becomes {@code {meter:{value, max}}}. */
public final class MeterCardType implements CardTypeHandler {
	@Override public String type() { return "meter"; }
	@Override public JsonMap toFragment(CardSource source) {
		var body = source.json();
		if (! (body.get("value") instanceof Number value))
			throw source.error("type='meter' requires a numeric 'value'.");
		var max = body.get("max") instanceof Number m ? m : 100;
		return new JsonMap().append("meter", new JsonMap().append("value", value).append("max", max));
	}
}
