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
  // Main Juneau sidebar
  mainSidebar: [
    // Documentation section (topics from /docs folder)
    {
      type: 'category',
      label: 'Documentation',
      collapsed: false,
      items: [
        {
          type: 'category',
          label: '1. Juneau Ecosystem',
          collapsed: false,
          items: [
        {
          type: 'doc',
          id: 'topics/01.01.00.JuneauEcosystemOverview',
          label: '1.1. Juneau Ecosystem Overview',
        },
        {
          type: 'doc',
          id: 'topics/01.02.00.Marshalling',
          label: '1.2. Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/01.03.00.EndToEndRest',
          label: '1.3. End-to-End REST',
        },
        {
          type: 'doc',
          id: 'topics/01.04.00.RestServer',
          label: '1.4. REST Server',
        },
        {
          type: 'doc',
          id: 'topics/01.05.00.RestClient',
          label: '1.5. REST Client',
        },
        {
          type: 'doc',
          id: 'topics/01.06.00.DtoBeans',
          label: '1.6. DTO Beans',
        },
        {
          type: 'doc',
          id: 'topics/01.07.00.ConfigFiles',
          label: '1.7. Config Files',
        },
        {
          type: 'doc',
          id: 'topics/01.08.00.FluentAssertions',
          label: '1.8. Fluent Assertions',
        },
        {
          type: 'doc',
          id: 'topics/01.09.00.GeneralDesign',
          label: '1.9. General Design',
        },
        {
          type: 'doc',
          id: 'topics/01.10.00.FrameworkComparisons',
          label: '1.10. Framework Comparisons',
        },
        {
          type: 'doc',
          id: 'topics/01.11.00.WhyJuneau',
          label: '1.11. Why Choose Juneau?',
        },
      ],
    },
    {
      type: 'category',
      label: '2. juneau-marshall',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/02.01.00.JuneauMarshallBasics',
          label: '2.1. juneau-marshall Basics',
        },
        {
          type: 'doc',
          id: 'topics/02.02.00.Marshallers',
          label: '2.2. Marshallers',
        },
        {
          type: 'doc',
          id: 'topics/02.03.00.SerializersAndParsers',
          label: '2.3. Serializers and Parsers',
        },
        {
          type: 'category',
          label: '2.4. Bean Contexts',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.04.01.BeanContextBasics',
              label: '2.4.1. Bean Context Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.04.02.JavaBeansSupport',
              label: '2.4.2. Java Beans Support',
            },
            {
              type: 'doc',
              id: 'topics/02.04.03.BeanAnnotation',
              label: '2.4.3. @Bean Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.04.BeanpAnnotation',
              label: '2.4.4. @Beanp Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.05.BeancAnnotation',
              label: '2.4.5. @Beanc Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.06.BeanIgnoreAnnotation',
              label: '2.4.6. @BeanIgnore Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.07.NamePropertyAnnotation',
              label: '2.4.7. @NameProperty Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.08.ParentPropertyAnnotation',
              label: '2.4.8. @ParentProperty Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.04.09.PojoBuilders',
              label: '2.4.9. POJO Builders',
            },
            {
              type: 'doc',
              id: 'topics/02.04.10.BypassSerialization',
              label: '2.4.10. Bypass Serialization',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.05.00.HttpPartSerializersParsers',
          label: '2.5. HTTP Part Serializers/Parsers',
        },
        {
          type: 'doc',
          id: 'topics/02.06.00.ContextSettings',
          label: '2.6. Context Settings',
        },
        {
          type: 'doc',
          id: 'topics/02.07.00.ContextAnnotations',
          label: '2.7. Context Annotations',
        },
        {
          type: 'doc',
          id: 'topics/02.08.00.JsonMap',
          label: '2.8. JsonMap',
        },
        {
          type: 'doc',
          id: 'topics/02.09.00.ComplexDataTypes',
          label: '2.9. Complex Data Types',
        },
        {
          type: 'doc',
          id: 'topics/02.10.00.SerializerSetsParserSets',
          label: '2.10. Serializer/Parser Sets',
        },
        {
          type: 'category',
          label: '2.11. Swaps',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.11.01.SwapBasics',
              label: '2.11.1. Swap Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.11.02.DefaultSwaps',
              label: '2.11.2. Default Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.03.AutoSwaps',
              label: '2.11.3. Auto-detected swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.04.PerMediaTypeSwaps',
              label: '2.11.4. Per-media-type Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.05.OneWaySwaps',
              label: '2.11.5. One-way Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.06.SwapAnnotation',
              label: '2.11.6. @Swap Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.11.07.TemplatedSwaps',
              label: '2.11.7. Templated Swaps',
            },
            {
              type: 'doc',
              id: 'topics/02.11.08.SurrogateClasses',
              label: '2.11.8. Surrogate Classes',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.12.00.DynamicallyAppliedAnnotations',
          label: '2.12. Dynamically Applied Annotations',
        },
        {
          type: 'category',
          label: '2.13. Bean Dictionaries',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.13.01.BeanDictionaryBasics',
              label: '2.13.1. Bean Dictionary Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.13.02.BeanSubTypes',
              label: '2.13.2. Bean Sub Types',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.14.00.VirtualBeans',
          label: '2.14. Virtual Beans',
        },
        {
          type: 'doc',
          id: 'topics/02.15.00.Recursion',
          label: '2.15. Recursion',
        },
        {
          type: 'doc',
          id: 'topics/02.16.00.ParsingIntoGenericModels',
          label: '2.16. Parsing into Generic Models',
        },
        {
          type: 'doc',
          id: 'topics/02.17.00.ReadingContinuousStreams',
          label: '2.17. Reading Continuous Streams',
        },
        {
          type: 'doc',
          id: 'topics/02.18.00.MarshallingUris',
          label: '2.18. Marshalling URIs',
        },
        {
          type: 'doc',
          id: 'topics/02.19.00.JacksonComparison',
          label: '2.19. Jackson Comparison',
        },
        {
          type: 'doc',
          id: 'topics/02.20.00.PojoCategories',
          label: '2.20. POJO Categories',
        },
        {
          type: 'category',
          label: '2.21. Simple Variable Language',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.21.01.SimpleVariableLanguageBasics',
              label: '2.21.1. Simple Variable Language Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.21.02.SvlVariables',
              label: '2.21.2. SVL Variables',
            },
            {
              type: 'doc',
              id: 'topics/02.21.03.VarResolvers',
              label: '2.21.3. Var Resolvers',
            },
            {
              type: 'doc',
              id: 'topics/02.21.04.DefaultVarResolver',
              label: '2.21.4. Default Var Resolver',
            },
            {
              type: 'doc',
              id: 'topics/02.21.05.SvlOtherNotes',
              label: '2.21.5. SVL Other Notes',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.22.00.Encoders',
          label: '2.22. Encoders',
        },
        {
          type: 'doc',
          id: 'topics/02.23.00.ObjectTools',
          label: '2.23. Object Tools',
        },
        {
          type: 'category',
          label: '2.24. JSON Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.24.01.JsonBasics',
              label: '2.24.1. JSON Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.24.02.JsonMethodology',
              label: '2.24.2. JSON Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.24.03.JsonSerializers',
              label: '2.24.3. JSON Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.24.04.Json5',
              label: '2.24.4. JSON5',
            },
            {
              type: 'doc',
              id: 'topics/02.24.05.JsonParsers',
              label: '2.24.5. JSON Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.24.06.JsonAnnotation',
              label: '2.24.6. @Json Annotation',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.25.00.JsonSchemaDetails',
          label: '2.25. JSON Schema Support',
        },
        {
          type: 'category',
          label: '2.26. XML Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.26.01.XmlBasics',
              label: '2.26.1. XML Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.26.02.XmlMethodology',
              label: '2.26.2. XML Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.26.03.XmlSerializers',
              label: '2.26.3. XML Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.26.04.XmlParsers',
              label: '2.26.4. XML Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.26.05.XmlBeanTypeNameAnnotation',
              label: '2.26.5. @XmlBeanTypeName Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.06.XmlChildNameAnnotation',
              label: '2.26.6. @XmlChildName Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.07.XmlFormatAnnotation',
              label: '2.26.7. @XmlFormat Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.26.08.XmlNamespaces',
              label: '2.26.8. XML Namespaces',
            },
          ],
        },
        {
          type: 'category',
          label: '2.27. HTML Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.27.01.HtmlBasics',
              label: '2.27.1. HTML Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.27.02.HtmlMethodology',
              label: '2.27.2. HTML Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.27.03.HtmlSerializers',
              label: '2.27.3. HTML Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.27.04.HtmlParsers',
              label: '2.27.4. HTML Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.27.05.HtmlAnnotation',
              label: '2.27.5. @Html Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.27.06.HtmlRenderAnnotation',
              label: '2.27.6. @HtmlRender Annotation',
            },
            {
              type: 'doc',
              id: 'topics/02.27.07.HtmlDocSerializer',
              label: '2.27.7. HtmlDocSerializer',
            },
            {
              type: 'doc',
              id: 'topics/02.27.08.BasicHtmlDocTemplate',
              label: '2.27.8. BasicHtmlDocTemplate',
            },
            {
              type: 'doc',
              id: 'topics/02.27.09.HtmlCustomTemplates',
              label: '2.27.9. HTML Custom Templates',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.28.00.HtmlSchemaSupport',
          label: '2.28. HTML-Schema Support',
        },
        {
          type: 'category',
          label: '2.29. UON Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.29.01.UonBasics',
              label: '2.29.1. UON Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.29.02.UonMethodology',
              label: '2.29.2. UON Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.29.03.UonSerializers',
              label: '2.29.3. UON Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.29.04.UonParsers',
              label: '2.29.4. UON Parsers',
            },
          ],
        },
        {
          type: 'category',
          label: '2.30. URL-Encoding Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.30.01.UrlEncodingBasics',
              label: '2.30.1. URL-Encoding Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.30.02.UrlEncMethodology',
              label: '2.30.2. URL-Encoding Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.30.03.UrlEncSerializers',
              label: '2.30.3. URL-Encoding Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.30.04.UrlEncParsers',
              label: '2.30.4. URL-Encoding Parsers',
            },
            {
              type: 'doc',
              id: 'topics/02.30.05.UrlEncodingAnnotation',
              label: '2.30.5. @UrlEncoding Annotation',
            },
          ],
        },
        {
          type: 'category',
          label: '2.31. MessagePack Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.31.01.MessagePackBasics',
              label: '2.31.1. MessagePack Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.31.02.MsgPackSerializers',
              label: '2.31.2. MessagePack Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.31.03.MsgPackParsers',
              label: '2.31.3. MessagePack Parsers',
            },
          ],
        },
        {
          type: 'category',
          label: '2.32. OpenApi Support',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/02.32.01.OpenApiBasics',
              label: '2.32.1. OpenApi Basics',
            },
            {
              type: 'doc',
              id: 'topics/02.32.02.OpenApiMethodology',
              label: '2.32.2. OpenAPI Methodology',
            },
            {
              type: 'doc',
              id: 'topics/02.32.03.OpenApiSerializers',
              label: '2.32.3. OpenAPI Serializers',
            },
            {
              type: 'doc',
              id: 'topics/02.32.04.OpenApiParsers',
              label: '2.32.4. OpenAPI Parsers',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/02.33.00.BestPractices',
          label: '2.33. Best Practices',
        },
      ],
    },
    {
      type: 'doc',
      id: 'topics/03.01.00.Module-juneau-marshall-rdf',
      label: '3. juneau-marshall-rdf',
    },
    {
      type: 'category',
      label: '4. juneau-bean',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/04.01.00.JuneauBeanBasics',
          label: '4.1. juneau-bean Basics',
        },
        {
          type: 'doc',
          id: 'topics/04.02.00.JuneauBeanHtml5',
          label: '4.2. juneau-bean-html5',
        },
        {
          type: 'doc',
          id: 'topics/04.03.00.JuneauBeanAtom',
          label: '4.3. juneau-bean-atom',
        },
        {
          type: 'doc',
          id: 'topics/04.04.00.JuneauBeanJsonSchema',
          label: '4.4. juneau-bean-jsonschema',
        },
        {
          type: 'doc',
          id: 'topics/04.05.00.JuneauBeanOpenApi3',
          label: '4.5. juneau-bean-openapi-v3',
        },
        {
          type: 'doc',
          id: 'topics/04.06.00.JuneauBeanCommon',
          label: '4.6. juneau-bean-common',
        },
        {
          type: 'doc',
          id: 'topics/04.07.00.JuneauBeanSwagger2',
          label: '4.7. juneau-bean-swagger-v2',
        },
      ],
    },
    {
      type: 'category',
      label: '5. juneau-config',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/05.01.00.JuneauConfigBasics',
          label: '5.1. juneau-config Basics',
        },
        {
          type: 'category',
          label: '5.2. Overview',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.02.01.ConfigOverviewBasics',
              label: '5.2.1. Config Overview Basics',
            },
            {
              type: 'doc',
              id: 'topics/05.02.02.SyntaxRules',
              label: '5.2.2. Syntax Rules',
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
              id: 'topics/05.03.01.ReadingEntriesBasics',
              label: '5.3.1. Reading Entries Basics',
            },
            {
              type: 'doc',
              id: 'topics/05.03.02.Pojos',
              label: '5.3.2. POJOs',
            },
            {
              type: 'doc',
              id: 'topics/05.03.03.Arrays',
              label: '5.3.3. Arrays',
            },
            {
              type: 'doc',
              id: 'topics/05.03.04.JCFObjects',
              label: '5.3.4. Java Collection Framework Objects',
            },
            {
              type: 'doc',
              id: 'topics/05.03.05.BinaryData',
              label: '5.3.5. Binary Data',
            },
          ],
        },
        {
          type: 'category',
          label: '5.4. Variables',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.04.01.VariableBasics',
              label: '5.4.1. Variable Basics',
            },
            {
              type: 'doc',
              id: 'topics/05.04.02.LogicVariables',
              label: '5.4.2. Logic Variables',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.05.00.ModdedEntries',
          label: '5.5. Modded/Encoded Entries',
        },
        {
          type: 'doc',
          id: 'topics/05.06.00.Sections',
          label: '5.6. Sections',
        },
        {
          type: 'category',
          label: '5.7. Setting Values',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.07.01.SettingValuesBasics',
              label: '5.7.1. Setting Values Basics',
            },
            {
              type: 'doc',
              id: 'topics/05.07.02.FileSystemChanges',
              label: '5.7.2. File System Changes',
            },
            {
              type: 'doc',
              id: 'topics/05.07.03.CustomEntrySerialization',
              label: '5.7.3. Custom Entry Serialization',
            },
            {
              type: 'doc',
              id: 'topics/05.07.04.BulkSettingValues',
              label: '5.7.4. Bulk Setting Values',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.08.00.Listeners',
          label: '5.8. Listeners',
        },
        {
          type: 'doc',
          id: 'topics/05.09.00.SerializingConfigs',
          label: '5.9. Serializing Configs',
        },
        {
          type: 'doc',
          id: 'topics/05.10.00.Imports',
          label: '5.10. Imports',
        },
        {
          type: 'category',
          label: '5.11. Config Stores',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/05.11.01.ConfigStoresBasics',
              label: '5.11.1. Config Stores Basics',
            },
            {
              type: 'doc',
              id: 'topics/05.11.02.MemoryStore',
              label: '5.11.2. Memory Store',
            },
            {
              type: 'doc',
              id: 'topics/05.11.03.FileStore',
              label: '5.11.3. File Store',
            },
            {
              type: 'doc',
              id: 'topics/05.11.04.CustomStores',
              label: '5.11.4. Custom Stores',
            },
            {
              type: 'doc',
              id: 'topics/05.11.05.StoreListeners',
              label: '5.11.5. Store Listeners',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/05.12.00.ReadOnlyConfigs',
          label: '5.12. Read-only Configs',
        },
        {
          type: 'doc',
          id: 'topics/05.13.00.ClosingConfigs',
          label: '5.13. Closing Configs',
        },
        {
          type: 'doc',
          id: 'topics/05.14.00.SystemDefaultConfig',
          label: '5.14. System Default Config',
        },
      ],
    },
    {
      type: 'category',
      label: '6. juneau-assertions',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/06.01.00.JuneauAssertionBasics',
          label: '6.1. juneau-assertions Basics',
        },
        {
          type: 'doc',
          id: 'topics/06.02.00.AssertionsOverview',
          label: '6.2. Assertions Overview',
        },
      ],
    },
    {
      type: 'category',
      label: '7. juneau-bct',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/07.01.00.JuneauBctBasics',
          label: '7.1. juneau-bct Basics',
        },
        {
          type: 'doc',
          id: 'topics/07.01.01.Stringifiers',
          label: '7.1.1. Stringifiers',
        },
        {
          type: 'doc',
          id: 'topics/07.01.02.Listifiers',
          label: '7.1.2. Listifiers',
        },
        {
          type: 'doc',
          id: 'topics/07.01.03.Swappers',
          label: '7.1.3. Swappers',
        },
        {
          type: 'doc',
          id: 'topics/07.01.04.PropertyExtractors',
          label: '7.1.4. Property Extractors',
        },
        {
          type: 'doc',
          id: 'topics/07.01.05.CustomErrorMessages',
          label: '7.1.5. Custom Error Messages',
        },
      ],
    },
    {
      type: 'category',
      label: '8. juneau-rest-common',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/08.01.00.JuneauRestCommonBasics',
          label: '8.1. juneau-rest-common Basics',
        },
        {
          type: 'doc',
          id: 'topics/08.02.00.HelperClasses',
          label: '8.2. Helper Classes',
        },
        {
          type: 'doc',
          id: 'topics/08.03.00.Annotations',
          label: '8.3. Annotations',
        },
        {
          type: 'doc',
          id: 'topics/08.04.00.HttpHeaders',
          label: '8.4. HTTP Headers',
        },
        {
          type: 'doc',
          id: 'topics/08.05.00.HttpParts',
          label: '9.5. HTTP Parts',
        },
        {
          type: 'doc',
          id: 'topics/08.06.00.HttpEntitiesAndResources',
          label: '8.6. HTTP Entities and Resources',
        },
        {
          type: 'doc',
          id: 'topics/08.07.00.HttpResponses',
          label: '8.7. HTTP Responses',
        },
        {
          type: 'doc',
          id: 'topics/08.08.00.RemoteProxyInterfaces',
          label: '8.8. Remote Proxy Interfaces',
        },
      ],
    },
    {
      type: 'category',
      label: '9. juneau-rest-server',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/09.01.00.JuneauRestServerBasics',
          label: '9.1. juneau-rest-server Basics',
        },
        {
          type: 'doc',
          id: 'topics/09.02.00.RestServerOverview',
          label: '9.2. REST Server Overview',
        },
        {
          type: 'category',
          label: '9.3. @Rest-Annotated Classes',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/09.03.01.RestAnnotatedClassBasics',
              label: '9.3.1. @Rest-Annotated Class Basics',
            },
            {
              type: 'doc',
              id: 'topics/09.03.02.PredefinedClasses',
              label: '9.3.2. Predefined Classes',
            },
            {
              type: 'doc',
              id: 'topics/09.03.03.ChildResources',
              label: '9.3.3. Child Resources',
            },
            {
              type: 'doc',
              id: 'topics/09.03.04.PathVariables',
              label: '9.3.4. Path Variables',
            },
            {
              type: 'doc',
              id: 'topics/09.03.05.Deployment',
              label: '9.3.5. Deployment',
            },
            {
              type: 'doc',
              id: 'topics/09.03.06.LifecycleHooks',
              label: '9.3.6. Lifecycle Hooks',
            },
          ],
        },
        {
          type: 'category',
          label: '9.4. @RestOp-Annotated Methods',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/09.04.01.RestOpAnnotatedMethodBasics',
              label: '9.4.1. @RestOp-Annotated Method Basics',
            },
            {
              type: 'doc',
              id: 'topics/09.04.02.InferredHttpMethodsAndPaths',
              label: '9.4.2. Inferred HTTP Methods and Paths',
            },
            {
              type: 'doc',
              id: 'topics/09.04.03.JavaMethodParameters',
              label: '9.4.3. Java Method Parameters',
            },
            {
              type: 'doc',
              id: 'topics/09.04.04.JavaMethodReturnTypes',
              label: '9.4.4. Java Method Return Types',
            },
            {
              type: 'doc',
              id: 'topics/09.04.05.JavaMethodThrowableTypes',
              label: '9.4.5. Java Method Throwable Types',
            },
            {
              type: 'doc',
              id: 'topics/09.04.06.PathPatterns',
              label: '9.4.6. Path Patterns',
            },
            {
              type: 'doc',
              id: 'topics/09.04.07.Matchers',
              label: '9.4.7. Matchers',
            },
            {
              type: 'doc',
              id: 'topics/09.04.08.OverloadingHttpMethods',
              label: '9.4.8. Overloading HTTP Methods',
            },
            {
              type: 'doc',
              id: 'topics/09.04.09.AdditionalInformation',
              label: '9.4.9. Additional Information',
            },
          ],
        },
        {
          type: 'category',
          label: '9.5. HTTP Parts',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/09.05.01.HttpPartBasics',
              label: '9.5.1. HTTP Part Basics',
            },
            {
              type: 'doc',
              id: 'topics/09.05.02.PartMarshallers',
              label: '9.5.2. Part Marshallers',
            },
            {
              type: 'doc',
              id: 'topics/09.05.03.HttpPartAnnotations',
              label: '9.5.3. HTTP Part Annotations',
            },
            {
              type: 'doc',
              id: 'topics/09.05.04.DefaultParts',
              label: '9.5.4. Default Parts',
            },
            {
              type: 'doc',
              id: 'topics/09.05.05.RequestBeans',
              label: '9.5.5. Request Beans',
            },
            {
              type: 'doc',
              id: 'topics/09.05.06.ResponseBeans',
              label: '9.5.6. Response Beans',
            },
            {
              type: 'doc',
              id: 'topics/09.05.07.HttpPartApis',
              label: '9.5.7. HTTP Part APIs',
            },
            {
              type: 'doc',
              id: 'topics/09.05.08.HttpPartValidation',
              label: '9.5.8. HTTP Part Validation',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/09.06.00.Marshalling',
          label: '9.6. Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/09.07.00.HandlingFormPosts',
          label: '9.7. Handling Form Posts',
        },
        {
          type: 'doc',
          id: 'topics/09.08.00.Guards',
          label: '9.8. Guards',
        },
        {
          type: 'doc',
          id: 'topics/09.09.00.Converters',
          label: '9.9. Converters',
        },
        {
          type: 'doc',
          id: 'topics/09.10.00.LocalizedMessages',
          label: '9.10. Localized Messages',
        },
        {
          type: 'doc',
          id: 'topics/09.11.00.Encoders',
          label: '9.11. Encoders',
        },
        {
          type: 'doc',
          id: 'topics/09.12.00.ConfigurationFiles',
          label: '9.12. Configuration Files',
        },
        {
          type: 'doc',
          id: 'topics/09.13.00.SvlVariables',
          label: '9.13. SVL Variables',
        },
        {
          type: 'doc',
          id: 'topics/09.14.00.StaticFiles',
          label: '9.14. Static Files',
        },
        {
          type: 'doc',
          id: 'topics/09.15.00.ClientVersioning',
          label: '9.15. Client Versioning',
        },
        {
          type: 'category',
          label: '9.16. Swagger',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/09.16.01.SwaggerBasics',
              label: '9.16.1. Swagger Basics',
            },
            {
              type: 'doc',
              id: 'topics/09.16.02.BasicRestServletSwagger',
              label: '9.16.2. Basic REST Servlet Swagger',
            },
            {
              type: 'doc',
              id: 'topics/09.16.03.BasicSwaggerInfo',
              label: '9.16.3. Basic Swagger Info',
            },
            {
              type: 'doc',
              id: 'topics/09.16.04.SwaggerTags',
              label: '9.16.4. Swagger Tags',
            },
            {
              type: 'doc',
              id: 'topics/09.16.05.SwaggerOperations',
              label: '9.16.5. Swagger Operations',
            },
            {
              type: 'doc',
              id: 'topics/09.16.06.SwaggerParameters',
              label: '9.16.6. Swagger Parameters',
            },
            {
              type: 'doc',
              id: 'topics/09.16.07.SwaggerResponses',
              label: '9.16.7. Swagger Responses',
            },
            {
              type: 'doc',
              id: 'topics/09.16.08.SwaggerModels',
              label: '9.16.8. Swagger Models',
            },
            {
              type: 'doc',
              id: 'topics/09.16.09.SwaggerStylesheet',
              label: '9.16.9. Swagger Stylesheet',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/09.17.00.ExecutionStatistics',
          label: '9.17. REST method execution statistics',
        },
        {
          type: 'category',
          label: '9.18. @HtmlDocConfig Annotation',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/09.18.01.HtmlDocConfigAnnotationBasics',
              label: '9.18.1. @HtmlDocConfig Annotation Basics',
            },
            {
              type: 'doc',
              id: 'topics/09.18.02.HtmlUIvsDI',
              label: '9.18.2. HTML UI vs DI',
            },
            {
              type: 'doc',
              id: 'topics/09.18.03.HtmlWidgets',
              label: '9.18.3. HTML Widgets',
            },
            {
              type: 'doc',
              id: 'topics/09.18.04.HtmlPredefinedWidgets',
              label: '9.18.4. HTML Predefined Widgets',
            },
            {
              type: 'doc',
              id: 'topics/09.18.05.HtmlUiCustomization',
              label: '9.18.5. HTML UI Customization',
            },
            {
              type: 'doc',
              id: 'topics/09.18.06.HtmlStylesheets',
              label: '9.18.6. HTML Stylesheets',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/09.19.00.LoggingAndDebugging',
          label: '9.19. Logging and Debugging',
        },
        {
          type: 'doc',
          id: 'topics/09.20.00.HttpStatusCodes',
          label: '9.20. HTTP Status Codes',
        },
        {
          type: 'doc',
          id: 'topics/09.21.00.BuiltInParameters',
          label: '9.21. Built-In Parameters',
        },
        {
          type: 'doc',
          id: 'topics/09.22.00.UsingWithOsgi',
          label: '9.22. Using with OSGi',
        },
        {
          type: 'doc',
          id: 'topics/09.23.00.RestContext',
          label: '9.23. REST Context',
        },
        {
          type: 'doc',
          id: 'topics/09.24.00.RestOpContext',
          label: '9.24. RestOp Context',
        },
        {
          type: 'doc',
          id: 'topics/09.25.00.ResponseProcessors',
          label: '9.25. Response Processors',
        },
        {
          type: 'doc',
          id: 'topics/09.26.00.RestRpc',
          label: '9.26. REST RPC',
        },
        {
          type: 'doc',
          id: 'topics/09.27.00.SerializingUris',
          label: '9.27. Serializing URIs',
        },
        {
          type: 'doc',
          id: 'topics/09.28.00.UtilityBeans',
          label: '9.28. Utility Beans',
        },
        {
          type: 'doc',
          id: 'topics/09.29.00.HtmlBeans',
          label: '9.29. HTML Beans',
        },
        {
          type: 'doc',
          id: 'topics/09.30.00.OtherNotes',
          label: '9.30. Other Notes',
        },
        {
          type: 'doc',
          id: 'topics/09.31.00.Log4j',
          label: '9.31. Log4J',
        },
      ],
    },
    {
      type: 'category',
      label: '10. juneau-rest-server-springboot',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/10.01.00.JuneauRestServerSpringbootBasics',
          label: '10.1. juneau-rest-server-springboot Basics',
        },
        {
          type: 'doc',
          id: 'topics/10.02.00.SpringBootOverview',
          label: '10.2. Spring Boot Overview',
        },
      ],
    },
    {
      type: 'category',
      label: '11. juneau-rest-client',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/11.01.00.JuneauRestClientBasics',
          label: '11.1. juneau-rest-client Basics',
        },
        {
          type: 'doc',
          id: 'topics/11.02.00.PojoMarshalling',
          label: '11.2. POJO Marshalling',
        },
        {
          type: 'doc',
          id: 'topics/11.03.00.RequestParts',
          label: '11.3. Request Parts',
        },
        {
          type: 'doc',
          id: 'topics/11.04.00.RequestContent',
          label: '11.4. Request Content',
        },
        {
          type: 'doc',
          id: 'topics/11.05.00.ResponseStatus',
          label: '11.5. Response Status',
        },
        {
          type: 'doc',
          id: 'topics/11.06.00.ResponseHeaders',
          label: '11.6. Response Headers',
        },
        {
          type: 'doc',
          id: 'topics/11.07.00.ResponseContent',
          label: '11.7. Response Content',
        },
        {
          type: 'doc',
          id: 'topics/11.08.00.CustomCallHandlers',
          label: '11.8. Custom Call Handlers',
        },
        {
          type: 'doc',
          id: 'topics/11.09.00.Interceptors',
          label: '11.9. Interceptors',
        },
        {
          type: 'category',
          label: '11.10. REST Proxies',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/11.10.01.RestProxyBasics',
              label: '11.10.1. REST Proxy Basics',
            },
            {
              type: 'doc',
              id: 'topics/11.10.02.Remote',
              label: '11.10.2. @Remote',
            },
            {
              type: 'doc',
              id: 'topics/11.10.03.RemoteMethod',
              label: '11.10.3. @RemoteMethod',
            },
            {
              type: 'doc',
              id: 'topics/11.10.04.Content',
              label: '11.10.4. @Content',
            },
            {
              type: 'doc',
              id: 'topics/11.10.05.FormData',
              label: '11.10.5. @FormData',
            },
            {
              type: 'doc',
              id: 'topics/11.10.06.Query',
              label: '11.10.6. @Query',
            },
            {
              type: 'doc',
              id: 'topics/11.10.07.Header',
              label: '11.10.7. @Header',
            },
            {
              type: 'doc',
              id: 'topics/11.10.08.Path',
              label: '11.10.8. @Path',
            },
            {
              type: 'doc',
              id: 'topics/11.10.09.Request',
              label: '11.10.9. @Request',
            },
            {
              type: 'doc',
              id: 'topics/11.10.10.Response',
              label: '11.10.10. @Response',
            },
            {
              type: 'doc',
              id: 'topics/11.10.11.DualPurposeInterfaces',
              label: '11.10.11. Dual-Purpose Interfaces',
            },
          ],
        },
        {
          type: 'doc',
          id: 'topics/11.11.00.LoggingAndDebugging',
          label: '11.11. Logging and Debugging',
        },
        {
          type: 'doc',
          id: 'topics/11.12.00.CustomizingHttpClient',
          label: '11.12. Customizing HTTP Client',
        },
        {
          type: 'doc',
          id: 'topics/11.13.00.ExtendingRestClient',
          label: '11.13. Extending REST Client',
        },
        {
          type: 'category',
          label: '11.14. Authentication',
          collapsed: true,
          items: [
            {
              type: 'doc',
              id: 'topics/11.14.01.AuthenticationBasics',
              label: '11.14.1. Authentication Basics',
            },
            {
              type: 'doc',
              id: 'topics/11.14.02.AuthenticationBASIC',
              label: '11.14.2. BASIC Authentication',
            },
            {
              type: 'doc',
              id: 'topics/11.14.03.AuthenticationForm',
              label: '11.14.3. Form-Based Authentication',
            },
            {
              type: 'doc',
              id: 'topics/11.14.04.AuthenticationOIDC',
              label: '11.14.4. OIDC Authentication',
            },
          ],
        },
      ],
    },
    {
      type: 'category',
      label: '12. juneau-rest-mock',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/12.01.00.JuneauRestMockBasics',
          label: '12.1. juneau-rest-mock Basics',
        },
        {
          type: 'doc',
          id: 'topics/12.02.00.MockRestClientOverview',
          label: '12.2. Mock REST Client Overview',
        },
      ],
    },
    {
      type: 'category',
      label: '13. juneau-microservice-core',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/13.01.00.JuneauMicroserviceCoreBasics',
          label: '13.1. juneau-microservice-core Basics',
        },
        {
          type: 'doc',
          id: 'topics/13.02.00.MicroserviceCoreOverview',
          label: '13.2. Microservice Core Overview',
        },
        {
          type: 'doc',
          id: 'topics/14.03.00.LifecycleMethods',
          label: '14.3. Lifecycle Methods',
        },
        {
          type: 'doc',
          id: 'topics/13.04.00.Args',
          label: '13.4. Args',
        },
        {
          type: 'doc',
          id: 'topics/13.05.00.Manifest',
          label: '13.5. Manifest',
        },
        {
          type: 'doc',
          id: 'topics/14.06.00.Config',
          label: '14.6. Config',
        },
        {
          type: 'doc',
          id: 'topics/13.07.00.SystemProperties',
          label: '13.7. System Properties',
        },
        {
          type: 'doc',
          id: 'topics/13.08.00.VarResolver',
          label: '13.8. Var Resolver',
        },
        {
          type: 'doc',
          id: 'topics/13.09.00.ConsoleCommands',
          label: '13.9. Console Commands',
        },
        {
          type: 'doc',
          id: 'topics/13.10.00.Listeners',
          label: '13.10. Listeners',
        },
      ],
    },
    {
      type: 'category',
      label: '14. juneau-microservice-jetty',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/14.01.00.JuneauMicroserviceJettyBasics',
          label: '14.1. juneau-microservice-jetty Basics',
        },
        {
          type: 'doc',
          id: 'topics/14.02.00.MicroserviceJettyOverview',
          label: '14.2. Microservice Jetty Overview',
        },
        {
          type: 'doc',
          id: 'topics/14.03.00.LifecycleMethods',
          label: '14.3. Lifecycle Methods',
        },
        {
          type: 'doc',
          id: 'topics/14.04.00.ResourceClasses',
          label: '14.4. Resource Classes',
        },
        {
          type: 'doc',
          id: 'topics/14.05.00.PredefinedResourceClasses',
          label: '14.5. Predefined Resource Classes',
        },
        {
          type: 'doc',
          id: 'topics/14.06.00.Config',
          label: '14.6. Config',
        },
        {
          type: 'doc',
          id: 'topics/14.07.00.JettyXml',
          label: '14.7. Jetty XML',
        },
        {
          type: 'doc',
          id: 'topics/14.08.00.UiCustomization',
          label: '14.8. UI Customization',
        },
        {
          type: 'doc',
          id: 'topics/14.09.00.Extending',
          label: '14.9. Extending',
        },
      ],
    },
    {
      type: 'category',
      label: '15. My Jetty Microservice',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/15.01.00.MyJettyMicroserviceBasics',
          label: '15.1. My Jetty Microservice Basics',
        },
        {
          type: 'doc',
          id: 'topics/15.02.00.MyJettyMicroserviceInstalling',
          label: '16.2. Installing',
        },
        {
          type: 'doc',
          id: 'topics/15.03.00.MyJettyMicroserviceRunning',
          label: '16.3. Running',
        },
        {
          type: 'doc',
          id: 'topics/15.04.00.MyJettyMicroserviceBuilding',
          label: '16.4. Building',
        },
      ],
    },
    {
      type: 'category',
      label: '16. My SpringBoot Microservice',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/16.01.00.MySpringBootMicroserviceBasics',
          label: '16.1. My SpringBoot Microservice Basics',
        },
        {
          type: 'doc',
          id: 'topics/16.02.00.MySpringBootMicroserviceInstalling',
          label: '16.2. Installing',
        },
        {
          type: 'doc',
          id: 'topics/16.03.00.MySpringBootMicroserviceRunning',
          label: '16.3. Running',
        },
        {
          type: 'doc',
          id: 'topics/16.04.00.MySpringBootMicroserviceBuilding',
          label: '16.4. Building',
        },
      ],
    },
    {
      type: 'category',
      label: '17. juneau-petstore',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/17.01.00.JuneauPetstoreBasics',
          label: '17.1. juneau-petstore Basics',
        },
        {
          type: 'doc',
          id: 'topics/17.02.00.JuneauPetstoreRunning',
          label: '17.2. Running the App',
        },
        {
          type: 'doc',
          id: 'topics/17.03.00.JuneauPetstoreApi',
          label: '17.3. juneau-petstore-api',
        },
        {
          type: 'doc',
          id: 'topics/17.04.00.JuneauPetstoreClient',
          label: '17.4. juneau-petstore-client',
        },
        {
          type: 'doc',
          id: 'topics/17.05.00.JuneauPetstoreServer',
          label: '17.5. juneau-petstore-server',
        },
      ],
    },
    {
      type: 'category',
      label: '18. juneau-examples',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/18.01.00.JuneauExamplesCore',
          label: '18.1. juneau-examples-core',
        },
        {
          type: 'doc',
          id: 'topics/18.02.00.JuneauExamplesRest',
          label: '18.2. juneau-examples-rest',
        },
        {
          type: 'doc',
          id: 'topics/18.03.00.JuneauExamplesRestJetty',
          label: '18.3. juneau-examples-rest-jetty',
        },
        {
          type: 'doc',
          id: 'topics/18.04.00.JuneauExamplesRestSpringboot',
          label: '18.4. juneau-examples-rest-springboot',
        },
      ],
    },
    {
      type: 'category',
      label: '19. juneau-shaded',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/19.01.00.JuneauShadedOverview',
          label: '19.1. Juneau Shaded Overview',
        },
        {
          type: 'doc',
          id: 'topics/19.02.00.JuneauShadedCore',
          label: '19.2. juneau-shaded-core',
        },
        {
          type: 'doc',
          id: 'topics/19.03.00.JuneauShadedRestClient',
          label: '19.3. juneau-shaded-rest-client',
        },
        {
          type: 'doc',
          id: 'topics/19.04.00.JuneauShadedRestServer',
          label: '19.4. juneau-shaded-rest-server',
        },
        {
          type: 'doc',
          id: 'topics/19.05.00.JuneauShadedRestServerSpringboot',
          label: '19.5. juneau-shaded-rest-server-springboot',
        },
        {
          type: 'doc',
          id: 'topics/19.06.00.JuneauShadedAll',
          label: '19.6. juneau-shaded-all',
        },
      ],
    },
    {
      type: 'category',
      label: '20. Security',
      collapsed: true,
      items: [
        {
          type: 'doc',
          id: 'topics/20.01.00.SecurityBasics',
          label: '20.1. Security Basics',
        },
        {
          type: 'doc',
          id: 'topics/20.02.00.MarshallingSecurity',
          label: '20.2. Marshalling Security',
        },
        {
          type: 'doc',
          id: 'topics/20.03.00.SvlSecurity',
          label: '20.3. SVL Security',
        },
        {
          type: 'doc',
          id: 'topics/20.04.00.RestSecurity',
          label: '20.4. REST Security',
        },
      ],
        },
        {
          type: 'doc',
          id: 'topics/21.01.00.V9.0-migration-guide',
          label: '21. V9.0 Migration Guide',
        },
      ],
    },
    // Release Notes section 
    {
      type: 'category',
      label: 'Release Notes',
      collapsed: true,
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
