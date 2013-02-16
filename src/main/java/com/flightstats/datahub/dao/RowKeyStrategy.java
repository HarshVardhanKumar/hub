package com.flightstats.datahub.dao;

/**
 * Strategy that chooses the key for a given payload.
 */
public interface RowKeyStrategy<K, T, V> {

    K buildKey(String channelName, T columnName);
}
