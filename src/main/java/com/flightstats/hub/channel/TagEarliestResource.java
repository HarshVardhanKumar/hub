package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@SuppressWarnings("WeakerAccess")
@Path("/tag/{tag}/earliest")
public class TagEarliestResource {

    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private final static TagService tagService = HubProvider.getInstance(TagService.class);

    @GET
    public Response getEarliest(@PathParam("tag") String tag,
                                @QueryParam("stable") @DefaultValue("true") boolean stable,
                                @QueryParam("trace") @DefaultValue("false") boolean trace,
                                @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                @Context UriInfo uriInfo) {
        Collection<ChannelContentKey> contentKeys = tagService.getEarliest(getQuery(tag, 1, stable, location, epoch));
        if (!contentKeys.isEmpty()) {
            URI uri = uriInfo.getBaseUriBuilder()
                    .path(contentKeys.iterator().next().toUrl())
                    .queryParam("tag", tag)
                    .build();
            return Response.status(SEE_OTHER).location(uri).build();
        }
        return Response.status(NOT_FOUND).build();
    }

    private DirectionQuery getQuery(String tag, int count, boolean stable, String location, String epoch) {
        return DirectionQuery.builder()
                .tagName(tag)
                .next(true)
                .stable(stable)
                .count(count)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .build();
    }

    @GET
    @Path("/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEarliestCount(@PathParam("tag") String tag,
                                     @PathParam("count") int count,
                                     @QueryParam("stable") @DefaultValue("true") boolean stable,
                                     @QueryParam("batch") @DefaultValue("false") boolean batch,
                                     @QueryParam("bulk") @DefaultValue("false") boolean bulk,
                                     @QueryParam("trace") @DefaultValue("false") boolean trace,
                                     @QueryParam("location") @DefaultValue(Location.DEFAULT) String location,
                                     @QueryParam("epoch") @DefaultValue(Epoch.DEFAULT) String epoch,
                                     @HeaderParam("Accept") String accept,
                                     @Context UriInfo uriInfo) {
        DirectionQuery query = getQuery(tag, count, stable, location, epoch);
        SortedSet<ChannelContentKey> keys = tagService.getEarliest(query);
        if (bulk || batch) {
            return BulkBuilder.buildTag(tag, keys, tagService.getChannelService(), uriInfo, accept);
        }
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        List<ChannelContentKey> list = new ArrayList<>(keys);
        String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
        if (!list.isEmpty()) {
            ObjectNode next = links.putObject("next");
            next.put("href", baseUri + list.get(list.size() - 1).getContentKey().toUrl() + "/next/" + count);
        }
        ArrayNode ids = links.putArray("uris");
        for (ChannelContentKey key : keys) {
            ids.add(uriInfo.getBaseUri() + key.toUrl() + "?tag=" + tag);
        }
        return Response.ok(root).build();


    }

}
