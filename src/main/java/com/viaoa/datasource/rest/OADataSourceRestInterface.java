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
import com.viaoa.remote.rest.annotation.OARestClass;
import com.viaoa.remote.rest.annotation.OARestMethod;
import com.viaoa.remote.rest.annotation.OARestMethod.MethodType;
import com.viaoa.remote.rest.annotation.OARestParam;
import com.viaoa.remote.rest.annotation.OARestParam.ParamType;

/**
 * Defines the REST API contract for remote OADataSource access.
 * Each method is annotated with {@link com.viaoa.remote.rest.annotation.OARestMethod}
 * for automatic exposure via {@code OARestServlet}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>CRUD operations: insert, update, delete, save</li>
 *   <li>Query operations: select, count, next</li>
 *   <li>Link updates: updateMany2ManyLinks</li>
 *   <li>Utility: assignId, supportsStorage, execute</li>
 * </ul>
 *
 */
@OARestClass()
public interface OADataSourceRestInterface {

	/**
	 * Indicates whether the remote data source assigns an identifier when a new
	 * object is created.
	 *
	 * @return {@code true} if ID assignment on create is supported; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean getAssignIdOnCreate();

	/**
	 * Determines whether the remote data source is currently available for
	 * operations.
	 *
	 * @return {@code true} if the data source can be accessed; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean isAvailable();

	/**
	 * Retrieves the maximum allowed length for a property on a given class.
	 *
	 * @param c the class declaring the property
	 * @param propertyName the property name
	 * @return the maximum length value provided by the remote data source
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	int getMaxLength(Class c, String propertyName);

	/**
	 * Checks whether the remote data source supports the specified class.
	 *
	 * @param clazz the class to check
	 * @return {@code true} if supported; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean isClassSupported(Class clazz);

	/**
	 * Inserts the given object into the remote data source without processing
	 * its reference properties.
	 *
	 * @param obj the object to insert
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void insertWithoutReferences(OAObject obj);

	/**
	 * Inserts the specified object into the remote data source.
	 *
	 * @param obj the object to insert
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void insert(OAObject obj);

	/**
	 * Updates the specified object through the remote data source. Optional
	 * include/exclude property lists may be supplied to limit the update scope.
	 *
	 * @param obj the object to update
	 * @param includeProperties properties to include in the update
	 * @param excludeProperties properties to exclude from the update
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void update(OAObject obj, String[] includeProperties, String[] excludeProperties);

	/**
	 * Saves the specified object through the remote data source, performing an
	 * insert or update depending on its state.
	 *
	 * @param obj the object to save
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void save(OAObject obj);

	/**
	 * Deletes the specified object from the remote data source.
	 *
	 * @param obj the object to delete
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void delete(OAObject obj);

	/**
	 * Deletes all objects of the given class from the remote data source.
	 *
	 * @param c the class whose instances should be deleted
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void deleteAll(Class c);

	/**
	 * Counts the number of objects that match the specified selection criteria.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere the WHERE clause filter
	 * @param params optional query parameter values
	 * @param whereObjectClass class of an optional where-object
	 * @param whereKey identity key used to resolve the where-object
	 * @param propertyFromWhereObject property name used for linked filtering
	 * @param extraWhere additional filter criteria
	 * @param max maximum count to return
	 * @return the number of matching objects
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	int count(Class selectClass, String queryWhere, Object[] params, Class whereObjectClass, String whereKey,
			String propertyFromWhereObject, String extraWhere, int max);

	/**
	 * Executes a pass-through count operation on the remote data source using
	 * a raw WHERE clause.
	 *
	 * @param selectClass the class to count
	 * @param queryWhere the WHERE clause
	 * @param max maximum count
	 * @return the number of matching objects
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	int countPassthru(Class selectClass, String queryWhere, int max);

	/**
	 * Indicates whether the underlying remote data source supports persistent
	 * storage.
	 *
	 * @return {@code true} if storage is supported; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean supportsStorage();

	/**
	 * Executes a select query on the remote data source and returns a handle
	 * identifying the server-side iterator for retrieving results.
	 *
	 * @param selectClass the class to select
	 * @param queryWhere the WHERE clause
	 * @param params optional parameter values
	 * @param queryOrderBy the ORDER BY clause
	 * @param whereObjectClass class of an optional where-object
	 * @param whereKey key used to resolve the where-object
	 * @param propertyFromWhereObject linked-property name used for filtering
	 * @param extraWhere additional filtering
	 * @param max maximum number of results
	 * @param bDirty whether dirty objects should be included
	 * @return a select identifier for subsequent retrieval
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	int select(Class selectClass,
			String queryWhere, Object[] params, String queryOrderBy,
			Class whereObjectClass, String whereKey, String propertyFromWhereObject, String extraWhere,
			int max, boolean bDirty);

	/**
	 * Executes a pass-through select query on the remote data source, returning
	 * a handle for retrieving the results.
	 *
	 * @param selectClass the class to select
	 * @param queryWhere raw WHERE clause
	 * @param queryOrder ORDER BY clause
	 * @param max maximum number of results
	 * @param bDirty whether dirty objects should be included
	 * @return a select identifier for retrieving results
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	int selectPassThru(Class selectClass,
			String queryWhere, String queryOrder,
			int max, boolean bDirty);

	/**
	 * Executes an arbitrary command on the remote data source.
	 *
	 * @param command the command text
	 * @return the result returned by the remote data source
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	Object execute(String command);

	/**
	 * Requests that the remote data source assign an identifier to the
	 * specified object.
	 *
	 * @param obj the object requiring an ID
	 * @param class1 the class to use for return-type resolution
	 * @return the same object instance, with an assigned identifier
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	OAObject assignId(OAObject obj, @OARestParam(type = ParamType.MethodReturnClass) Class<? extends OAObject> class1);

	/**
	 * Indicates whether the remote data source will create a value for the
	 * specified property.
	 *
	 * @param object the object being evaluated
	 * @param propertyName the property being checked
	 * @return {@code true} if a value will be created; otherwise {@code false}
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	boolean willCreatePropertyValue(OAObject object, String propertyName);

	/**
	 * Updates many-to-many relationship links on the remote data source.
	 *
	 * @param masterClass the class of the master object
	 * @param masterId the identity of the master object
	 * @param adds objects to add to the relationship
	 * @param addClazz the class of added objects
	 * @param removes objects to remove from the relationship
	 * @param removeClazz the class of removed objects
	 * @param propertyNameFromMaster the master-side relationship property name
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void updateMany2ManyLinks(Class masterClass, String masterId, OAObject[] adds, Class addClazz, OAObject[] removes, Class removeClazz,
			String propertyNameFromMaster);

	/**
	 * Retrieves the next block of objects from a previously issued select query.
	 *
	 * @param selectId the identifier of the active select iterator
	 * @param clazz the expected result type
	 * @return an array of returned objects
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	OAObject[] next(int selectId, @OARestParam(type = ParamType.MethodReturnClass) Class clazz);

	/**
	 * Closes and removes the iterator associated with the specified select
	 * identifier on the remote data source.
	 *
	 * @param selectId the identifier of the select iterator to remove
	 */
	@OARestMethod(methodType = MethodType.OARemote)
	void removeSelect(int selectId);
}
