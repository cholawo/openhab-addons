/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.bmwconnecteddrive.internal.handler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerCallback;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.CarType;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.StatusWrapper;
import org.openhab.binding.bmwconnecteddrive.internal.util.FileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link CarTests} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class CarTests {
    private final Logger logger = LoggerFactory.getLogger(ConnectedCarHandler.class);
    // ConnectedCarHandler cch = new Co

    @Nullable
    ArgumentCaptor<ChannelUID> channelCaptor;
    @Nullable
    ArgumentCaptor<State> stateCaptor;
    @Nullable
    ThingHandlerCallback tc;
    @Nullable
    ConnectedCarHandler cch;
    @Nullable
    List<ChannelUID> allChannels;
    @Nullable
    List<State> allStates;
    @Nullable
    String driveTrain;
    boolean imperial;

    /**
     * Prepare environment for Vehicle Status Updates
     */
    public void setup(String type, boolean imperial) {
        driveTrain = type;
        this.imperial = imperial;
        Thing thing = mock(Thing.class);
        when(thing.getUID()).thenReturn(new ThingUID("testbinding", "test"));
        HttpClient hc = mock(HttpClient.class);
        cch = new ConnectedCarHandler(thing, hc, type, imperial);
        tc = mock(ThingHandlerCallback.class);
        cch.setCallback(tc);
        channelCaptor = ArgumentCaptor.forClass(ChannelUID.class);
        stateCaptor = ArgumentCaptor.forClass(State.class);

    }

    private boolean testCar(@Nullable String statusContent, int callbacksExpected) {
        assertNotNull(statusContent);

        cch.vehicleStatusCallback.onResponse(Optional.of(statusContent));
        verify(tc, times(callbacksExpected)).stateUpdated(channelCaptor.capture(), stateCaptor.capture());
        allChannels = channelCaptor.getAllValues();
        allStates = stateCaptor.getAllValues();

        assertNotNull(driveTrain);
        StatusWrapper checker = new StatusWrapper(driveTrain, imperial, statusContent);
        trace();
        return checker.checkResults(allChannels, allStates);
    }

    private void trace() {
        for (int i = 0; i < allChannels.size(); i++) {
            logger.info("Channel {} {}", allChannels.get(i), allStates.get(i));
        }
    }

    /**
     * Sequence for BEV_REX
     * Channel testbinding::test:status#lock Secured
     * Channel testbinding::test:status#doors Closed
     * Channel testbinding::test:status#windows Closed
     * Channel testbinding::test:status#check-control Ok
     * Channel testbinding::test:status#service Nov 2021 - Brake Fluid
     * Channel testbinding::test:status#charging-status Invalid
     * Channel testbinding::test:range#mileage 17273.0 km
     * Channel testbinding::test:range#remaining-range-electric 148.0 km
     * Channel testbinding::test:range#remaining-range-fuel 70.0 km
     * Channel testbinding::test:range#remaining-range-hybrid 218.0 km
     * Channel testbinding::test:location#range-radius 218000.0
     * Channel testbinding::test:range#remaining-soc 71.0 %
     * Channel testbinding::test:range#remaining-fuel 4.0 l
     * Channel testbinding::test:range#last-update 24.08.2020 17:55
     * Channel testbinding::test:location#latitude 50.55604934692383
     * Channel testbinding::test:location#longitude 8.4956693649292
     * Channel testbinding::test:location#latlong 50.55605,8.495669
     * Channel testbinding::test:location#heading 219.0 °
     */
    @Test
    public void testMyi3Rex() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC_REX.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/webapi/vehicle-status.json");
        assertTrue(testCar(content, 18));
    }

    /**
     * Channel testbinding::test:status#lock Secured
     * Channel testbinding::test:status#doors Closed
     * Channel testbinding::test:status#windows Closed
     * Channel testbinding::test:status#check-control Ok
     * Channel testbinding::test:status#service Nov 2021 - Brake Fluid
     * Channel testbinding::test:status#charging-status Invalid
     * Channel testbinding::test:range#mileage 17273.0 mi
     * Channel testbinding::test:range#remaining-range-electric 91.0 mi
     * Channel testbinding::test:range#remaining-range-fuel 43.0 mi
     * Channel testbinding::test:range#remaining-range-hybrid 134.0 mi
     * Channel testbinding::test:location#range-radius 707520.0
     * Channel testbinding::test:range#remaining-soc 71.0 %
     * Channel testbinding::test:range#remaining-fuel 4.0 l
     * Channel testbinding::test:range#last-update 24.08.2020 17:55
     * Channel testbinding::test:location#latitude 50.55604934692383
     * Channel testbinding::test:location#longitude 8.4956693649292
     * Channel testbinding::test:location#latlong 50.55605,8.495669
     * Channel testbinding::test:location#heading 219.0 °
     */
    @Test
    public void testMyi3RexMiles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC_REX.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/webapi/vehicle-status.json");
        assertTrue(testCar(content, 18));
    }

    /**
     * Channel testbinding::test:status#lock Unlocked
     * Channel testbinding::test:status#doors Closed
     * Channel testbinding::test:status#windows Closed
     * Channel testbinding::test:status#check-control Ok
     * Channel testbinding::test:status#service Jun 2018 or in 12000 km - Oil
     * Channel testbinding::test:range#mileage 1629.0 km
     * Channel testbinding::test:range#remaining-range-fuel 249.0 km
     * Channel testbinding::test:location#range-radius 249000.0
     * Channel testbinding::test:range#remaining-fuel 30.0 l
     * Channel testbinding::test:range#last-update 09.03.2018 04:21
     * Channel testbinding::test:location#latitude 123.12300109863281
     * Channel testbinding::test:location#longitude -123.12300109863281
     * Channel testbinding::test:location#latlong 123.123,-123.123
     * Channel testbinding::test:location#heading 11.0 °
     */
    @Test
    public void testF15() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/F15/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF15Miles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/F15/status.json");
        assertTrue(testCar(content, 14));
    }

    /**
     * Channel testbinding::test:status#lock Unkown
     * Channel testbinding::test:status#doors Unkown
     * Channel testbinding::test:status#windows Unkown
     * Channel testbinding::test:status#check-control Unkown
     * Channel testbinding::test:status#service Unkown
     * Channel testbinding::test:range#mileage 0.0 km
     * Channel testbinding::test:range#remaining-range-fuel 0.0 km
     * Channel testbinding::test:location#range-radius 0.0
     * Channel testbinding::test:range#remaining-fuel 0.0 l
     * Channel testbinding::test:range#last-update Unkown
     * Channel testbinding::test:location#latitude 12.345600128173828
     * Channel testbinding::test:location#longitude 34.56779861450195
     * Channel testbinding::test:location#latlong 12.3456,34.5678
     * Channel testbinding::test:location#heading 0.0 ° @Test
     */
    @Test
    public void testF31() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/F31/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF31Miles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/F31/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF35() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/F35/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF35Miles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/F35/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF45() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/F45/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF45Miles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/F45/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF48() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/F48/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testF48Miles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/F48/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testG31NBTEvo() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/G31_NBTevo/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testG31NBTEvoMiles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.CONVENTIONAL.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/G31_NBTevo/status.json");
        assertTrue(testCar(content, 14));
    }

    @Test
    public void testI01NoRex() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/I01_NOREX/status.json");
        assertTrue(testCar(content, 15));
    }

    @Test
    public void testI01NoRexMiles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/I01_NOREX/status.json");
        assertTrue(testCar(content, 15));
    }

    @Test
    public void testI01Rex() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC_REX.toString(), false);
        String content = FileReader.readFileInString("src/test/resources/responses/I01_REX/status.json");
        assertTrue(testCar(content, 18));
    }

    @Test
    public void testI01RexMiles() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        setup(CarType.ELECTRIC_REX.toString(), true);
        String content = FileReader.readFileInString("src/test/resources/responses/I01_REX/status.json");
        assertTrue(testCar(content, 18));
    }
}
