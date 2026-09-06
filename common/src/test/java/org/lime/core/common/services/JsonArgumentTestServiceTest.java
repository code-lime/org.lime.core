package org.lime.core.common.services;

import org.junit.jupiter.api.Test;
import org.lime.core.common.Artifact;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonArgumentTestServiceTest {
    @Test
    void skipsJsonArgumentCreationOnVelocity() {
        JsonArgumentTestService service = new JsonArgumentTestService();
        service.artifact = Artifact.VELOCITY;

        assertTrue(service.command().isEmpty());
    }
}
