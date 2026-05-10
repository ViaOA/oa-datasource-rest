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

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.rest.annotation.OARestClass;
import com.viaoa.remote.rest.annotation.OARestMethod;
import com.viaoa.remote.rest.annotation.OARestMethod.MethodType;
import com.viaoa.sync.remote.RemoteClientInterface;

/**
 * REST-enabled version of {@link com.viaoa.sync.remote.RemoteClientInterface}
 * that maintains message ordering and synchronization consistency between
 * OA clients and servers.
 *
 * <h2>Usage</h2>
 * Implementations of this interface are automatically bound to the
 * {@code OARestServlet} and share the same message queue namespace as
 * the primary RemoteSync channel.
 */
@OARestClass()
public interface RemoteRestClientInterface extends RemoteClientInterface {

	/**
	 * Creates and returns a copy of the specified object on the remote server.
	 * Properties listed in {@code excludeProperties} will not be included in
	 * the copied object.
	 *
	 * @param objectClass the class of the object to copy
	 * @param objectKey the identity key of the object
	 * @param excludeProperties names of properties to exclude from the copy
	 * @return the copied {@link OAObject} instance
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	OAObject createCopy(Class objectClass, OAObjectKey objectKey, String[] excludeProperties);

	/**
	 * Retrieves detail data for a master object using the specified property.
	 *
	 * @param id client request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey the identity of the master object
	 * @param property the detail property to retrieve
	 * @param bForHubMerger whether results are used for hub-merging
	 * @return the retrieved detail value
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger);

	/**
	 * Retrieves detail data for a master object with additional master property
	 * values and sibling object keys used for more complex synchronization.
	 *
	 * @param id client request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey the identity of the master object
	 * @param property the detail property to retrieve
	 * @param masterProps property names used to provide master-side context
	 * @param siblingKeys keys of sibling objects included in the request
	 * @param bForHubMerger whether results are used for hub-merging
	 * @return the retrieved detail value
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);

	// dont put in queue, but have it returned on vsocket for queued messages
	//     All of the other methods are put in queue to be processed and have the return value set.
	//@OARemoteMethod(returnOnQueueSocket = true)

	/**
	 * Retrieves detail information immediately instead of placing the request
	 * into the remote message queue. Used for synchronous operations that
	 * require an immediate return value.
	 *
	 * @param id client request identifier
	 * @param masterClass the class of the master object
	 * @param masterObjectKey the identity of the master object
	 * @param property the detail property name
	 * @param masterProps property names used to provide master-side context
	 * @param siblingKeys keys of sibling objects included in the request
	 * @param bForHubMerger whether results are used for hub-merging
	 * @return the detail value returned immediately
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey,
			String property, String[] masterProps, OAObjectKey[] siblingKeys, boolean bForHubMerger);

	/**
	 * Executes a data source command on the server and returns the result.
	 *
	 * @param command the numeric command identifier
	 * @param objects optional command parameters
	 * @return the result returned by the remote data source
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	Object datasource(int command, Object[] objects);

	/**
	 * Executes a data source command on the server without expecting a return
	 * value. Used for one-way operations.
	 *
	 * @param command the numeric command identifier
	 * @param objects optional command parameters
	 */
	//@OARemoteMethod(noReturnValue = true)
	@OARestMethod(methodType = MethodType.OARemote)
	void datasourceNoReturn(int command, Object[] objects);

	/**
	 * Deletes the object identified by the given class and key on the server.
	 *
	 * @param objectClass the class of the object to delete
	 * @param objectKey the identity key of the object
	 * @return {@code true} if deletion succeeded; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean delete(Class objectClass, OAObjectKey objectKey);

	/**
	 * Deletes all objects linked to a master object through the specified hub
	 * property.
	 *
	 * @param objectClass the class of the master object
	 * @param objectKey the identity of the master object
	 * @param hubPropertyName the hub-property whose linked objects should be deleted
	 * @return {@code true} if deletion succeeded; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName);
}
