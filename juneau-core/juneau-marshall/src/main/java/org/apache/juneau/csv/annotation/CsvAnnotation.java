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
package org.apache.juneau.csv.annotation;

import java.lang.annotation.*;

import org.apache.juneau.*;

/**
 * A concrete implementation of the {@link Csv} annotation.
 *
 * <ul class='seealso'>
 * 	<li class='jm'>{@link BeanContextBuilder#annotations(Annotation...)}
 * </ul>
 */
public class CsvAnnotation implements Csv {

	private String on = "";

	/**
	 * Constructor.
	 *
	 * @param on The initial value for the <c>on</c> property.
	 * 	<br>See {@link Csv#on()}
	 */
	public CsvAnnotation(String on) {
		this.on = on;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Csv.class;
	}

	@Override
	public String on() {
		return on;
	}

	/**
	 * Sets the <c>on</c> property on this annotation.
	 *
	 * @param value The new value for this property.
	 * @return This object (for method chaining).
	 */
	public CsvAnnotation on(String value) {
		this.on = value;
		return this;
	}
}
