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
package org.openhab.binding.bmwconnecteddrive.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link ConnectedDriveConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Bernd Weymann - Initial contribution
 */
@SuppressWarnings("serial")
@NonNullByDefault
public class ConnectedDriveConstants {

    private static final String BINDING_ID = "bmwconnecteddrive";

    // Units
    public static final String UNITS_AUTODETECT = "AUTODETECT";
    public static final String UNITS_IMPERIAL = "IMPERIAL";
    public static final String UNITS_METRIC = "METRIC";

    public static final int DEFAULT_IMAGE_SIZE = 1024;
    public static final int DEFAULT_REFRESH_INTERVAL = 5;
    public static final String DEFAULT_IMAGE_VIEWPORT = "FRONT";

    // See constants from bimmer-connected
    // https://github.com/bimmerconnected/bimmer_connected/blob/master/bimmer_connected/vehicle.py
    public enum VehicleType {
        CONVENTIONAL("CONV"),
        PLUGIN_HYBRID("PHEV"),
        ELECTRIC_REX("BEV_REX"),
        ELECTRIC("BEV");

        private final String type;

        VehicleType(String s) {
            type = s;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    public static final Set<String> FUEL_VEHICLES = new HashSet<String>() {
        {
            add(VehicleType.CONVENTIONAL.toString());
            add(VehicleType.PLUGIN_HYBRID.toString());
            add(VehicleType.ELECTRIC_REX.toString());
        }
    };
    public static final Set<String> ELECTRIC_VEHICLES = new HashSet<String>() {
        {
            add(VehicleType.ELECTRIC.toString());
            add(VehicleType.PLUGIN_HYBRID.toString());
            add(VehicleType.ELECTRIC_REX.toString());
        }
    };

    // Countries with Mileage display
    public static final Set<String> IMPERIAL_COUNTRIES = new HashSet<String>() {
        {
            add("US");
            add("GB");
        }
    };

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_CONNECTED_DRIVE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_CONV = new ThingTypeUID(BINDING_ID,
            VehicleType.CONVENTIONAL.toString());
    public static final ThingTypeUID THING_TYPE_PHEV = new ThingTypeUID(BINDING_ID,
            VehicleType.PLUGIN_HYBRID.toString());
    public static final ThingTypeUID THING_TYPE_BEV_REX = new ThingTypeUID(BINDING_ID,
            VehicleType.ELECTRIC_REX.toString());
    public static final ThingTypeUID THING_TYPE_BEV = new ThingTypeUID(BINDING_ID, VehicleType.ELECTRIC.toString());
    public static final Set<ThingTypeUID> SUPPORTED_THING_SET = new HashSet<ThingTypeUID>() {
        {
            add(THING_TYPE_CONNECTED_DRIVE_ACCOUNT);
            add(THING_TYPE_CONV);
            add(THING_TYPE_PHEV);
            add(THING_TYPE_BEV_REX);
            add(THING_TYPE_BEV);
        }
    };

    // Thing Group definitions
    public static final String CHANNEL_GROUP_STATUS = "status";
    public static final String CHANNEL_GROUP_SERVICE = "service";
    public static final String CHANNEL_GROUP_CHECK_CONTROL = "check";
    public static final String CHANNEL_GROUP_DOORS = "doors";
    public static final String CHANNEL_GROUP_RANGE = "range";
    public static final String CHANNEL_GROUP_LOCATION = "location";
    public static final String CHANNEL_GROUP_LAST_TRIP = "last-trip";
    public static final String CHANNEL_GROUP_LIFETIME = "lifetime";
    public static final String CHANNEL_GROUP_REMOTE = "remote";
    public static final String CHANNEL_GROUP_CHARGE = "charge";
    public static final String CHANNEL_GROUP_VEHICLE_IMAGE = "image";
    public static final String CHANNEL_GROUP_DESTINATION = "destination";
    public static final String CHANNEL_GROUP_TROUBLESHOOT = "troubleshoot";

    // List Interface Constants
    public static final String SIZE = "size";
    public static final String INDEX = "index";
    public static final String NEXT = "next";

    // Generic Constants for several groups
    public static final String NAME = "name";
    public static final String DATE = "date";
    public static final String MILEAGE = "mileage";
    public static final String LONGITUDE = "longitude";
    public static final String LATITUDE = "latitude";
    public static final String HEADING = "heading";

    // Status
    public static final String DOORS = "doors";
    public static final String WINDOWS = "windows";
    public static final String LOCK = "lock";
    public static final String SERVICE_DATE = "service-date";
    public static final String SERVICE_MILEAGE = "service-mileage";
    public static final String CHECK_CONTROL = "check-control";
    public static final String CHARGE_STATUS = "charge";
    public static final String LAST_UPDATE = "last-update";

    // Door Details
    public static final String DOOR_DRIVER_FRONT = "driver-front";
    public static final String DOOR_DRIVER_REAR = "driver-rear";
    public static final String DOOR_PASSENGER_FRONT = "passenger-front";
    public static final String DOOR_PASSENGER_REAR = "passenger-rear";
    public static final String HOOD = "hood";
    public static final String TRUNK = "trunk";
    public static final String WINDOW_DOOR_DRIVER_FORNT = "window-driver-front";
    public static final String WINDOW_DOOR_DRIVER_REAR = "window-driver-rear";
    public static final String WINDOW_DOOR_PASSENGER_FRONT = "window-passenger-front";
    public static final String WINDOW_DOOR_PASSENGER_REAR = "window-passenger-rear";
    public static final String WINDOW_REAR = "window-rear";
    public static final String SUNROOF = "sunroof";

    // Charge Profile
    public static final String CHARGE_PROFILE_CLIMATE = "profile-climate";
    public static final String CHARGE_PROFILE_MODE = "profile-mode";
    public static final String CHARGE_PROFILE_PREFS = "profile-prefs";
    public static final String CHARGE_WINDOW_START_HOUR = "window-start-hour";
    public static final String CHARGE_WINDOW_START_MINUTE = "window-start-minute";
    public static final String CHARGE_WINDOW_END_HOUR = "window-end-hour";
    public static final String CHARGE_WINDOW_END_MINUTE = "window-end-minute";
    public static final String CHARGE_TIMER1_DEPARTURE_HOUR = "timer1-departure-hour";
    public static final String CHARGE_TIMER1_DEPARTURE_MINUTE = "timer1-departure-minute";
    public static final String CHARGE_TIMER1_DAY_MON = "timer1-day-mon";
    public static final String CHARGE_TIMER1_DAY_TUE = "timer1-day-tue";
    public static final String CHARGE_TIMER1_DAY_WED = "timer1-day-wed";
    public static final String CHARGE_TIMER1_DAY_THU = "timer1-day-thu";
    public static final String CHARGE_TIMER1_DAY_FRI = "timer1-day-fri";
    public static final String CHARGE_TIMER1_DAY_SAT = "timer1-day-sat";
    public static final String CHARGE_TIMER1_DAY_SUN = "timer1-day-sun";
    public static final String CHARGE_TIMER1_ENABLED = "timer1-enabled";
    public static final String CHARGE_TIMER2_DEPARTURE_HOUR = "timer2-departure-hour";
    public static final String CHARGE_TIMER2_DEPARTURE_MINUTE = "timer2-departure-minute";
    public static final String CHARGE_TIMER2_DAY_MON = "timer2-day-mon";
    public static final String CHARGE_TIMER2_DAY_TUE = "timer2-daytue";
    public static final String CHARGE_TIMER2_DAY_WED = "timer2-day-wed";
    public static final String CHARGE_TIMER2_DAY_THU = "timer2-day-thu";
    public static final String CHARGE_TIMER2_DAY_FRI = "timer2-day-fri";
    public static final String CHARGE_TIMER2_DAY_SAT = "timer2-day-sat";
    public static final String CHARGE_TIMER2_DAY_SUN = "timer2-day-sun";
    public static final String CHARGE_TIMER2_ENABLED = "timer2-enabled";
    public static final String CHARGE_TIMER3_DEPARTURE_HOUR = "timer3-departure-hour";
    public static final String CHARGE_TIMER3_DEPARTURE_MINUTE = "timer3-departure-minute";
    public static final String CHARGE_TIMER3_DAY_MON = "timer3-day-mon";
    public static final String CHARGE_TIMER3_DAY_TUE = "timer3-day-tue";
    public static final String CHARGE_TIMER3_DAY_WED = "timer3-day-wed";
    public static final String CHARGE_TIMER3_DAY_THU = "timer3-day-thu";
    public static final String CHARGE_TIMER3_DAY_FRI = "timer3-day-fri";
    public static final String CHARGE_TIMER3_DAY_SAT = "timer3-day-sat";
    public static final String CHARGE_TIMER3_DAY_SUN = "timer3-day-sun";
    public static final String CHARGE_TIMER3_ENABLED = "timer3-enabled";
    public static final String CHARGE_SINGLE_ENABLED = "single-enabled";
    public static final String CHARGE_SINGLE_DEPARTURE_HOUR = "single-departure-hour";
    public static final String CHARGE_SINGLE_DEPARTURE_MINUTE = "single-departure-minute";

    // Range
    public static final String RANGE_HYBRID = "hybrid";
    public static final String RANGE_ELECTRIC = "electric";
    public static final String SOC = "soc";
    public static final String RANGE_FUEL = "fuel";
    public static final String REMAINING_FUEL = "remaining-fuel";
    public static final String RANGE_RADIUS_ELECTRIC = "radius-electric";
    public static final String RANGE_RADIUS_FUEL = "radius-fuel";
    public static final String RANGE_RADIUS_HYBRID = "radius-hybrid";

    // Last Trip
    public static final String DURATION = "duration";
    public static final String DISTANCE = "distance";
    public static final String DISTANCE_SINCE_CHARGING = "distance-since-charging";
    public static final String AVG_CONSUMPTION = "average-consumption";
    public static final String AVG_COMBINED_CONSUMPTION = "average-combined-consumption";
    public static final String AVG_RECUPERATION = "average-recuperation";

    // Lifetime + Average Consumptions
    public static final String CUMULATED_DRIVEN_DISTANCE = "cumulated-driven-distance";
    public static final String SINGLE_LONGEST_DISTANCE = "single-longest-distance";

    // Image
    public static final String IMAGE_FORMAT = "png";
    public static final String IMAGE_VIEWPORT = "view";
    public static final String IMAGE_SIZE = "size";

    // Remote Services
    public static final String REMOTE_SERVICE_LIGHT_FLASH = "light";
    public static final String REMOTE_SERVICE_VEHICLE_FINDER = "finder";
    public static final String REMOTE_SERVICE_DOOR_LOCK = "lock";
    public static final String REMOTE_SERVICE_DOOR_UNLOCK = "unlock";
    public static final String REMOTE_SERVICE_HORN = "horn";
    public static final String REMOTE_SERVICE_AIR_CONDITIONING = "climate";
    public static final String REMOTE_SERVICE_CHARGE_NOW = "charge-now";
    public static final String REMOTE_SERVICE_CHARGING_CONTROL = "charge-control";
    public static final String REMOTE_STATE = "state";

    // Troubleshoot
    public static final String DISCOVERY_FINGERPRINT = "discovery-fingerprint";
    public static final String VEHICLE_FINGERPRINT = "vehicle-fingerprint";
}
