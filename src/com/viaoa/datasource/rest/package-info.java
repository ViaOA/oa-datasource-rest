/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Provides REST-based client/server data-source communication for OA.
 * <p>
 * The classes in this package define REST interfaces and implementations
 * that allow OA clients to interact with OA servers using JSON over HTTP.
 *
 * <h2>Key Components</h2>
 * <ul>
 *   <li>{@link com.viaoa.datasource.rest.OADataSourceRestClient} — client-side proxy.</li>
 *   <li>{@link com.viaoa.datasource.rest.OADataSourceRestImpl} — server-side delegate.</li>
 *   <li>{@link com.viaoa.datasource.rest.RemoteRestClientInterface} — synchronization protocol.</li>
 *   <li>{@link com.viaoa.datasource.rest.RemoteRestClientImpl} — REST-aware base client.</li>
 * </ul>
 *
 * @since OA 4.0
 */
package com.viaoa.datasource.rest;
