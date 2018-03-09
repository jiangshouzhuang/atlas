/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.graphdb.titan0;

import com.thinkaurelius.titan.core.Cardinality;
import com.thinkaurelius.titan.core.EdgeLabel;
import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.schema.PropertyKeyMaker;
import com.thinkaurelius.titan.core.schema.TitanGraphIndex;
import com.thinkaurelius.titan.core.schema.TitanManagement;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasEdgeLabel;
import org.apache.atlas.repository.graphdb.AtlasGraphIndex;
import org.apache.atlas.repository.graphdb.AtlasGraphManagement;
import org.apache.atlas.repository.graphdb.AtlasPropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Titan 0.5.4 implementation of AtlasGraphManagement.
 */
public class Titan0GraphManagement implements AtlasGraphManagement {

    private static final Logger LOG = LoggerFactory.getLogger(Titan0GraphManagement.class);

    private final Titan0Graph graph;
    private final TitanManagement management;

    private Set<String> newMultProperties = new HashSet<>();

    public Titan0GraphManagement(Titan0Graph graph, TitanManagement managementSystem) {

        this.graph = graph;
        management = managementSystem;
    }

    @Override
    public void createEdgeMixedIndex(String index, String backingIndex, List<AtlasPropertyKey> propertyKeys) {
    }

    @Override
    public void createEdgeIndex(String label, String indexName, AtlasEdgeDirection edgeDirection, List<AtlasPropertyKey> propertyKeys) {
        EdgeLabel edgeLabel = management.getEdgeLabel(label);

        if (edgeLabel == null) {
            edgeLabel = management.makeEdgeLabel(label).make();
        }

        Direction     direction = TitanObjectFactory.createDirection(edgeDirection);
        PropertyKey[] keys      = TitanObjectFactory.createPropertyKeys(propertyKeys);

        management.buildEdgeIndex(edgeLabel, indexName, direction, keys);
    }

    @Override
    public void createFullTextMixedIndex(String index, String backingIndex, List<AtlasPropertyKey> propertyKeys) {
    }

    private void buildMixedIndex(String index, Class<? extends Element> titanClass, String backingIndex) {

        management.buildIndex(index, titanClass).buildMixedIndex(backingIndex);
    }

    @Override
    public boolean containsPropertyKey(String propertyKey) {
        return management.containsPropertyKey(propertyKey);
    }

    @Override
    public void rollback() {
        management.rollback();

    }

    @Override
    public void commit() {
        graph.addMultiProperties(newMultProperties);
        newMultProperties.clear();
        management.commit();
    }

    @Override
    public AtlasPropertyKey makePropertyKey(String propertyName, Class propertyClass, AtlasCardinality cardinality) {

        if (cardinality.isMany()) {
            newMultProperties.add(propertyName);
        }

        PropertyKeyMaker propertyKeyBuilder = management.makePropertyKey(propertyName).dataType(propertyClass);

        if (cardinality != null) {
            Cardinality titanCardinality = TitanObjectFactory.createCardinality(cardinality);
            propertyKeyBuilder.cardinality(titanCardinality);
        }
        PropertyKey propertyKey = propertyKeyBuilder.make();
        return GraphDbObjectFactory.createPropertyKey(propertyKey);
    }

    @Override
    public AtlasEdgeLabel makeEdgeLabel(String label) {
        EdgeLabel edgeLabel = management.makeEdgeLabel(label).make();

        return GraphDbObjectFactory.createEdgeLabel(edgeLabel);
    }

    @Override
    public void deletePropertyKey(String propertyKey) {
        PropertyKey titanPropertyKey = management.getPropertyKey(propertyKey);

        if (null == titanPropertyKey) return;

        for (int i = 0;; i++) {
            String deletedKeyName = titanPropertyKey + "_deleted_" + i;
            if (null == management.getPropertyKey(deletedKeyName)) {
                management.changeName(titanPropertyKey, deletedKeyName);
                break;
            }
        }
    }

    @Override
    public AtlasPropertyKey getPropertyKey(String propertyName) {

        return GraphDbObjectFactory.createPropertyKey(management.getPropertyKey(propertyName));
    }

    @Override
    public AtlasEdgeLabel getEdgeLabel(String label) {
        return GraphDbObjectFactory.createEdgeLabel(management.getEdgeLabel(label));
    }

    @Override
    public void createVertexCompositeIndex(String propertyName, boolean enforceUniqueness,
                                           List<AtlasPropertyKey> propertyKeys) {

        TitanManagement.IndexBuilder indexBuilder = management.buildIndex(propertyName, Vertex.class);
        for(AtlasPropertyKey key : propertyKeys) {
            PropertyKey titanKey = TitanObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(titanKey);
        }
        if (enforceUniqueness) {
            indexBuilder.unique();
        }
        indexBuilder.buildCompositeIndex();
    }

    @Override
    public void createEdgeCompositeIndex(String propertyName, boolean isUnique, List<AtlasPropertyKey> propertyKeys) {
        TitanManagement.IndexBuilder indexBuilder = management.buildIndex(propertyName, Edge.class);

        for(AtlasPropertyKey key : propertyKeys) {
            PropertyKey titanKey = TitanObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(titanKey);
        }

        if (isUnique) {
            indexBuilder.unique();
        }

        indexBuilder.buildCompositeIndex();
    }

    @Override
    public void createVertexMixedIndex(String propertyName, String backingIndex, List<AtlasPropertyKey> propertyKeys) {

        TitanManagement.IndexBuilder indexBuilder = management.buildIndex(propertyName, Vertex.class);
        for(AtlasPropertyKey key : propertyKeys) {
            PropertyKey titanKey = TitanObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(titanKey);
        }
        indexBuilder.buildMixedIndex(backingIndex);
    }


    @Override
    public void addMixedIndex(String indexName, AtlasPropertyKey propertyKey) {
        PropertyKey titanKey = TitanObjectFactory.createPropertyKey(propertyKey);
        TitanGraphIndex vertexIndex = management.getGraphIndex(indexName);
        management.addIndexKey(vertexIndex, titanKey);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.atlas.repository.graphdb.AtlasGraphManagement#getGraphIndex(
     * java.lang.String)
     */
    @Override
    public AtlasGraphIndex getGraphIndex(String indexName) {
        TitanGraphIndex index = management.getGraphIndex(indexName);
        return GraphDbObjectFactory.createGraphIndex(index);
    }

    @Override
    public boolean edgeIndexExist(String label, String indexName) {
        EdgeLabel edgeLabel = management.getEdgeLabel(label);

        return edgeLabel != null && management.getRelationIndex(edgeLabel, indexName) != null;
    }
}
