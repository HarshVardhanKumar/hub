package com.flightstats.hub.health;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.metrics.InternalTracesResource;
import com.flightstats.hub.spoke.RemoteSpokeStore;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

@SuppressWarnings("WeakerAccess")
@Path("/internal/health")
public class InternalHealthResource {
    private static final Client client = HubProvider.getInstance(Client.class);
    private static final ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth(@Context UriInfo uriInfo) {
        ObjectNode healthRoot = InternalTracesResource.serverAndServers("/health");
        ObjectNode root = mapper.createObjectNode();
        JsonNode servers = healthRoot.get("servers");
        for (JsonNode server : servers) {
            callHealth(root, server.asText());
        }
        return Response.ok(root).build();
    }

    private void callHealth(ObjectNode root, String link) {
        ClientResponse response = null;
        try {
            response = client.resource(link).get(ClientResponse.class);
            String string = response.getEntity(String.class);
            JsonNode jsonNode = mapper.readTree(string);
            root.set(link, jsonNode);
        } catch (IOException e) {
            root.put(link, "unable to get response " + e.getMessage());
        } finally {
            RemoteSpokeStore.close(response);
        }
    }


}
