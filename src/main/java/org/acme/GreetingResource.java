package org.acme;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/merchant")
public class GreetingResource {

    @Inject
    MeterRegistry meterRegistry;

    @GET
    public Response hello() throws InterruptedException {
        meterRegistry.counter("Lucas2").count();

        Thread.sleep(1000);
        return Response.accepted("Hello from Quarkus REST").build();
    }



}
