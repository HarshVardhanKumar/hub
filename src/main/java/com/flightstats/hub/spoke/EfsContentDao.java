package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.file.SingleContentService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Wrapper to use existing SingleContentService
 */
public class EfsContentDao implements ContentDao {

    @Inject
    private SingleContentService contentService;

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return contentService.insert(channelName, content);
    }

    @Override
    public SortedSet<ContentKey> insert(BulkContent bulkContent) throws Exception {
        return new TreeSet<>(contentService.insert(bulkContent));
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        return contentService.get(channelName, key).orNull();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .startKey(limitKey)
                .earliestTime(TimeUtil.now().minusMinutes(HubProperties.getSpokeTtl()))
                .epoch(Epoch.IMMUTABLE)
                .count(1)
                .build();
        return contentService.getLatest(query);
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        throw new UnsupportedOperationException("deleteBefore is not supported");
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        return new TreeSet<>(contentService.queryByTime(query));
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        return new TreeSet<>(contentService.queryDirection(query));
    }

    @Override
    public void delete(String channelName) {
        contentService.delete(channelName);
    }

    @Override
    public void initialize() {
        //do anything?
    }
}
