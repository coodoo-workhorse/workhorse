package io.coodoo.workhorse.persistence.interfaces.listing;

import javax.enterprise.context.ApplicationScoped;

/**
 * Listing configuration
 * 
 * @author coodoo GmbH (coodoo.io)
 */
@ApplicationScoped
public class ListingConfig {

    /**
     * Default index for pagination
     */
    public static int DEFAULT_INDEX = 0;

    /**
     * Default current page number for pagination
     */
    public static int DEFAULT_PAGE = 1;

    /**
     * Default limit of results per page for pagination
     */
    public static int DEFAULT_LIMIT = 10;

    private ListingConfig() {}

}
