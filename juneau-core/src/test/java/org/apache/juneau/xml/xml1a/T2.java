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
package org.apache.juneau.xml.xml1a;

import org.apache.juneau.annotation.*;
import org.apache.juneau.xml.annotation.*;

@Xml(prefix="foo",namespace="http://foo",name="T2")
@Bean(sort=true)
@SuppressWarnings("javadoc")
public class T2 {

	public int f1 = 1;

	@Xml(prefix="bar",namespace="http://bar") public int f2 = 2;

	private int f3 = 3;
	public int getF3() { return f3;}
	public void setF3(int f3) { this.f3 = f3;}

	private int f4 = 4;
	@Xml(prefix="baz",namespace="http://baz") public int getF4() { return f4; }
	public void setF4(int f4) { this.f4 = f4;}

	public boolean equals(T2 x) {
		return x.f1 == f1 && x.f2 == f2 && x.f3 == f3 && x.f4 == f4;
	}
}