package tz.go.moh.him.hfr.mediator.mock;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.openhim.mediator.engine.messages.MediatorHTTPRequest;
import org.openhim.mediator.engine.testing.MockHTTPConnector;
import tz.go.moh.him.hfr.mediator.domain.HfrRequest;
import tz.go.moh.him.hfr.mediator.domain.HrhisMessage;
import tz.go.moh.him.hfr.mediator.orchestrator.HrhisOrchestratorTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a mock destination.
 */
public class MockHrhis extends MockHTTPConnector {

    /**
     * Initializes a new instance of the {@link MockHrhis} class.
     */
    public MockHrhis() {
    }

    /**
     * Gets the response.
     *
     * @return Returns the response.
     */
    @Override
    public String getResponse() {
        return null;
    }

    /**
     * Gets the status code.
     *
     * @return Returns the status code.
     */
    @Override
    public Integer getStatus() {
        return 200;
    }

    /**
     * Gets the HTTP headers.
     *
     * @return Returns the HTTP headers.
     */
    @Override
    public Map<String, String> getHeaders() {
        return Collections.emptyMap();
    }

    /**
     * Handles the message.
     *
     * @param msg The message.
     */
    @Override
    public void executeOnReceive(MediatorHTTPRequest msg) {

        InputStream stream = HrhisOrchestratorTest.class.getClassLoader().getResourceAsStream("request.json");

        Assert.assertNotNull(stream);

        Gson gson = new Gson();

        HfrRequest expected;

        try {
            expected = gson.fromJson(IOUtils.toString(stream), HfrRequest.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        HrhisMessage actual = gson.fromJson(msg.getBody(), HrhisMessage.class);

        Assert.assertNotNull(actual);
        Assert.assertNotNull(expected);

        Assert.assertEquals(expected.getName() + " " + expected.getFacilityType(), actual.getName());
        Assert.assertEquals(expected.getFacilityIdNumber(), actual.getCode());
        Assert.assertEquals(expected.getName(), actual.getShortName());
    }
}
