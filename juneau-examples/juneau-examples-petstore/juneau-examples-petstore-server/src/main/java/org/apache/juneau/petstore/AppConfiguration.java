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
package org.apache.juneau.petstore;

import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.rest.springboot.annotation.JuneauRestRoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Configuration
public class AppConfiguration {

    public static String DEFAULT_JDBC_URL = "jdbc:h2:mem:testdb;MODE=PostgreSQL";
    public static String DEFAULT_JDBC_USERNAME = "sa";
    public static String DEFAULT_JDBC_PASSWORD = "";

    @Autowired
    private static volatile ApplicationContext appContext;

    public static ApplicationContext getAppContext() {
        return appContext;
    }

    public static void setAppContext(ApplicationContext appContext) {
        AppConfiguration.appContext = appContext;
    }

    //-----------------------------------------------------------------------------------------------------------------
    // Services
    //-----------------------------------------------------------------------------------------------------------------

    //-----------------------------------------------------------------------------------------------------------------
    // REST
    //-----------------------------------------------------------------------------------------------------------------

    @Bean @JuneauRestRoot
    public RootResources rootResources() {
        return new RootResources();
    }

    @Bean
    public PetStoreResource petStoreResource() {
        return new PetStoreResource();
    }
}
