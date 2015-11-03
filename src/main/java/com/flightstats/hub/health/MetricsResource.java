package com.flightstats.hub.health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.MetricsRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/health/metrics")
public class MetricsResource {
    private final static Logger logger = LoggerFactory.getLogger(MetricsResource.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkHealth() {
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("openFiles", MetricsRunner.getOpenFiles());
        return Response.ok(rootNode).build();
    }


}
