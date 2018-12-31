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
package org.apache.juneau.examples.rest.petstore.dto;

import static javax.persistence.EnumType.*;

import javax.persistence.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.jsonschema.annotation.*;

/**
 * User bean.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Bean(typeName="User", fluentSetters=true, properties="username,firstName,lastName,email,password,phone,userStatus")
@Entity(name="PetstoreUser")
public class User {

	@Id
	@Column(length=8)
	@Schema(description="Username.", minLength=3, maxLength=8)
	@Html(link="servlet:/user/{username}")
	private String username;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50)
	private String firstName;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50)
	private String lastName;

	@Column(length=50)
	@Schema(description="First name.", maxLength=50, pattern="\\S+\\@\\S+")
	private String email;

	@Column(length=8)
	@Schema(description="Password.", minLength=3, maxLength=8, pattern="[\\w\\d]{3,8}")
	private String password;

	@Column
	@Schema(description="Phone number.", minLength=12, maxLength=12, pattern="\\d{3}\\-\\d{3}\\-\\d{4}")
	private String phone;

	@Column
	@Enumerated(STRING)
	private UserStatus userStatus;

	public User apply(User c) {
		this.username = c.getUsername();
		this.firstName = c.getFirstName();
		this.lastName = c.getLastName();
		this.email = c.getEmail();
		this.password = c.getPassword();
		this.phone = c.getPhone();
		this.userStatus = c.getUserStatus();
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean properties
	//-----------------------------------------------------------------------------------------------------------------

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

	public UserStatus getUserStatus() {
		return userStatus;
	}

	public User userStatus(UserStatus userStatus) {
		this.userStatus = userStatus;
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * This shows an example generated from a static method.
	 */
	@Example
	public static User EXAMPLE = new User()
		.username("billy")
		.firstName("Billy")
		.lastName("Bob")
		.email("billy@apache.org")
		.userStatus(UserStatus.ACTIVE)
		.phone("111-222-3333");

}
