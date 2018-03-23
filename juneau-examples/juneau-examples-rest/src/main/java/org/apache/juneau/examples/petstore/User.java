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
package org.apache.juneau.examples.petstore;

import org.apache.juneau.annotation.*;

@Bean(typeName="User", fluentSetters=true)
public class User {
	private long id;
	private String username, firstName, lastName, email, password, phone;
	private int userStatus;
	
	public long getId() {
		return id;
	}

	public User id(long id) {
		this.id = id;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public User username(String username) {
		this.username = username;
		return this;
	}

	public String getFirstName() {
		return firstName;
	}

	public User firstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	public String getLastName() {
		return lastName;
	}

	public User lastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public User email(String email) {
		this.email = email;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public User password(String password) {
		this.password = password;
		return this;
	}

	public String getPhone() {
		return phone;
	}

	public User phone(String phone) {
		this.phone = phone;
		return this;
	}

	public int getUserStatus() {
		return userStatus;
	}

	public User userStatus(int userStatus) {
		this.userStatus = userStatus;
		return this;
	}
}
