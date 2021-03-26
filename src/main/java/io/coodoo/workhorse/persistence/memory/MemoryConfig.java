package io.coodoo.workhorse.persistence.memory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.coodoo.workhorse.core.entity.WorkhorseConfig;

/**
 * A class to access the {@link WorkhorseConfig} configurations defined by the Memory Persistence.
 * 
 * @author coodoo GmbH (coodoo.io)
 */
public class MemoryConfig extends WorkhorseConfig {

    public static final String NAME = "MEMORY";

    private static String version = null;
    {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("version.txt");
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            version = reader.readLine();
            if (version == null) {
                version = "Unknown";
            } else {
                if (version.endsWith("SNAPSHOT")) {
                    String timestamp = reader.readLine();
                    if (timestamp != null) {
                        version += " (" + timestamp + ")";
                    }
                }
            }
        } catch (IOException e) {
            version = "Unknown (" + e.getMessage() + ")";
        }
    }

    @Override
    public String getPersistenceName() {
        return NAME;
    }

    @Override
    public String getPersistenceVersion() {
        return version;
    }
}
