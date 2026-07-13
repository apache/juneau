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
package org.apache.juneau.petstore.dto;

/**
 * User domain bean.
 *
 * <p>
 * Represents a registered petstore user.  The {@code username} is the primary key.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class User {

	private String username;
	private String firstName;
	private String lastName;
	private String email;
	private String password;
	private String phone;
	private UserStatus userStatus;

	/**
	 * Returns the username (primary key).
	 *
	 * @return The username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public User setUsername(String value) {
		username = value;
		return this;
	}

	/**
	 * Returns the first name.
	 *
	 * @return The first name.  Can be <jk>null</jk>.
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Sets the first name.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public User setFirstName(String value) {
		firstName = value;
		return this;
	}

	/**
	 * Returns the last name.
	 *
	 * @return The last name.  Can be <jk>null</jk>.
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Sets the last name.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public User setLastName(String value) {
		lastName = value;
		return this;
	}

	/**
	 * Returns the email address.
	 *
	 * @return The email address.  Can be <jk>null</jk>.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email address.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public User setEmail(String value) {
		email = value;
		return this;
	}

	/**
	 * Returns the password.
	 *
	 * <p>
	 * Stored in plaintext for sample purposes only.  Do not use this pattern in production.
	 *
	 * @return The password.  Can be <jk>null</jk>.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public User setPassword(String value) {
		password = value;
		return this;
	}

	/**
	 * Returns the phone number.
	 *
	 * @return The phone number.  Can be <jk>null</jk>.
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * Sets the phone number.
	 *
	 * @param value The new value.  Can be <jk>null</jk>.
	 * @return This object.
	 */
	public User setPhone(String value) {
		phone = value;
		return this;
	}

	/**
	 * Returns the user account status.
	 *
	 * @return The user account status.
	 */
	public UserStatus getUserStatus() {
		return userStatus;
	}

	/**
	 * Sets the user account status.
	 *
	 * @param value The new value.
	 * @return This object.
	 */
	public User setUserStatus(UserStatus value) {
		userStatus = value;
		return this;
	}
}
