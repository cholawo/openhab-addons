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
package org.openhab.binding.bmwconnecteddrive.internal.dto;

import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.VehicleType;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.Doors;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.VehicleStatus;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.VehicleStatusContainer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.Windows;
import org.openhab.binding.bmwconnecteddrive.internal.utils.Constants;
import org.openhab.binding.bmwconnecteddrive.internal.utils.Converter;
import org.openhab.binding.bmwconnecteddrive.internal.utils.VehicleStatusUtils;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.types.State;

import com.google.gson.Gson;

/**
 * The {@link StatusWrapper} Test json responses from ConnectedDrive Portal
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class StatusWrapper {
    private static final Gson GSON = new Gson();
    private static final Unit<Length> KILOMETRE = MetricPrefix.KILO(SIUnits.METRE);

    private VehicleStatus vStatus;
    private boolean imperial;
    private boolean isElectric;
    private boolean hasFuel;
    private boolean isHybrid;

    private Map<String, State> specialHandlingMap = new HashMap<String, State>();

    public StatusWrapper(String type, boolean imperial, String statusJson) {
        this.imperial = imperial;
        hasFuel = type.equals(VehicleType.CONVENTIONAL.toString()) || type.equals(VehicleType.PLUGIN_HYBRID.toString())
                || type.equals(VehicleType.ELECTRIC_REX.toString());
        isElectric = type.equals(VehicleType.PLUGIN_HYBRID.toString())
                || type.equals(VehicleType.ELECTRIC_REX.toString()) || type.equals(VehicleType.ELECTRIC.toString());
        isHybrid = hasFuel && isElectric;
        VehicleStatusContainer container = GSON.fromJson(statusJson, VehicleStatusContainer.class);
        assertNotNull(container);
        assertNotNull(container.vehicleStatus);
        vStatus = container.vehicleStatus;
    }

    /**
     * Test results auctomatically against json values
     *
     * @param channels
     * @param states
     * @return
     */
    public boolean checkResults(@Nullable List<ChannelUID> channels, @Nullable List<State> states) {
        assertNotNull(channels);
        assertNotNull(states);
        assertTrue(channels.size() == states.size(), "Same list sizes");
        for (int i = 0; i < channels.size(); i++) {
            checkResult(channels.get(i), states.get(i));
        }
        return true;
    }

    /**
     * Add a specific check for a value e.g. hard coded "Upcoming Service" in order to check the right ordering
     *
     * @param specialHand
     * @return
     */
    public StatusWrapper append(Map<String, State> compareMap) {
        specialHandlingMap.putAll(compareMap);
        return this;
    }

    private void checkResult(ChannelUID channelUID, State state) {
        String cUid = channelUID.getIdWithoutGroup();
        String gUid = channelUID.getGroupId();
        QuantityType<Length> qt;
        StringType st;
        StringType wanted;
        DateTimeType dtt;
        DecimalType dt;
        PointType pt;
        switch (cUid) {
            case MILEAGE:
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                if (imperial) {
                    assertEquals(ImperialUnits.MILE, qt.getUnit(), "Miles");
                } else {
                    assertEquals(KILOMETRE, qt.getUnit(), "KM");
                }
                switch (gUid) {
                    case CHANNEL_GROUP_RANGE:
                        assertEquals(qt.intValue(), vStatus.mileage, "Mileage");
                        break;
                    case CHANNEL_GROUP_SERVICE:
                        if (vStatus.cbsData.isEmpty()) {
                            assertEquals(qt.intValue(), -1, "Service Mileage");
                        } else {
                            assertEquals(qt.intValue(), vStatus.cbsData.get(0).cbsRemainingMileage, "Service Mileage");
                        }
                        break;
                    case CHANNEL_GROUP_CHECK_CONTROL:
                        if (vStatus.checkControlMessages.isEmpty()) {
                            assertEquals(qt.intValue(), -1, "CheckControl Mileage");
                        } else {
                            assertEquals(qt.intValue(), vStatus.checkControlMessages.get(0).ccmMileage,
                                    "CheckControl Mileage");
                        }
                        break;
                    default:
                        assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                        break;
                }
                break;
            case RANGE_ELECTRIC:
                assertTrue(isElectric, "Is Eelctric");
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                if (imperial) {
                    assertEquals(ImperialUnits.MILE, qt.getUnit(), "Miles");
                    assertEquals(Converter.round(qt.floatValue()), Converter.round(vStatus.remainingRangeElectricMls),
                            0.01, "Mileage");
                } else {
                    assertEquals(KILOMETRE, qt.getUnit(), "KM");
                    assertEquals(Converter.round(qt.floatValue()), Converter.round(vStatus.remainingRangeElectric),
                            0.01, "Mileage");
                }
                break;
            case RANGE_FUEL:
                assertTrue(hasFuel, "Has Fuel");
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                if (imperial) {
                    assertEquals(ImperialUnits.MILE, qt.getUnit(), "Miles");
                    assertEquals(Converter.round(qt.floatValue()), Converter.round(vStatus.remainingRangeFuelMls), 0.01,
                            "Mileage");
                } else {
                    assertEquals(KILOMETRE, qt.getUnit(), "KM");
                    assertEquals(Converter.round(qt.floatValue()), Converter.round(vStatus.remainingRangeFuel), 0.01,
                            "Mileage");
                }
                break;
            case RANGE_HYBRID:
                assertTrue(isHybrid, "Is Hybrid");
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                if (imperial) {
                    assertEquals(ImperialUnits.MILE, qt.getUnit(), "Miles");
                    assertEquals(Converter.round(qt.floatValue()),
                            Converter.round(vStatus.remainingRangeElectricMls + vStatus.remainingRangeFuelMls), 0.01,
                            "Mileage");
                } else {
                    assertEquals(KILOMETRE, qt.getUnit(), "KM");
                    assertEquals(Converter.round(qt.floatValue()),
                            Converter.round(vStatus.remainingRangeElectric + vStatus.remainingRangeFuel), 0.01,
                            "Mileage");
                }
                break;
            case REMAINING_FUEL:
                assertTrue(hasFuel, "Has Fuel");
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                assertEquals(Units.LITRE, qt.getUnit(), "Liter Unit");
                assertEquals(Converter.round(vStatus.remainingFuel), Converter.round(qt.floatValue()), 0.01,
                        "Fuel Level");
                break;
            case SOC:
                assertTrue(isElectric, "Is Eelctric");
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                assertEquals(Units.PERCENT, qt.getUnit(), "Percent");
                assertEquals(Converter.round(vStatus.chargingLevelHv), Converter.round(qt.floatValue()), 0.01,
                        "Charge Level");
                break;
            case LOCK:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                assertEquals(Converter.toTitleCase(vStatus.doorLockState), st.toString(), "Vehicle locked");
                break;
            case DOORS:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                Doors doorState = GSON.fromJson(GSON.toJson(vStatus), Doors.class);
                if (doorState != null) {
                    assertEquals(VehicleStatusUtils.checkClosed(doorState), st.toString(), "Doors Closed");
                } else {
                    assertTrue(false);
                }

                break;
            case WINDOWS:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                Windows windowState = GSON.fromJson(GSON.toJson(vStatus), Windows.class);
                if (windowState != null) {
                    if (specialHandlingMap.containsKey(WINDOWS)) {
                        assertEquals(specialHandlingMap.get(WINDOWS).toString(), st.toString(), "Windows");
                    } else {
                        assertEquals(VehicleStatusUtils.checkClosed(windowState), st.toString(), "Windows");
                    }
                } else {
                    assertTrue(false);
                }

                break;
            case CHECK_CONTROL:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                if (specialHandlingMap.containsKey(CHECK_CONTROL)) {
                    assertEquals(specialHandlingMap.get(CHECK_CONTROL).toString(), st.toString(), "Check Control");
                } else {
                    assertEquals(Converter.toTitleCase(VehicleStatusUtils.checkControlActive(vStatus)), st.toString(),
                            "Check Control");
                }
                break;
            case CHARGE_STATUS:
                assertTrue(isElectric, "Is Eelctric");
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                if (vStatus.chargingStatus.contentEquals(Constants.INVALID)) {
                    assertEquals(Converter.toTitleCase(vStatus.lastChargingEndReason), st.toString(), "Charge Status");
                } else {
                    assertEquals(Converter.toTitleCase(vStatus.chargingStatus), st.toString(), "Charge Status");
                }
                break;
            case LAST_UPDATE:
                assertTrue(state instanceof DateTimeType);
                dtt = (DateTimeType) state;
                DateTimeType expected = DateTimeType
                        .valueOf(Converter.getLocalDateTime(VehicleStatusUtils.getUpdateTime(vStatus)));
                assertEquals(expected.toString(), dtt.toString(), "Last Update");
                break;
            case GPS:
                assertTrue(state instanceof PointType);
                pt = (PointType) state;
                assertNotNull(vStatus.position);
                assertEquals(vStatus.position.getCoordinates(), pt.toString(), "Coordinates");
                break;
            case HEADING:
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                assertEquals(Units.DEGREE_ANGLE, qt.getUnit(), "Angle Unit");
                assertNotNull(vStatus.position);
                assertEquals(vStatus.position.heading, qt.intValue(), 0.01, "Heading");
                break;
            case RANGE_RADIUS_ELECTRIC:
                assertTrue(state instanceof QuantityType);
                assertTrue(isElectric);
                qt = (QuantityType) state;
                if (imperial) {
                    assertEquals(Converter.guessRangeRadius(vStatus.remainingRangeElectricMls), qt.floatValue(), 0.1,
                            "Range Radius Electric mi");
                } else {
                    assertEquals(Converter.guessRangeRadius(vStatus.remainingRangeElectric), qt.floatValue(), 0.1,
                            "Range Radius Electric km");
                }
                break;
            case RANGE_RADIUS_FUEL:
                assertTrue(state instanceof QuantityType);
                assertTrue(hasFuel);
                qt = (QuantityType) state;
                if (imperial) {
                    assertEquals(Converter.guessRangeRadius(vStatus.remainingRangeFuelMls), qt.floatValue(), 0.1,
                            "Range Radius Fuel mi");
                } else {
                    assertEquals(Converter.guessRangeRadius(vStatus.remainingRangeFuel), qt.floatValue(), 0.1,
                            "Range Radius Fuel km");
                }
                break;
            case RANGE_RADIUS_HYBRID:
                assertTrue(state instanceof QuantityType);
                assertTrue(isHybrid);
                qt = (QuantityType) state;
                if (imperial) {
                    assertEquals(
                            Converter.guessRangeRadius(
                                    vStatus.remainingRangeElectricMls + vStatus.remainingRangeFuelMls),
                            qt.floatValue(), 0.1, "Range Radius Hybrid mi");
                } else {
                    assertEquals(
                            Converter.guessRangeRadius(vStatus.remainingRangeElectric + vStatus.remainingRangeFuel),
                            qt.floatValue(), 0.1, "Range Radius Hybrid km");
                }
                break;
            case DOOR_DRIVER_FRONT:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.doorDriverFront);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case DOOR_DRIVER_REAR:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.doorDriverRear);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case DOOR_PASSENGER_FRONT:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.doorPassengerFront);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case DOOR_PASSENGER_REAR:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.doorPassengerRear);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case TRUNK:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.trunk);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case HOOD:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.hood);
                assertEquals(wanted.toString(), st.toString(), "Door");
                break;
            case WINDOW_DOOR_DRIVER_FORNT:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.windowDriverFront);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case WINDOW_DOOR_DRIVER_REAR:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.windowDriverRear);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case WINDOW_DOOR_PASSENGER_FRONT:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.windowPassengerFront);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case WINDOW_DOOR_PASSENGER_REAR:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.windowPassengerRear);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case WINDOW_REAR:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.rearWindow);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case SUNROOF:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                wanted = StringType.valueOf(vStatus.sunroof);
                assertEquals(wanted.toString(), st.toString(), "Window");
                break;
            case SIZE:
                assertTrue(state instanceof DecimalType);
                dt = (DecimalType) state;
                switch (gUid) {
                    case CHANNEL_GROUP_CHECK_CONTROL:
                        assertEquals(vStatus.checkControlMessages.size(), dt.intValue(),
                                "Number CheckControl Messages");
                        break;
                    case CHANNEL_GROUP_SERVICE:
                        assertEquals(vStatus.cbsData.size(), dt.intValue(), "Number of Services");
                        break;
                    default:
                        assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                        break;
                }
                break;
            case INDEX:
                assertTrue(state instanceof DecimalType);
                dt = (DecimalType) state;
                switch (gUid) {
                    case CHANNEL_GROUP_SERVICE:
                        if (vStatus.cbsData.isEmpty()) {
                            assertEquals(-1, dt.intValue(), "Index of Services");
                        } else {
                            assertEquals(0, dt.intValue(), "Index of Services");
                        }
                        break;
                    case CHANNEL_GROUP_CHECK_CONTROL:
                        if (vStatus.checkControlMessages.isEmpty()) {
                            assertEquals(-1, dt.intValue(), "Index of CheckControls");
                        } else {
                            assertEquals(0, dt.intValue(), "Index of CheckControls");
                        }
                        break;
                    default:
                        assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                        break;
                }
                break;
            case SERVICE_DATE:
                assertTrue(state instanceof DateTimeType);
                dtt = (DateTimeType) state;
                if (gUid.contentEquals(CHANNEL_GROUP_STATUS)) {
                    if (specialHandlingMap.containsKey(SERVICE_DATE)) {
                        assertEquals(specialHandlingMap.get(SERVICE_DATE).toString(), dtt.toString(), "Next Service");
                    } else {
                        String dueDateString = VehicleStatusUtils.getNextServiceDate(vStatus);
                        DateTimeType expectedDTT = DateTimeType.valueOf(Converter.getLocalDateTime(dueDateString));
                        assertEquals(expectedDTT.toString(), dtt.toString(), "Next Service");
                    }
                } else if (gUid.equals(CHANNEL_GROUP_SERVICE)) {
                    String dueDateString = vStatus.cbsData.get(0).getDueDate();
                    DateTimeType expectedDTT = DateTimeType.valueOf(Converter.getLocalDateTime(dueDateString));
                    assertEquals(expectedDTT.toString(), dtt.toString(), "First Service Date");
                }
                break;
            case SERVICE_MILEAGE:
                assertTrue(state instanceof QuantityType);
                qt = ((QuantityType) state);
                if (gUid.contentEquals(CHANNEL_GROUP_STATUS)) {
                    if (imperial) {
                        assertEquals(ImperialUnits.MILE, qt.getUnit(), "Next Service Miles");
                        assertEquals(VehicleStatusUtils.getNextServiceMileage(vStatus), qt.intValue(), "Mileage");
                    } else {
                        assertEquals(KILOMETRE, qt.getUnit(), "Next Service KM");
                        assertEquals(VehicleStatusUtils.getNextServiceMileage(vStatus), qt.intValue(), "Mileage");
                    }
                } else if (gUid.equals(CHANNEL_GROUP_SERVICE)) {
                    if (imperial) {
                        assertEquals(ImperialUnits.MILE, qt.getUnit(), "First Service Miles");
                        assertEquals(vStatus.cbsData.get(0).cbsRemainingMileage, qt.intValue(),
                                "First Service Mileage");
                    } else {
                        assertEquals(KILOMETRE, qt.getUnit(), "First Service KM");
                        assertEquals(vStatus.cbsData.get(0).cbsRemainingMileage, qt.intValue(),
                                "First Service Mileage");
                    }
                }
                break;
            case NAME:
                assertTrue(state instanceof StringType);
                st = (StringType) state;
                switch (gUid) {
                    case CHANNEL_GROUP_SERVICE:
                        wanted = StringType.valueOf(Constants.INVALID);
                        if (!vStatus.cbsData.isEmpty()) {
                            wanted = StringType.valueOf(Converter.toTitleCase(vStatus.cbsData.get(0).getType()));
                        }
                        assertEquals(wanted.toString(), st.toString(), "Service Name");
                        break;
                    case CHANNEL_GROUP_CHECK_CONTROL:
                        wanted = StringType.valueOf(Constants.INVALID);
                        if (!vStatus.checkControlMessages.isEmpty()) {
                            wanted = StringType.valueOf(vStatus.checkControlMessages.get(0).ccmDescriptionShort);
                        }
                        assertEquals(wanted.toString(), st.toString(), "CheckControl Name");
                        break;
                    default:
                        assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                        break;
                }
                break;
            case DATE:
                assertTrue(state instanceof DateTimeType);
                dtt = (DateTimeType) state;
                switch (gUid) {
                    case CHANNEL_GROUP_SERVICE:
                        String dueDateString = Constants.NULL_DATE;
                        if (!vStatus.cbsData.isEmpty()) {
                            dueDateString = vStatus.cbsData.get(0).getDueDate();
                        }
                        DateTimeType expectedDTT = DateTimeType.valueOf(Converter.getLocalDateTime(dueDateString));
                        assertEquals(expectedDTT.toString(), dtt.toString(), "ServiceSate");
                        break;
                    default:
                        assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                        break;
                }
                break;
            default:
                // fail in case of unknown update
                assertFalse(true, "Channel " + channelUID + " " + state + " not found");
                break;
        }
    }
}
