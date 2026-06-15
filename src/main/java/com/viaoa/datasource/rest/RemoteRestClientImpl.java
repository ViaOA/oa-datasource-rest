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
package com.viaoa.datasource.rest;

import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.sync.remote.RemoteClientImpl;

/**
 * Base class for REST client implementations that communicate with an OA Server
 * using the REST protocol.
 * <p>
 * Extends {@link com.viaoa.sync.remote.RemoteClientImpl} to manage session
 * context and provide a callback mechanism for caching objects on the client
 * to prevent premature garbage collection on the server.
 *
 */
public abstract class RemoteRestClientImpl extends RemoteClientImpl {
	private static Logger LOG = Logger.getLogger(RemoteRestClientImpl.class.getName());

	/**
	 * Constructs a REST client instance associated with the specified remote
	 * session. Delegates initialization to the superclass.
	 *
	 * @param sessionId the session identifier for the remote connection
	 */
	public RemoteRestClientImpl(int sessionId) {
		super(sessionId, null);
	}

	/**
	 * Callback invoked to inform the client that the given object should be
	 * referenced in the client-side cache. This prevents the server from
	 * garbage-collecting the object while it remains in use by the client.
	 *
	 * @param obj the object to retain in the client cache
	 */
	public abstract void setCached(OAObject obj);
}
