/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

import type {SidebarsConfig} from '@docusaurus/plugin-content-docs';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */
const sidebars: SidebarsConfig = {
  // Juneau documentation sidebar
  tutorialSidebar: [
    // 'overview',  // Temporarily commented to avoid conflicts
    // 'syntax-highlighting-demo',
    // 'class-hierarchy-demo', 
    // 'class-hierarchy-markdown',
    {
      type: 'category',
      label: '1. Juneau',
      collapsed: false,
      items: [
        {
          type: 'doc',
          id: 'topics/01.01.Overview',
          label: '1.1. Juneau Overview',
        },
        {
          type: 'doc',
          id: 'topics/01.02.Marshalling',
          label: '1.2. Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/01.03.EndToEndRest',
          label: '1.3. End-to-End REST',
        },
        {
          type: 'doc',
          id: 'topics/01.04.RestServer',
          label: '1.4. REST Server',
        },
        {
          type: 'doc',
          id: 'topics/01.05.RestClient',
          label: '1.5. REST Client',
        },
        {
          type: 'doc',
          id: 'topics/01.06.Dtos',
          label: '1.6. DTOs',
        },
        {
          type: 'doc',
          id: 'topics/01.07.ConfigFiles',
          label: '1.7. Config Files',
        },
        {
          type: 'doc',
          id: 'topics/01.08.FluentAssertions',
          label: '1.8. Fluent Assertions',
        },
        {
          type: 'doc',
          id: 'topics/01.09.GeneralDesign',
          label: '1.9. General Design',
        },
      ],
    },
    {
      type: 'category',
      label: '2. Marshall',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/02.01.Module-juneau-marshall',
          label: '2.1. Module: juneau-marshall',
        },
        {
          type: 'doc',
          id: 'topics/02.02.Marshallers',
          label: '2.2. Marshallers',
        },
        {
          type: 'doc',
          id: 'topics/02.03.SerializersAndParsers',
          label: '2.3. Serializers and Parsers',
        },
        {
          type: 'category',
          label: '2.4. Bean Contexts',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.04.BeanContexts',
              label: '2.4. Bean Contexts',
            },
            {
              type: 'doc',
              id: 'topics/02.04.01.BeanAnnotation',
              label: '2.4.1. @Bean Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.02.BeanpAnnotation',
              label: '2.4.2. @Beanp Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.03.BeancAnnotation',
              label: '2.4.3. @Beanc Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.04.BeanIgnoreAnnotation',
              label: '2.4.4. @BeanIgnore Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.05.NamePropertyAnnotation',
              label: '2.4.5. @NameProperty Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.06.ParentPropertyAnnotation',
              label: '2.4.6. @ParentProperty Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.07.PojoBuilders',
              label: '2.4.7. POJO Builders',
            },
            {
              type: 'doc',
              id: 'topics/02.04.08.BypassSerialization',
              label: '2.4.8. Bypass Serialization',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.05.JavaBeansSupport',
          label: '2.5. Java Beans Support',
        },
        {
          type: 'doc',
          id: 'topics/02.06.HttpPartSerializersParsers',
          label: '2.6. HTTP Part Serializers/Parsers',
        },
        {
          type: 'doc',
          id: 'topics/02.07.ContextSettings',
          label: '2.7. Context Settings',
        },
        {
          type: 'doc',
          id: 'topics/02.08.ContextAnnotations',
          label: '2.8. Context Annotations',
        },
        {
          type: 'doc',
          id: 'topics/02.09.JsonMap',
          label: '2.9. JsonMap',
        },
        {
          type: 'doc',
          id: 'topics/02.10.ComplexDataTypes',
          label: '2.10. Complex Data Types',
        },
        {
          type: 'category',
          label: '2.11. Swaps',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.11.SerializerSetsParserSets',
              label: '2.11. Serializer/Parser Sets',
            },
            {
              type: 'doc',
              id: 'topics/02.11.01.DefaultSwaps',
              label: '2.11.1. Default Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.02.AutoSwaps',
              label: '2.11.2. Auto Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.03.PerMediaTypeSwaps',
              label: '2.11.3. Per-Media-Type Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.04.OneWaySwaps',
              label: '2.11.4. One-Way Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.05.SwapAnnotation',
              label: '2.11.5. @Swap Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.11.06.TemplatedSwaps',
              label: '2.11.6. Templated Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.07.SurrogateClasses',
              label: '2.11.7. Surrogate Classes',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.12.Swaps',
          label: '2.12. Swaps',
        },
        {
          type: 'category',
          label: '2.13. Dynamic Annotations',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.13.DynamicallyAppliedAnnotations',
              label: '2.13. Dynamically Applied Annotations',
            },
            {
              type: 'doc',
              id: 'topics/02.13.01.BeanSubTypes',
              label: '2.13.1. Bean Sub Types',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.14.BeanDictionaries',
          label: '2.14. Bean Dictionaries',
        },
        {
          type: 'doc',
          id: 'topics/02.15.VirtualBeans',
          label: '2.15. Virtual Beans',
        },
        {
          type: 'doc',
          id: 'topics/02.16.Recursion',
          label: '2.16. Recursion',
        },
        {
          type: 'doc',
          id: 'topics/02.17.ParsingIntoGenericModels',
          label: '2.17. Parsing into Generic Models',
        },
        {
          type: 'doc',
          id: 'topics/02.18.ReadingContinuousStreams',
          label: '2.18. Reading Continuous Streams',
        },
        {
          type: 'doc',
          id: 'topics/02.19.MarshallingUris',
          label: '2.19. Marshalling URIs',
        },
        {
          type: 'doc',
          id: 'topics/02.20.JacksonComparison',
          label: '2.20. Jackson Comparison',
        },
        {
          type: 'category',
          label: '2.21. Simple Variable Language',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.21.PojoCategories',
              label: '2.21. POJO Categories',
            },
            {
              type: 'doc',
              id: 'topics/02.21.01.SvlVariables',
              label: '2.21.1. SVL Variables',
            },
            {
              type: 'doc',
              id: 'topics/02.21.02.VarResolvers',
              label: '2.21.2. Var Resolvers',
            },
            {
              type: 'doc',
              id: 'topics/02.21.03.DefaultVarResolver',
              label: '2.21.3. Default Var Resolver',
            },
            {
              type: 'doc',
              id: 'topics/02.21.04.SvlOtherNotes',
              label: '2.21.4. SVL Other Notes',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.22.SimpleVariableLanguage',
          label: '2.22. Simple Variable Language',
        },
        {
          type: 'doc',
          id: 'topics/02.23.Encoders',
          label: '2.23. Encoders',
        },
        {
          type: 'category',
          label: '2.24. JSON Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.24.ObjectTools',
              label: '2.24. Object Tools',
            },
            {
              type: 'doc',
              id: 'topics/02.24.01.JsonMethodology',
              label: '2.24.1. JSON Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.24.02.JsonSerializers',
              label: '2.24.2. JSON Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.24.03.Json5',
              label: '2.24.3. JSON5',
            },
            {
              type: 'doc',
              id: 'topics/02.24.04.JsonParsers',
              label: '2.24.4. JSON Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.24.05.JsonAnnotation',
              label: '2.24.5. @Json Annotation',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.25.JsonDetails',
          label: '2.25. JSON Details',
        },
        {
          type: 'category',
          label: '2.26. XML Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.26.JsonSchemaDetails',
              label: '2.26. JSON Schema Details',
            },
            {
              type: 'doc',
              id: 'topics/02.26.01.XmlMethodology',
              label: '2.26.1. XML Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.26.02.XmlSerializers',
              label: '2.26.2. XML Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.26.03.XmlParsers',
              label: '2.26.3. XML Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.26.04.XmlBeanTypeNameAnnotation',
              label: '2.26.4. @XmlBeanTypeName Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.05.XmlChildNameAnnotation',
              label: '2.26.5. @XmlChildName Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.06.XmlFormatAnnotation',
              label: '2.26.6. @XmlFormat Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.07.XmlNamespaces',
              label: '2.26.7. XML Namespaces',
            },
          ],
        },
        {
          type: 'category',
          label: '2.27. HTML Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.27.XmlDetails',
              label: '2.27. XML Details',
            },
            {
              type: 'doc',
              id: 'topics/02.27.01.HtmlMethodology',
              label: '2.27.1. HTML Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.27.02.HtmlSerializers',
              label: '2.27.2. HTML Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.27.03.HtmlParsers',
              label: '2.27.3. HTML Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.27.04.HtmlAnnotation',
              label: '2.27.4. @Html Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.27.05.HtmlRenderAnnotation',
              label: '2.27.5. @HtmlRender Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.27.06.HtmlDocSerializer',
              label: '2.27.6. HTML Doc Serializer',
            },
            {
              type: 'doc',
              id: 'topics/02.27.07.BasicHtmlDocTemplate',
              label: '2.27.7. Basic HTML Doc Template',
            },
            {
              type: 'doc',
              id: 'topics/02.27.08.HtmlCustomTemplates',
              label: '2.27.8. HTML Custom Templates',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.28.HtmlDetails',
          label: '2.28. HTML Details',
        },
        {
          type: 'category',
          label: '2.29. UON Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.29.HtmlSchema',
              label: '2.29. HTML Schema',
            },
            {
              type: 'doc',
              id: 'topics/02.29.01.UonMethodology',
              label: '2.29.1. UON Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.29.02.UonSerializers',
              label: '2.29.2. UON Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.29.03.UonParsers',
              label: '2.29.3. UON Parsers',
            },
          ],
        },
        {
          type: 'category',
          label: '2.30. URL-Encoding Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.30.UonDetails',
              label: '2.30. UON Details',
            },
            {
              type: 'doc',
              id: 'topics/02.30.01.UrlEncMethodology',
              label: '2.30.1. URL-Encoding Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.30.02.UrlEncSerializers',
              label: '2.30.2. URL-Encoding Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.30.03.UrlEncParsers',
              label: '2.30.3. URL-Encoding Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.30.04.UrlEncodingAnnotation',
              label: '2.30.4. @UrlEncoding Annotation',
            },
          ],
        },
        {
          type: 'category',
          label: '2.31. MessagePack Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.31.UrlEncodingDetails',
              label: '2.31. URL-Encoding Details',
            },
            {
              type: 'doc',
              id: 'topics/02.31.01.MsgPackSerializers',
              label: '2.31.1. MessagePack Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.31.02.MsgPackParsers',
              label: '2.31.2. MessagePack Parsers',
            },
          ],
        },
        {
          type: 'category',
          label: '2.32. OpenAPI Details',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.32.MsgPackDetails',
              label: '2.32. MessagePack Details',
            },
            {
              type: 'doc',
              id: 'topics/02.32.01.OpenApiMethodology',
              label: '2.32.1. OpenAPI Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.32.02.OpenApiSerializers',
              label: '2.32.2. OpenAPI Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.32.03.OpenApiParsers',
              label: '2.32.3. OpenAPI Parsers',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.33.OpenApiDetails',
          label: '2.33. OpenAPI Details',
        },
        {
          type: 'doc',
          id: 'topics/02.34.BestPractices',
          label: '2.34. Best Practices',
        },
      ],
    },
    {
      type: 'doc',
      id: 'topics/03.01.Module-juneau-marshall-rdf',
      label: '3. Module: juneau-marshall-rdf',
    },
    {
      type: 'category',
      label: '4. DTOs',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/04.01.Module-juneau-dto',
          label: '4.1. Module: juneau-dto',
        },
        {
          type: 'doc',
          id: 'topics/04.02.Html5',
          label: '4.2. HTML5',
        },
        {
          type: 'doc',
          id: 'topics/04.03.Atom',
          label: '4.3. Atom',
        },
        {
          type: 'doc',
          id: 'topics/04.04.Swagger',
          label: '4.4. Swagger',
        },
        {
          type: 'doc',
          id: 'topics/04.05.SwaggerUi',
          label: '4.5. Swagger UI',
        },
      ],
    },
    {
      type: 'category',
      label: '5. Config',
      collapsed: true,
      items: [
        {
          type: 'category',
          label: '5.1. Juneau Config',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.01.Module-juneau-config',
              label: '5.1. Module: juneau-config',
            },
            {
              type: 'doc',
              id: 'topics/05.01.01.SyntaxRules',
              label: '5.1.1. Syntax Rules',
            },
          ],
        },
        {
          type: 'category',
          label: '5.2. Overview',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.02.Overview',
              label: '5.2. Overview',
            },
            {
              type: 'doc',
              id: 'topics/05.02.01.Pojos',
              label: '5.2.1. POJOs',
            },
            {
              type: 'doc',
              id: 'topics/05.02.02.Arrays',
              label: '5.2.2. Arrays',
            },
            {
              type: 'doc',
              id: 'topics/05.02.03.JCFObjects',
              label: '5.2.3. JCF Objects',
            },
            {
              type: 'doc',
              id: 'topics/05.02.04.BinaryData',
              label: '5.2.4. Binary Data',
            },
          ],
        },
        {
          type: 'category',
          label: '5.3. Reading Entries',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.03.ReadingEntries',
              label: '5.3. Reading Entries',
            },
            {
              type: 'doc',
              id: 'topics/05.03.01.LogicVariables',
              label: '5.3.1. Logic Variables',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.04.Variables',
          label: '5.4. Variables',
        },
        {
          type: 'doc',
          id: 'topics/05.05.ModdedEntries',
          label: '5.5. Modified Entries',
        },
        {
          type: 'category',
          label: '5.6. Sections',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.06.Sections',
              label: '5.6. Sections',
            },
            {
              type: 'doc',
              id: 'topics/05.06.01.FileSystemChanges',
              label: '5.6.1. File System Changes',
            },
            {
              type: 'doc',
              id: 'topics/05.06.02.CustomEntrySerialization',
              label: '5.6.2. Custom Entry Serialization',
            },
            {
              type: 'doc',
              id: 'topics/05.06.03.BulkSettingValues',
              label: '5.6.3. Bulk Setting Values',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.07.SettingValues',
          label: '5.7. Setting Values',
        },
        {
          type: 'doc',
          id: 'topics/05.08.Listeners',
          label: '5.8. Listeners',
        },
        {
          type: 'doc',
          id: 'topics/05.09.SerializingConfigs',
          label: '5.9. Serializing Configs',
        },
        {
          type: 'category',
          label: '5.10. Config Imports',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.10.ConfigImports',
              label: '5.10. Config Imports',
            },
            {
              type: 'doc',
              id: 'topics/05.10.01.MemoryStore',
              label: '5.10.1. Memory Store',
            },
            {
              type: 'doc',
              id: 'topics/05.10.02.FileStore',
              label: '5.10.2. File Store',
            },
            {
              type: 'doc',
              id: 'topics/05.10.03.CustomStores',
              label: '5.10.3. Custom Stores',
            },
            {
              type: 'doc',
              id: 'topics/05.10.04.StoreListeners',
              label: '5.10.4. Store Listeners',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.11.ConfigStores',
          label: '5.11. Config Stores',
        },
        {
          type: 'doc',
          id: 'topics/05.12.ReadOnlyConfigs',
          label: '5.12. Read-Only Configs',
        },
        {
          type: 'doc',
          id: 'topics/05.13.ClosingConfigs',
          label: '5.13. Closing Configs',
        },
        {
          type: 'doc',
          id: 'topics/05.14.SystemDefaultConfig',
          label: '5.14. System Default Config',
        },
      ],
    },
    {
      type: 'category',
      label: '6. Assertions',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/06.01.Module-juneau-assertions',
          label: '6.1. Module: juneau-assertions',
        },
        {
          type: 'doc',
          id: 'topics/06.02.Overview',
          label: '6.2. Overview',
        },
      ],
    },
    {
      type: 'category',
      label: '7. REST Common',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/07.01.Module-juneau-rest-common',
          label: '7.1. Module: juneau-rest-common',
        },
        {
          type: 'doc',
          id: 'topics/07.02.HelperClasses',
          label: '7.2. Helper Classes',
        },
        {
          type: 'doc',
          id: 'topics/07.03.Annotations',
          label: '7.3. Annotations',
        },
        {
          type: 'doc',
          id: 'topics/07.04.HttpHeaders',
          label: '7.4. HTTP Headers',
        },
        {
          type: 'doc',
          id: 'topics/07.05.HttpParts',
          label: '7.5. HTTP Parts',
        },
        {
          type: 'doc',
          id: 'topics/07.06.HttpEntitiesAndResources',
          label: '7.6. HTTP Entities and Resources',
        },
        {
          type: 'doc',
          id: 'topics/07.07.HttpResponses',
          label: '7.7. HTTP Responses',
        },
        {
          type: 'doc',
          id: 'topics/07.08.RemoteProxyInterfaces',
          label: '7.8. Remote Proxy Interfaces',
        },
      ],
    },
    {
      type: 'category',
      label: '8. REST Server',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/08.01.Module-juneau-rest-server',
          label: '8.1. Module: juneau-rest-server',
        },
        {
          type: 'category',
          label: '8.2. Overview',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/08.02.Overview',
              label: '8.2. Overview',
            },
            {
              type: 'doc',
              id: 'topics/08.02.01.PredefinedClasses',
              label: '8.2.1. Predefined Classes',
            },
            {
              type: 'doc',
              id: 'topics/08.02.02.ChildResources',
              label: '8.2.2. Child Resources',
            },
            {
              type: 'doc',
              id: 'topics/08.02.03.PathVariables',
              label: '8.2.3. Path Variables',
            },
            {
              type: 'doc',
              id: 'topics/08.02.04.Deployment',
              label: '8.2.4. Deployment',
            },
            {
              type: 'doc',
              id: 'topics/08.02.05.LifecycleHooks',
              label: '8.2.5. Lifecycle Hooks',
            },
          ],
        },
        {
          type: 'category',
          label: '8.3. Annotated Classes',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/08.03.AnnotatedClasses',
              label: '8.3. Annotated Classes',
            },
            {
              type: 'doc',
              id: 'topics/08.03.01.InferredHttpMethodsAndPaths',
              label: '8.3.1. Inferred HTTP Methods and Paths',
            },
            {
              type: 'doc',
              id: 'topics/08.03.02.JavaMethodParameters',
              label: '8.3.2. Java Method Parameters',
            },
            {
              type: 'doc',
              id: 'topics/08.03.03.JavaMethodReturnTypes',
              label: '8.3.3. Java Method Return Types',
            },
            {
              type: 'doc',
              id: 'topics/08.03.04.JavaMethodThrowableTypes',
              label: '8.3.4. Java Method Throwable Types',
            },
            {
              type: 'doc',
              id: 'topics/08.03.05.PathPatterns',
              label: '8.3.5. Path Patterns',
            },
            {
              type: 'doc',
              id: 'topics/08.03.06.Matchers',
              label: '8.3.6. Matchers',
            },
            {
              type: 'doc',
              id: 'topics/08.03.07.OverloadingHttpMethods',
              label: '8.3.7. Overloading HTTP Methods',
            },
            {
              type: 'doc',
              id: 'topics/08.03.08.AdditionalInformation',
              label: '8.3.8. Additional Information',
            },
          ],
        },
        {
          type: 'category',
          label: '8.4. RestOp Annotated Methods',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/08.04.RestOpAnnotatedMethods',
              label: '8.4. RestOp Annotated Methods',
            },
            {
              type: 'doc',
              id: 'topics/08.04.01.PartMarshallers',
              label: '8.4.1. Part Marshallers',
            },
            {
              type: 'doc',
              id: 'topics/08.04.02.HttpPartAnnotations',
              label: '8.4.2. HTTP Part Annotations',
            },
            {
              type: 'doc',
              id: 'topics/08.04.03.DefaultParts',
              label: '8.4.3. Default Parts',
            },
            {
              type: 'doc',
              id: 'topics/08.04.04.RequestBeans',
              label: '8.4.4. Request Beans',
            },
            {
              type: 'doc',
              id: 'topics/08.04.05.ResponseBeans',
              label: '8.4.5. Response Beans',
            },
            {
              type: 'doc',
              id: 'topics/08.04.06.HttpPartApis',
              label: '8.4.6. HTTP Part APIs',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/08.05.HttpParts',
          label: '8.5. HTTP Parts',
        },
        {
          type: 'doc',
          id: 'topics/08.06.Marshalling',
          label: '8.6. Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/08.07.HandlingFormPosts',
          label: '8.7. Handling Form Posts',
        },
        {
          type: 'doc',
          id: 'topics/08.08.Guards',
          label: '8.8. Guards',
        },
        {
          type: 'doc',
          id: 'topics/08.09.Converters',
          label: '8.9. Converters',
        },
        {
          type: 'doc',
          id: 'topics/08.10.LocalizedMessages',
          label: '8.10. Localized Messages',
        },
        {
          type: 'doc',
          id: 'topics/08.11.Encoders',
          label: '8.11. Encoders',
        },
        {
          type: 'doc',
          id: 'topics/08.12.ConfigurationFiles',
          label: '8.12. Configuration Files',
        },
        {
          type: 'doc',
          id: 'topics/08.13.SvlVariables',
          label: '8.13. SVL Variables',
        },
        {
          type: 'doc',
          id: 'topics/08.14.StaticFiles',
          label: '8.14. Static Files',
        },
        {
          type: 'category',
          label: '8.15. Client Versioning',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/08.15.ClientVersioning',
              label: '8.15. Client Versioning',
            },
            {
              type: 'doc',
              id: 'topics/08.15.01.BasicRestServletSwagger',
              label: '8.15.1. Basic REST Servlet Swagger',
            },
            {
              type: 'doc',
              id: 'topics/08.15.02.BasicSwaggerInfo',
              label: '8.15.2. Basic Swagger Info',
            },
            {
              type: 'doc',
              id: 'topics/08.15.03.SwaggerTags',
              label: '8.15.3. Swagger Tags',
            },
            {
              type: 'doc',
              id: 'topics/08.15.04.SwaggerOperations',
              label: '8.15.4. Swagger Operations',
            },
            {
              type: 'doc',
              id: 'topics/08.15.05.SwaggerParameters',
              label: '8.15.5. Swagger Parameters',
            },
            {
              type: 'doc',
              id: 'topics/08.15.06.SwaggerResponses',
              label: '8.15.6. Swagger Responses',
            },
            {
              type: 'doc',
              id: 'topics/08.15.07.SwaggerModels',
              label: '8.15.7. Swagger Models',
            },
            {
              type: 'doc',
              id: 'topics/08.15.08.SwaggerStylesheet',
              label: '8.15.8. Swagger Stylesheet',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/08.16.Swagger',
          label: '8.16. Swagger',
        },
        {
          type: 'category',
          label: '8.17. Execution Statistics',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/08.17.ExecutionStatistics',
              label: '8.17. Execution Statistics',
            },
            {
              type: 'doc',
              id: 'topics/08.17.01.HtmlUIvsDI',
              label: '8.17.1. HTML UI vs DI',
            },
            {
              type: 'doc',
              id: 'topics/08.17.02.HtmlWidgets',
              label: '8.17.2. HTML Widgets',
            },
            {
              type: 'doc',
              id: 'topics/08.17.03.HtmlPredefinedWidgets',
              label: '8.17.3. HTML Predefined Widgets',
            },
            {
              type: 'doc',
              id: 'topics/08.17.04.HtmlUiCustomization',
              label: '8.17.4. HTML UI Customization',
            },
            {
              type: 'doc',
              id: 'topics/08.17.05.HtmlStylesheets',
              label: '8.17.5. HTML Stylesheets',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/08.18.HtmlDocAnnotation',
          label: '8.18. HtmlDoc Annotation',
        },
        {
          type: 'doc',
          id: 'topics/08.19.LoggingAndDebugging',
          label: '8.19. Logging and Debugging',
        },
        {
          type: 'doc',
          id: 'topics/08.20.HttpStatusCodes',
          label: '8.20. HTTP Status Codes',
        },
        {
          type: 'doc',
          id: 'topics/08.21.BuiltInParameters',
          label: '8.21. Built-In Parameters',
        },
        {
          type: 'doc',
          id: 'topics/08.22.UsingWithOsgi',
          label: '8.22. Using with OSGi',
        },
        {
          type: 'doc',
          id: 'topics/08.23.RestContext',
          label: '8.23. REST Context',
        },
        {
          type: 'doc',
          id: 'topics/08.24.RestOpContext',
          label: '8.24. RestOp Context',
        },
        {
          type: 'doc',
          id: 'topics/08.25.ResponseProcessors',
          label: '8.25. Response Processors',
        },
        {
          type: 'doc',
          id: 'topics/08.26.RestRpc',
          label: '8.26. REST RPC',
        },
        {
          type: 'doc',
          id: 'topics/08.27.SerializingUris',
          label: '8.27. Serializing URIs',
        },
        {
          type: 'doc',
          id: 'topics/08.28.UtilityBeans',
          label: '8.28. Utility Beans',
        },
        {
          type: 'doc',
          id: 'topics/08.29.HtmlBeans',
          label: '8.29. HTML Beans',
        },
        {
          type: 'doc',
          id: 'topics/08.30.OtherNotes',
          label: '8.30. Other Notes',
        },
        {
          type: 'doc',
          id: 'topics/08.32.01.Log4j',
          label: '8.32.1. Log4j',
        },
      ],
    },
    {
      type: 'category',
      label: '9. REST Server SpringBoot',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/09.01.Module-juneau-rest-server-springboot',
          label: '9.1. Module: juneau-rest-server-springboot',
        },
        {
          type: 'doc',
          id: 'topics/09.02.Overview',
          label: '9.2. Overview',
        },
      ],
    },
    {
      type: 'category',
      label: '10. REST Client',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/10.01.Module-juneau-rest-client',
          label: '10.1. Module: juneau-rest-client',
        },
        {
          type: 'doc',
          id: 'topics/10.02.PojoMarshalling',
          label: '10.2. POJO Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/10.03.RequestParts',
          label: '10.3. Request Parts',
        },
        {
          type: 'doc',
          id: 'topics/10.04.RequestContent',
          label: '10.4. Request Content',
        },
        {
          type: 'doc',
          id: 'topics/10.05.ResponseStatus',
          label: '10.5. Response Status',
        },
        {
          type: 'doc',
          id: 'topics/10.06.ResponseHeaders',
          label: '10.6. Response Headers',
        },
        {
          type: 'doc',
          id: 'topics/10.07.ResponseContent',
          label: '10.7. Response Content',
        },
        {
          type: 'doc',
          id: 'topics/10.08.CustomCallHandlers',
          label: '10.8. Custom Call Handlers',
        },
        {
          type: 'category',
          label: '10.9. Interceptors',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/10.09.Interceptors',
              label: '10.9. Interceptors',
            },
            {
              type: 'doc',
              id: 'topics/10.09.01.Remote',
              label: '10.9.1. Remote',
            },
            {
              type: 'doc',
              id: 'topics/10.09.02.RemoteMethod',
              label: '10.9.2. Remote Method',
            },
            {
              type: 'doc',
              id: 'topics/10.09.03.Content',
              label: '10.9.3. Content',
            },
            {
              type: 'doc',
              id: 'topics/10.09.04.FormData',
              label: '10.9.4. Form Data',
            },
            {
              type: 'doc',
              id: 'topics/10.09.05.Query',
              label: '10.9.5. Query',
            },
            {
              type: 'doc',
              id: 'topics/10.09.06.Header',
              label: '10.9.6. Header',
            },
            {
              type: 'doc',
              id: 'topics/10.09.07.Path',
              label: '10.9.7. Path',
            },
            {
              type: 'doc',
              id: 'topics/10.09.08.Request',
              label: '10.9.8. Request',
            },
            {
              type: 'doc',
              id: 'topics/10.09.09.Response',
              label: '10.9.9. Response',
            },
            {
              type: 'doc',
              id: 'topics/10.09.10.DualPurposeInterfaces',
              label: '10.9.10. Dual Purpose Interfaces',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/10.10.Proxies',
          label: '10.10. Proxies',
        },
        {
          type: 'doc',
          id: 'topics/10.11.LoggingAndDebugging',
          label: '10.11. Logging and Debugging',
        },
        {
          type: 'doc',
          id: 'topics/10.12.CustomizingHttpClient',
          label: '10.12. Customizing HTTP Client',
        },
        {
          type: 'category',
          label: '10.13. Extending REST Client',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/10.13.ExtendingRestClient',
              label: '10.13. Extending REST Client',
            },
            {
              type: 'doc',
              id: 'topics/10.13.01.AuthenticationBASIC',
              label: '10.13.1. Authentication BASIC',
            },
            {
              type: 'doc',
              id: 'topics/10.13.02.AuthenticationForm',
              label: '10.13.2. Authentication Form',
            },
            {
              type: 'doc',
              id: 'topics/10.13.03.AuthenticationOIDC',
              label: '10.13.3. Authentication OIDC',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/10.14.Authentication',
          label: '10.14. Authentication',
        },
      ],
    },
    {
      type: 'category',
      label: '11. REST Mock',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/11.01.Module-juneau-rest-mock',
          label: '11.1. Module: juneau-rest-mock',
        },
        {
          type: 'doc',
          id: 'topics/11.02.MockRestClient',
          label: '11.2. Mock REST Client',
        },
      ],
    },
    {
      type: 'category',
      label: '12. Microservice Core',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/12.01.Module-juneau-microservice-core',
          label: '12.1. Module: juneau-microservice-core',
        },
        {
          type: 'doc',
          id: 'topics/12.02.Overview',
          label: '12.2. Overview',
        },
        {
          type: 'doc',
          id: 'topics/12.03.LifecycleMethods',
          label: '12.3. Lifecycle Methods',
        },
        {
          type: 'doc',
          id: 'topics/12.04.Args',
          label: '12.4. Args',
        },
        {
          type: 'doc',
          id: 'topics/12.05.Manifest',
          label: '12.5. Manifest',
        },
        {
          type: 'doc',
          id: 'topics/12.06.Config',
          label: '12.6. Config',
        },
        {
          type: 'doc',
          id: 'topics/12.07.SystemProperties',
          label: '12.7. System Properties',
        },
        {
          type: 'doc',
          id: 'topics/12.08.VarResolver',
          label: '12.8. Var Resolver',
        },
        {
          type: 'doc',
          id: 'topics/12.09.ConsoleCommands',
          label: '12.9. Console Commands',
        },
        {
          type: 'doc',
          id: 'topics/12.10.Listeners',
          label: '12.10. Listeners',
        },
      ],
    },
    {
      type: 'category',
      label: '13. Microservice Jetty',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/13.01.Module-juneau-microservice-jetty',
          label: '13.1. Module: juneau-microservice-jetty',
        },
        {
          type: 'doc',
          id: 'topics/13.02.Overview',
          label: '13.2. Overview',
        },
        {
          type: 'doc',
          id: 'topics/13.03.LifecycleMethods',
          label: '13.3. Lifecycle Methods',
        },
        {
          type: 'doc',
          id: 'topics/13.04.ResourceClasses',
          label: '13.4. Resource Classes',
        },
        {
          type: 'doc',
          id: 'topics/13.05.PredefinedResourceClasses',
          label: '13.5. Predefined Resource Classes',
        },
        {
          type: 'doc',
          id: 'topics/13.06.Config',
          label: '13.6. Config',
        },
        {
          type: 'doc',
          id: 'topics/13.07.JettyXml',
          label: '13.7. Jetty XML',
        },
        {
          type: 'doc',
          id: 'topics/13.08.UiCustomization',
          label: '13.8. UI Customization',
        },
        {
          type: 'doc',
          id: 'topics/13.09.Extending',
          label: '13.9. Extending',
        },
      ],
    },
    {
      type: 'category',
      label: '14. My Jetty Microservice',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/14.01.My-jetty-microservice',
          label: '14.1. My Jetty Microservice',
        },
        {
          type: 'doc',
          id: 'topics/14.02.Installing',
          label: '14.2. Installing',
        },
        {
          type: 'doc',
          id: 'topics/14.03.Running',
          label: '14.3. Running',
        },
        {
          type: 'doc',
          id: 'topics/14.04.Building',
          label: '14.4. Building',
        },
      ],
    },
    {
      type: 'category',
      label: '15. My SpringBoot Microservice',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/15.01.My-springboot-microservice',
          label: '15.1. My SpringBoot Microservice',
        },
        {
          type: 'doc',
          id: 'topics/15.02.Installing',
          label: '15.2. Installing',
        },
        {
          type: 'doc',
          id: 'topics/15.03.Running',
          label: '15.3. Running',
        },
        {
          type: 'doc',
          id: 'topics/15.04.Building',
          label: '15.4. Building',
        },
      ],
    },
    {
      type: 'category',
      label: '16. PetStore App',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/16.01.App-juneau-petstore',
          label: '16.1. App: juneau-petstore',
        },
        {
          type: 'doc',
          id: 'topics/16.02.RunningTheApp',
          label: '16.2. Running the App',
        },
        {
          type: 'doc',
          id: 'topics/16.03.App-juneau-petstore-api',
          label: '16.3. App: juneau-petstore-api',
        },
        {
          type: 'doc',
          id: 'topics/16.04.App-juneau-petstore-client',
          label: '16.4. App: juneau-petstore-client',
        },
        {
          type: 'doc',
          id: 'topics/16.05.App-juneau-petstore-server',
          label: '16.5. App: juneau-petstore-server',
        },
      ],
    },
    {
      type: 'category',
      label: '17. Security',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/17.01.Security',
          label: '17.1. Security',
        },
        {
          type: 'doc',
          id: 'topics/17.02.Marshall',
          label: '17.2. Marshall',
        },
        {
          type: 'doc',
          id: 'topics/17.03.Svl',
          label: '17.3. SVL',
        },
        {
          type: 'doc',
          id: 'topics/17.04.Rest',
          label: '17.4. REST',
        },
      ],
    },
    {
      type: 'doc',
      id: 'topics/18.01.V9.0-migration-guide',
      label: '18. V9.0 Migration Guide',
    },
    {
      type: 'doc',
      id: 'topics/TODO',
      label: 'TODO Links',
    },
    {
      type: 'category',
      label: 'Release Notes',
      items: [
        {
          type: 'category',
          label: 'Version 9.x',
          items: [
            {
              type: 'doc',
              id: 'release-notes/9.0.0',
              label: '9.0.0',
            },
          ],
        },
        {
          type: 'category',
          label: 'Version 8.x',
          items: [
            {
              type: 'doc',
              id: 'release-notes/8.2.0',
              label: '8.2.0',
            },
            {
              type: 'doc',
              id: 'release-notes/8.1.3',
              label: '8.1.3',
            },
            {
              type: 'doc',
              id: 'release-notes/8.1.2',
              label: '8.1.2',
            },
            {
              type: 'doc',
              id: 'release-notes/8.1.1',
              label: '8.1.1',
            },
            {
              type: 'doc',
              id: 'release-notes/8.1.0',
              label: '8.1.0',
            },
            {
              type: 'doc',
              id: 'release-notes/8.0.0',
              label: '8.0.0',
            },
          ],
        },
        {
          type: 'category',
          label: 'Version 7.x',
          items: [
            {
              type: 'doc',
              id: 'release-notes/7.2.2',
              label: '7.2.2',
            },
            {
              type: 'doc',
              id: 'release-notes/7.2.1',
              label: '7.2.1',
            },
            {
              type: 'doc',
              id: 'release-notes/7.2.0',
              label: '7.2.0',
            },
            {
              type: 'doc',
              id: 'release-notes/7.1.0',
              label: '7.1.0',
            },
            {
              type: 'doc',
              id: 'release-notes/7.0.1',
              label: '7.0.1',
            },
            {
              type: 'doc',
              id: 'release-notes/7.0.0',
              label: '7.0.0',
            },
          ],
        },
        {
          type: 'category',
          label: 'Version 6.x',
          items: [
            {
              type: 'doc',
              id: 'release-notes/6.4.0',
              label: '6.4.0',
            },
            {
              type: 'doc',
              id: 'release-notes/6.3.1',
              label: '6.3.1',
            },
            {
              type: 'doc',
              id: 'release-notes/6.3.0',
              label: '6.3.0',
            },
            {
              type: 'doc',
              id: 'release-notes/6.2.0',
              label: '6.2.0',
            },
            {
              type: 'doc',
              id: 'release-notes/6.1.0',
              label: '6.1.0',
            },
            {
              type: 'doc',
              id: 'release-notes/6.0.1',
              label: '6.0.1',
            },
            {
              type: 'doc',
              id: 'release-notes/6.0.0',
              label: '6.0.0',
            },
          ],
        },
        {
          type: 'category',
          label: 'Version 5.x',
          items: [
            {
              type: 'doc',
              id: 'release-notes/5.2.0.1',
              label: '5.2.0.1',
            },
            {
              type: 'doc',
              id: 'release-notes/5.2.0.0',
              label: '5.2.0.0',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.20',
              label: '5.1.0.20',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.19',
              label: '5.1.0.19',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.18',
              label: '5.1.0.18',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.17',
              label: '5.1.0.17',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.16',
              label: '5.1.0.16',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.15',
              label: '5.1.0.15',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.14',
              label: '5.1.0.14',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.13',
              label: '5.1.0.13',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.12',
              label: '5.1.0.12',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.11',
              label: '5.1.0.11',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.10',
              label: '5.1.0.10',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.09',
              label: '5.1.0.09',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.08',
              label: '5.1.0.08',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.07',
              label: '5.1.0.07',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.06',
              label: '5.1.0.06',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.05',
              label: '5.1.0.05',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.04',
              label: '5.1.0.04',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.03',
              label: '5.1.0.03',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.02',
              label: '5.1.0.02',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.01',
              label: '5.1.0.01',
            },
            {
              type: 'doc',
              id: 'release-notes/5.1.0.00',
              label: '5.1.0.00',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.36',
              label: '5.0.0.36',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.35',
              label: '5.0.0.35',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.34',
              label: '5.0.0.34',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.33',
              label: '5.0.0.33',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.32',
              label: '5.0.0.32',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.31',
              label: '5.0.0.31',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.30',
              label: '5.0.0.30',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.29',
              label: '5.0.0.29',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.28',
              label: '5.0.0.28',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.27',
              label: '5.0.0.27',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.26',
              label: '5.0.0.26',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.25',
              label: '5.0.0.25',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.24',
              label: '5.0.0.24',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.23',
              label: '5.0.0.23',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.22',
              label: '5.0.0.22',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.21',
              label: '5.0.0.21',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.20',
              label: '5.0.0.20',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.19',
              label: '5.0.0.19',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.18',
              label: '5.0.0.18',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.17',
              label: '5.0.0.17',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.16',
              label: '5.0.0.16',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.15',
              label: '5.0.0.15',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.14',
              label: '5.0.0.14',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.13',
              label: '5.0.0.13',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.12',
              label: '5.0.0.12',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.11',
              label: '5.0.0.11',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.10',
              label: '5.0.0.10',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.09',
              label: '5.0.0.09',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.08',
              label: '5.0.0.08',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.07',
              label: '5.0.0.07',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.06',
              label: '5.0.0.06',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.05',
              label: '5.0.0.05',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.04',
              label: '5.0.0.04',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.03',
              label: '5.0.0.03',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.02',
              label: '5.0.0.02',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.01',
              label: '5.0.0.01',
            },
            {
              type: 'doc',
              id: 'release-notes/5.0.0.00',
              label: '5.0.0.00',
            },
          ],
        },
      ],
    },
  ],

  // But you can create a sidebar manually
  /*
  tutorialSidebar: [
    'intro',
    'hello',
    {
      type: 'category',
      label: 'Tutorial',
      items: ['tutorial-basics/create-a-document'],
    },
  ],
   */
};

export default sidebars;
