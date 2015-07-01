package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.metrics.EventTimed;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.rest.PATCH;
import com.flightstats.hub.time.NTPMonitor;
import com.flightstats.hub.util.Sleeper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;

import static com.flightstats.hub.rest.Linked.linked;

/**
 * This resource represents a single channel in the Hub.
 */
@Path("/channel/{channel}")
public class ChannelResource {
    private final static Logger logger = LoggerFactory.getLogger(ChannelResource.class);
    private final ChannelService channelService;
    private final UriInfo uriInfo;
    private NTPMonitor ntpMonitor;

    @Inject
    public ChannelResource(ChannelService channelService, UriInfo uriInfo, NTPMonitor ntpMonitor) {
        this.channelService = channelService;
        this.uriInfo = uriInfo;
        this.ntpMonitor = ntpMonitor;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelMetadata(@PathParam("channel") String channelName) {
        ChannelConfig config = channelService.getChannelConfig(channelName);
        if (config == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        URI channelUri = LinkBuilder.buildChannelUri(config, uriInfo);
        Linked<ChannelConfig> linked = LinkBuilder.buildChannelLinks(config, channelUri);

        return Response.ok(channelUri).entity(linked).build();
    }

    @PUT
    @EventTimed(name = "channels.put")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@PathParam("channel") String channelName, String json) throws Exception {
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName);
        ChannelConfig channelConfig = ChannelConfig.fromJsonName(json, channelName);
        if (oldConfig != null) {
            logger.info("using old channel {} {}", oldConfig, oldConfig.getCreationDate().getTime());
            channelConfig = ChannelConfig.builder()
                    .withChannelConfiguration(oldConfig)
                    .withUpdateJson(json)
                    .build();
        }
        logger.info("creating channel {} {}", channelConfig, channelConfig.getCreationDate().getTime());
        channelConfig = channelService.updateChannel(channelConfig);
        URI channelUri = LinkBuilder.buildChannelUri(channelConfig, uriInfo);
        return Response.created(channelUri).entity(
                LinkBuilder.buildChannelLinks(channelConfig, channelUri))
                .build();
    }

    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetadata(@PathParam("channel") String channelName, String json) throws Exception {
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName);
        if (oldConfig == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        ChannelConfig newConfig = ChannelConfig.builder()
                .withChannelConfiguration(oldConfig)
                .withUpdateJson(json)
                .build();

        newConfig = channelService.updateChannel(newConfig);

        URI channelUri = LinkBuilder.buildChannelUri(newConfig, uriInfo);
        Linked<ChannelConfig> linked = LinkBuilder.buildChannelLinks(newConfig, channelUri);
        return Response.ok(channelUri).entity(linked).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channel") final String channelName, @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam("Content-Language") final String contentLanguage, @HeaderParam("User") final String user,
                                final InputStream data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        long start = System.currentTimeMillis();
        Content content = Content.builder()
                .withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withStream(data)
                .withUser(user)
                .build();
        try {
            ContentKey contentKey = channelService.insert(channelName, content);
            logger.trace("posted {}", contentKey);
            InsertedContentKey insertionResult = new InsertedContentKey(contentKey);
            URI payloadUri = LinkBuilder.buildItemUri(contentKey, uriInfo.getRequestUri());
            Linked<InsertedContentKey> linkedResult = linked(insertionResult)
                    .withLink("channel", LinkBuilder.buildChannelUri(channelName, uriInfo))
                    .withLink("self", payloadUri)
                    .build();

            Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
            builder.entity(linkedResult);
            builder.location(payloadUri);
            content.getTraces().logSlow(100, logger);
            long time = System.currentTimeMillis() - start;
            int postTimeBuffer = ntpMonitor.getPostTimeBuffer();
            if (time < postTimeBuffer) {
                Sleeper.sleep(postTimeBuffer - time);
            }
            return builder.build();
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            String key = "";
            if (content.getContentKey().isPresent()) {
                key = content.getContentKey().get().toString();
            }
            logger.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        if (channelService.delete(channelName)) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("channel " + channelName + " not found").build();
        }
    }

}
