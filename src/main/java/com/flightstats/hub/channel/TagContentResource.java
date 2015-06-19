package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.TagService;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.flightstats.hub.util.TimeUtil.Unit;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/tag/{tag}")
public class TagContentResource {

    private final static Logger logger = LoggerFactory.getLogger(TagContentResource.class);

    private final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Inject
    private ObjectMapper mapper;
    @Inject
    private UriInfo uriInfo;
    @Inject
    private TagService tagService;
    @Inject
    private LinkBuilder linkBuilder;
    @Inject
    MetricsSender sender;


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagLinks(@PathParam("tag") String tag) {
        Iterable<ChannelConfig> channels = tagService.getChannels(tag);
        Map<String, URI> mappedUris = new HashMap<>();
        for (ChannelConfig channelConfig : channels) {
            String channelName = channelConfig.getName();
            mappedUris.put(channelName, LinkBuilder.buildChannelUri(channelName, uriInfo));
        }
        Linked<?> result = LinkBuilder.buildLinks(mappedUris, "channels", builder -> {
            String uri = uriInfo.getRequestUri().toString();
            builder.withLink("self", uriInfo.getRequestUri())
                    //.withLink("latest", uri + "/latest")
                    //.withLink("earliest", uri + "/earliest")
                    //.withLink("status", uri + "/status")
                    .withLink("time", uri + "/time");

        });
        return Response.ok(result).build();
    }

    @Path("/{Y}/{M}/{D}/")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getDay(@PathParam("tag") String tag,
                           @PathParam("Y") int year,
                           @PathParam("M") int month,
                           @PathParam("D") int day,
                           @QueryParam("location") @DefaultValue("ALL") String location,
                           @QueryParam("trace") @DefaultValue("false") boolean trace,
                           @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.DAYS);
    }

    @Path("/{Y}/{M}/{D}/{hour}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getHour(@PathParam("tag") String tag,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("hour") int hour,
                            @QueryParam("location") @DefaultValue("ALL") String location,
                            @QueryParam("trace") @DefaultValue("false") boolean trace,
                            @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, 0, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.HOURS);
    }

    @Path("/{Y}/{M}/{D}/{h}/{minute}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getMinute(@PathParam("tag") String tag,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("minute") int minute,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, 0, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.MINUTES);
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{second}")
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response getSecond(@PathParam("tag") String tag,
                              @PathParam("Y") int year,
                              @PathParam("M") int month,
                              @PathParam("D") int day,
                              @PathParam("h") int hour,
                              @PathParam("m") int minute,
                              @PathParam("second") int second,
                              @QueryParam("location") @DefaultValue("ALL") String location,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("stable") @DefaultValue("true") boolean stable) {
        DateTime startTime = new DateTime(year, month, day, hour, minute, second, 0, DateTimeZone.UTC);
        return getTimeQueryResponse(tag, startTime, location, trace, stable, Unit.SECONDS);
    }

    public Response getTimeQueryResponse(String tag, DateTime startTime, String location, boolean trace, boolean stable,
                                         Unit unit) {
        TimeQuery query = TimeQuery.builder()
                .tagName(tag)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .build();
        query.trace(trace);
        Collection<ChannelContentKey> keys = tagService.queryByTime(query);
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        DateTime next = startTime.plus(unit.getDuration());
        DateTime previous = startTime.minus(unit.getDuration());
        if (next.isBefore(TimeUtil.time(stable))) {
            links.putObject("next").put("href", uriInfo.getBaseUri() + "tag/" + tag + "/" + unit.format(next) + "?stable=" + stable);
        }
        links.putObject("previous").put("href", uriInfo.getBaseUri() + "tag/" + tag + "/" + unit.format(previous) + "?stable=" + stable);
        ArrayNode ids = links.putArray("uris");
        for (ChannelContentKey key : keys) {
            URI channelUri = LinkBuilder.buildChannelUri(key.getChannel(), uriInfo);
            URI uri = LinkBuilder.buildItemUri(key.getContentKey(), channelUri);
            ids.add(uri.toString() + "?tag=" + tag);
        }
        query.getTraces().output(root);
        return Response.ok(root).build();
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/next")
    @GET
    public Response getNext(@PathParam("tag") String tag,
                            @PathParam("Y") int year,
                            @PathParam("M") int month,
                            @PathParam("D") int day,
                            @PathParam("h") int hour,
                            @PathParam("m") int minute,
                            @PathParam("s") int second,
                            @PathParam("ms") int millis,
                            @PathParam("hash") String hash,
                            @QueryParam("stable") @DefaultValue("true") boolean stable) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(tag, contentKey, stable, true, tagService, uriInfo);
    }

    @Path("/{Y}/{M}/{D}/{h}/{m}/{s}/{ms}/{hash}/previous")
    @GET
    public Response getPrevious(@PathParam("tag") String tag,
                                @PathParam("Y") int year,
                                @PathParam("M") int month,
                                @PathParam("D") int day,
                                @PathParam("h") int hour,
                                @PathParam("m") int minute,
                                @PathParam("s") int second,
                                @PathParam("ms") int millis,
                                @PathParam("hash") String hash,
                                @QueryParam("stable") @DefaultValue("true") boolean stable) {
        ContentKey contentKey = new ContentKey(year, month, day, hour, minute, second, millis, hash);
        return adjacent(tag, contentKey, stable, false, tagService, uriInfo);
    }

    public static Response adjacent(String tag, ContentKey contentKey, boolean stable, boolean next,
                                    TagService tagService, UriInfo uriInfo) {
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .contentKey(contentKey)
                .next(next)
                .stable(stable)
                .count(1).build();
        query.trace(false);
        Collection<ChannelContentKey> keys = tagService.getKeys(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        ChannelContentKey foundKey = keys.iterator().next();
        URI uri = uriInfo.getBaseUriBuilder()
                .path("channel")
                .path(foundKey.getChannel())
                .path(foundKey.getContentKey().toUrl())
                .queryParam("tag", tag)
                .queryParam("stable", stable)
                .build();
        String channelUri = uriInfo.getBaseUri() + "channel/" + foundKey.getChannel();
        logger.trace("returning url {}", uri);
        builder.location(uri);
        return builder.build();

    }

}
