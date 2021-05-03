package io.coodoo.workhorse.api;

import java.time.ZoneId;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.coodoo.workhorse.api.dto.TimeZonesDTO;
import io.coodoo.workhorse.api.dto.UpdateWorkhorseConfigDTO;
import io.coodoo.workhorse.core.boundary.WorkhorseService;
import io.coodoo.workhorse.core.entity.WorkhorseConfig;

/**
 * @author coodoo GmbH (coodoo.io)
 */
@Path("/workhorse/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkhorseConfigRessource {

    @Inject
    WorkhorseService workhorseService;

    @GET
    @Path("/")
    public WorkhorseConfig get() {
        return workhorseService.getWorkhorseConfig();
    }

    @PUT
    @Path("/")
    public WorkhorseConfig update(UpdateWorkhorseConfigDTO config) {
        workhorseService.updateWorkhorseConfig(config.timeZone, config.bufferMax, config.bufferMin, config.bufferPollInterval,
                        config.bufferPushFallbackPollInterval, config.minutesUntilCleanup, config.executionTimeout, config.executionTimeoutStatus,
                        config.logChange, config.logTimeFormat, config.logInfoMarker, config.logWarnMarker, config.logErrorMarker);

        return workhorseService.getWorkhorseConfig();
    }

    @GET
    @Path("/timezones")
    public TimeZonesDTO getTimeZones() {
        TimeZonesDTO timeZonesDTO = new TimeZonesDTO();
        timeZonesDTO.setSystemDefaultTimeZone(ZoneId.systemDefault().getId());
        timeZonesDTO.setTimeZones(ZoneId.getAvailableZoneIds().stream().sorted().collect(Collectors.toList()));
        return timeZonesDTO;
    }

}
