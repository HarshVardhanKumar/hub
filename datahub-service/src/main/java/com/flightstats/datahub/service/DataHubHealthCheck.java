package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.jerseyguice.jetty.health.HealthCheck;
import com.google.inject.Inject;

public class DataHubHealthCheck implements HealthCheck {

	private final ChannelDao channelDao;

	@Inject
	public DataHubHealthCheck(ChannelDao channelDao) {
		this.channelDao = channelDao;
	}

    @Override
    public boolean isHealthy() {
        //todo - gfm - 12/19/13 - this should check if zookeeper has a connection/quorum.
        return channelDao.isHealthy();
    }
}
