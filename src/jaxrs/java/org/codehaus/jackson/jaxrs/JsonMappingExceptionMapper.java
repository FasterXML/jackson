package org.codehaus.jackson.jaxrs;

import org.codehaus.jackson.map.JsonMappingException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Implementation if {@link ExceptionMapper} to send down a "400 Bad Request"
 * response in the event that unmappable JSON is received.
 */
@Provider
public class JsonMappingExceptionMapper implements ExceptionMapper<JsonMappingException> {
    // only compiles on 1.6:
    //@Override
    public Response toResponse(JsonMappingException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type("text/plain").build();
    }
}
