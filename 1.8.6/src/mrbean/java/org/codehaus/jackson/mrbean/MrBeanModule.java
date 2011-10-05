package org.codehaus.jackson.mrbean;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.Module;
import org.codehaus.jackson.mrbean.AbstractTypeMaterializer;

public class MrBeanModule extends Module
{
    private final String NAME = "MrBeanModule";

    // TODO: externalize
    private final static Version VERSION = new Version(1, 8, 0, null);

    /**
     * Configured materializer instance to register with deserializer factory.
     */
    protected AbstractTypeMaterializer _materializer;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    public MrBeanModule() {
        this(new AbstractTypeMaterializer());
    }

    public MrBeanModule(AbstractTypeMaterializer materializer) {
        _materializer = materializer;
    }

    @Override public String getModuleName() { return NAME; }
    @Override public Version version() { return VERSION; }
    
    @Override
    public void setupModule(SetupContext context)
    {
        // All we really need to for now is to register materializer:
        context.addAbstractTypeResolver(_materializer);
    }

    /*
    /**********************************************************
    /* Extended API, configuration
    /**********************************************************
     */
}
