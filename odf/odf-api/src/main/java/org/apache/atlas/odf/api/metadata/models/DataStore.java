/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.odf.api.metadata.models;

import java.util.List;

import org.apache.atlas.odf.api.metadata.MetaDataObjectReference;

/**
 * This class represents a metadataobject that references other metadataobjects
 *
 */
public abstract class DataStore extends MetaDataObject {
	private List<MetaDataObjectReference> connections;

	public List<MetaDataObjectReference> getConnections() {
		return connections;
	}

	public void setConnections(List<MetaDataObjectReference> connections) {
		this.connections = connections;
	}

}