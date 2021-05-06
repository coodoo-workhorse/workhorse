package io.coodoo.workhorse.saas;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import io.coodoo.workhorse.core.boundary.WorkhorseService;

/**
 * @author coodoo GmbH (coodoo.io) Automatic start of the server
 */
@ApplicationScoped
public class StartUp {

    @Inject
    WorkhorseService workhorseService;

    public void startUp(@Observes @Initialized(ApplicationScoped.class) Object o) {
        workhorseService.init();
    }
}
