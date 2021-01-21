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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.junit.Test;
import org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants;
import org.openhab.binding.bmwconnecteddrive.internal.util.FileReader;
import org.openhab.binding.bmwconnecteddrive.internal.utils.ChargeProfileWrapper;
import org.openhab.binding.bmwconnecteddrive.internal.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ChargeProfileWrapperTest} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class ChargeProfileWrapperTest {
    private final Logger logger = LoggerFactory.getLogger(VehicleHandler.class);

    @Test
    public void testWeeklyPlanner() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        String content = FileReader.readFileInString("src/test/resources/webapi/charging-profile.json");
        ChargeProfileWrapper cpw = new ChargeProfileWrapper(content);
        fullTest(cpw);
    }

    @Test
    public void testHybridChargingProfile() {
        logger.info("{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        String content = FileReader.readFileInString("src/test/resources/responses/XE225/charge-profile.json");
        ChargeProfileWrapper cpw = new ChargeProfileWrapper(content);
        fullTest(cpw);
    }

    private void fullTest(ChargeProfileWrapper cpw) {
        cpw.setTime(ConnectedDriveConstants.CHARGE_WINDOW_START_HOUR, 1);
        cpw.setTime(ConnectedDriveConstants.CHARGE_WINDOW_START_MINUTE, 2);
        cpw.setTime(ConnectedDriveConstants.CHARGE_WINDOW_END_HOUR, 3);
        cpw.setTime(ConnectedDriveConstants.CHARGE_WINDOW_END_MINUTE, 4);

        cpw.daySelection(1, Constants.MONDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.TUESDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.WEDNESDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.THURSDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.FRIDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.SATURDAY, OnOffType.OFF);
        cpw.daySelection(1, Constants.SUNDAY, OnOffType.OFF);
        cpw.enableDisableTimer(1, false);

        cpw.daySelection(2, Constants.MONDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.TUESDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.WEDNESDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.THURSDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.FRIDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.SATURDAY, OnOffType.OFF);
        cpw.daySelection(2, Constants.SUNDAY, OnOffType.OFF);
        cpw.enableDisableTimer(2, false);

        cpw.daySelection(3, Constants.MONDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.TUESDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.WEDNESDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.THURSDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.FRIDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.SATURDAY, OnOffType.OFF);
        cpw.daySelection(3, Constants.SUNDAY, OnOffType.OFF);
        cpw.enableDisableTimer(3, false);
        System.out.println(cpw.getJson());

        cpw.daySelection(1, Constants.MONDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.TUESDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.WEDNESDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.THURSDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.FRIDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.SATURDAY, OnOffType.ON);
        cpw.daySelection(1, Constants.SUNDAY, OnOffType.ON);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_HOUR, 5);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_MINUTE, 6);
        cpw.enableDisableTimer(1, true);

        cpw.daySelection(2, Constants.MONDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.TUESDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.WEDNESDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.THURSDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.FRIDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.SATURDAY, OnOffType.ON);
        cpw.daySelection(2, Constants.SUNDAY, OnOffType.ON);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_HOUR, 7);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_MINUTE, 8);
        cpw.enableDisableTimer(2, true);

        cpw.daySelection(3, Constants.MONDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.TUESDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.WEDNESDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.THURSDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.FRIDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.SATURDAY, OnOffType.ON);
        cpw.daySelection(3, Constants.SUNDAY, OnOffType.ON);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_HOUR, 9);
        cpw.setTime(ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_MINUTE, 10);
        cpw.enableDisableTimer(3, true);
        System.out.println(cpw.getJson());
    }
}
