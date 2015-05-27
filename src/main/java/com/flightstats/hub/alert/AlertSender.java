package com.flightstats.hub.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.sun.jersey.api.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;

public class AlertSender {

    private final static Logger logger = LoggerFactory.getLogger(AlertSender.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String alertChannelEscalate = HubProperties.getProperty("alert.channel.escalate", "escalationAlerts");

    public static void create(Client client, String hubAppUrl) {
        client.resource(hubAppUrl + "channel/" + alertChannelEscalate)
                .type(MediaType.APPLICATION_JSON)
                .put("{\"ttlDays\":14, \"tags\":[\"alerts\"], \"description\":\"alerts to be sent and confirmations\"}");
    }

    public static void sendAlert(AlertConfig alertConfig, AlertStatus alertStatus, int count, Client client) {
        String url = alertConfig.getHubDomain() + "channel/" + alertChannelEscalate;
        ObjectNode alert = mapper.createObjectNode();
        alert.put("serviceName", alertConfig.getServiceName());
        alert.put("description", alertConfig.getName() + ": " +
                alertConfig.getHubDomain() + "channel/" + alertConfig.getChannel() + " volume " +
                count + " " + alertConfig.getOperator() + " " + alertConfig.getThreshold());
        alert.put("details", alertStatus.toJson());
        String entity = alert.toString();
        logger.info("sending alert {}", entity);
        client.resource(url)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .post(entity);
    }

}
