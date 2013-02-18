package com.flightstats.datahub.dao;

import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Let's hide the fact that HFactory is all static methods.  :/
 */
public class HectorFactoryWrapper {

    public KeyspaceDefinition createKeyspaceDefinition(String keyspaceName, String defStrategyClass, int replicationFactor, List<ColumnFamilyDefinition> columnFamilyDefinitions) {
        return HFactory.createKeyspaceDefinition(keyspaceName, defStrategyClass, replicationFactor, columnFamilyDefinitions);
    }

    public <K> Mutator<K> createMutator(Keyspace keyspace, Serializer<K> keySerializer) {
        return HFactory.createMutator(keyspace, keySerializer);
    }

    public ColumnFamilyDefinition createColumnFamilyDefinition(String keyspaceName, String columnFamilyName) {
        return HFactory.createColumnFamilyDefinition(keyspaceName, columnFamilyName);
    }

    public <K, V> HColumn<K, V> createColumn(K name, V value, Serializer<K> nameSerializer, Serializer<V> valueSerializer) {
        return HFactory.createColumn(name, value, nameSerializer, valueSerializer);
    }

    public <K, N, V> ColumnQuery<K, N, V> createColumnQuery(Keyspace keyspace, Serializer<K> keySerializer, Serializer<N> nameSerializer, Serializer<V> valueSerializer) {
        return HFactory.createColumnQuery(keyspace, keySerializer, nameSerializer, valueSerializer);
    }

    public Keyspace createKeyspace(String keyspaceName, Cluster cluster) {
        return HFactory.createKeyspace(keyspaceName, cluster);
    }

    public UUID getUniqueTimeUUIDinMillis() {
        return TimeUUIDUtils.getUniqueTimeUUIDinMillis();
    }

    public long getTimeFromUUID(UUID uuid) {
        return TimeUUIDUtils.getTimeFromUUID(uuid);
    }

    public Date getDateFromUUID(UUID uuid) {
        return new Date(getTimeFromUUID(uuid));
    }
}
