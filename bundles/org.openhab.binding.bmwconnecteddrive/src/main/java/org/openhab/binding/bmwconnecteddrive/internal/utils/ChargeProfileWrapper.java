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
package org.openhab.binding.bmwconnecteddrive.internal.utils;

import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.ChargeProfile;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.WeeklyPlanner;

/**
 * The {@link ChargeProfileWrapper} Wrapper for ChargeProfiles
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class ChargeProfileWrapper {

    public static int WEEKLY = 0;
    public static int TWO_TIMES = 1;
    public static int EMPTY = -1;

    private int type = EMPTY;
    private ChargeProfile chargeProfile;
    private WeeklyPlanner planner;
    // private List<String> timer1Days = new ArrayList<String>(Arrays.asList("Not suppoerted"));
    // private List<String> timer2Days = new ArrayList<String>(Arrays.asList("Not suppoerted"));
    // private List<String> timer3Days = new ArrayList<String>(Arrays.asList("Not suppoerted"));

    private HashMap<String, Integer> timers = new HashMap<String, Integer>();

    public int getType() {
        return type;
    }

    public ChargeProfileWrapper(String content) {
        chargeProfile = Converter.getGson().fromJson(content, ChargeProfile.class);

        if (chargeProfile.weeklyPlanner != null) {
            type = WEEKLY;
            planner = chargeProfile.weeklyPlanner;
        } else if (chargeProfile.twoTimesTimer != null) {
            type = TWO_TIMES;
            planner = chargeProfile.twoTimesTimer;
            // timer days not supported
        } else {
            type = EMPTY;
            planner = new WeeklyPlanner();
        }

        if (type != EMPTY) {
            String[] chargeWindowStartSplit = planner.preferredChargingWindow.startTime.split(Constants.COLON);
            timers.put(ConnectedDriveConstants.CHARGE_WINDOW_START_HOUR, Integer.parseInt(chargeWindowStartSplit[0]));
            timers.put(ConnectedDriveConstants.CHARGE_WINDOW_START_MINUTE, Integer.parseInt(chargeWindowStartSplit[1]));
            String[] chargeWindowEndSplit = planner.preferredChargingWindow.endTime.split(Constants.COLON);
            timers.put(ConnectedDriveConstants.CHARGE_WINDOW_END_HOUR, Integer.parseInt(chargeWindowEndSplit[0]));
            timers.put(ConnectedDriveConstants.CHARGE_WINDOW_END_MINUTE, Integer.parseInt(chargeWindowEndSplit[1]));
            String[] timer1DepartureSplit = planner.timer1.departureTime.split(Constants.COLON);
            timers.put(ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_HOUR, Integer.parseInt(timer1DepartureSplit[0]));
            timers.put(ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_MINUTE,
                    Integer.parseInt(timer1DepartureSplit[1]));
            String[] timer2DepartureSplit = planner.timer2.departureTime.split(Constants.COLON);
            timers.put(ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_HOUR, Integer.parseInt(timer2DepartureSplit[0]));
            timers.put(ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_MINUTE,
                    Integer.parseInt(timer2DepartureSplit[1]));
            if (type == WEEKLY) {
                String[] timer3DepartureSplit = planner.timer3.departureTime.split(Constants.COLON);
                timers.put(ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_HOUR,
                        Integer.parseInt(timer3DepartureSplit[0]));
                timers.put(ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_MINUTE,
                        Integer.parseInt(timer3DepartureSplit[1]));
                String[] singleDepartureSplit = planner.overrideTimer.departureTime.split(Constants.COLON);
                timers.put(ConnectedDriveConstants.CHARGE_SINGLE_DEPARTURE_HOUR,
                        Integer.parseInt(singleDepartureSplit[0]));
                timers.put(ConnectedDriveConstants.CHARGE_SINGLE_DEPARTURE_MINUTE,
                        Integer.parseInt(singleDepartureSplit[1]));
            }
        }
    }

    public int getTime(String timerId) {
        if (timers.containsKey(timerId)) {
            if (timers.get(timerId) != null) {
                return timers.get(timerId);
            }

        }
        return -1;
    }

    public void setTime(String timerId, int t) {
        String time = String.format("%02d", t);
        String[] split;
        switch (timerId) {
            case ConnectedDriveConstants.CHARGE_WINDOW_START_HOUR:
                split = planner.preferredChargingWindow.startTime.split(Constants.COLON);
                planner.preferredChargingWindow.startTime = time + Constants.COLON + split[1];

                break;
            case ConnectedDriveConstants.CHARGE_WINDOW_START_MINUTE:
                split = planner.preferredChargingWindow.startTime.split(Constants.COLON);
                planner.preferredChargingWindow.startTime = split[0] + Constants.COLON + time;
                break;
            case ConnectedDriveConstants.CHARGE_WINDOW_END_HOUR:
                split = planner.preferredChargingWindow.endTime.split(Constants.COLON);
                planner.preferredChargingWindow.endTime = time + Constants.COLON + split[1];
                break;
            case ConnectedDriveConstants.CHARGE_WINDOW_END_MINUTE:
                split = planner.preferredChargingWindow.endTime.split(Constants.COLON);
                planner.preferredChargingWindow.endTime = split[0] + Constants.COLON + time;
                break;
            case ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_HOUR:
                split = planner.timer1.departureTime.split(Constants.COLON);
                planner.timer1.departureTime = time + Constants.COLON + split[1];
                break;
            case ConnectedDriveConstants.CHARGE_TIMER1_DEPARTURE_MINUTE:
                split = planner.timer1.departureTime.split(Constants.COLON);
                planner.timer1.departureTime = split[0] + Constants.COLON + time;
                break;
            case ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_HOUR:
                split = planner.timer2.departureTime.split(Constants.COLON);
                planner.timer2.departureTime = time + Constants.COLON + split[1];
                break;
            case ConnectedDriveConstants.CHARGE_TIMER2_DEPARTURE_MINUTE:
                split = planner.timer2.departureTime.split(Constants.COLON);
                planner.timer2.departureTime = split[0] + Constants.COLON + time;
                break;
            case ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_HOUR:
                if (type == WEEKLY) {
                    split = planner.timer3.departureTime.split(Constants.COLON);
                    planner.timer3.departureTime = time + Constants.COLON + split[1];
                }
                break;
            case ConnectedDriveConstants.CHARGE_TIMER3_DEPARTURE_MINUTE:
                if (type == WEEKLY) {
                    split = planner.timer3.departureTime.split(Constants.COLON);
                    planner.timer3.departureTime = split[0] + Constants.COLON + time;
                }
            case ConnectedDriveConstants.CHARGE_SINGLE_DEPARTURE_HOUR:
                if (type == WEEKLY) {
                    split = planner.overrideTimer.departureTime.split(Constants.COLON);
                    planner.overrideTimer.departureTime = time + Constants.COLON + split[1];
                }
                break;
            case ConnectedDriveConstants.CHARGE_SINGLE_DEPARTURE_MINUTE:
                if (type == WEEKLY) {
                    split = planner.overrideTimer.departureTime.split(Constants.COLON);
                    planner.overrideTimer.departureTime = split[0] + Constants.COLON + time;
                }
                break;
        }
    }

    public boolean isDaySelected(int timerId, String day) {
        if (type == WEEKLY) {
            switch (timerId) {
                case 1:
                    return planner.timer1.weekdays.contains(day);
                case 2:
                    return planner.timer2.weekdays.contains(day);
                case 3:
                    return planner.timer3.weekdays.contains(day);
            }
        }
        return false;
    }

    public void daySelection(int timerId, String day, OnOffType onOff) {
        if (type == WEEKLY) {
            switch (onOff) {
                case ON:
                    switch (timerId) {
                        case 1:
                            if (!planner.timer1.weekdays.contains(day)) {
                                planner.timer1.weekdays.add(day);
                            }
                            break;
                        case 2:
                            if (!planner.timer2.weekdays.contains(day)) {
                                planner.timer2.weekdays.add(day);
                            }
                            break;
                        case 3:
                            if (!planner.timer3.weekdays.contains(day)) {
                                planner.timer3.weekdays.add(day);
                            }
                            break;
                    }
                    break;
                case OFF:
                    switch (timerId) {
                        case 1:
                            if (planner.timer1.weekdays.contains(day)) {
                                planner.timer1.weekdays.remove(day);
                            }
                            break;
                        case 2:
                            if (planner.timer2.weekdays.contains(day)) {
                                planner.timer2.weekdays.remove(day);
                            }
                            break;
                        case 3:
                            if (planner.timer3.weekdays.contains(day)) {
                                planner.timer3.weekdays.remove(day);
                            }
                            break;
                    }
                    break;
            }
        }
    }

    public boolean isTimerEnabled(int timerId) {
        switch (timerId) {
            case 1:
                return planner.timer1.timerEnabled;
            case 2:
                return planner.timer2.timerEnabled;
            case 3:
                if (type == WEEKLY) {
                    return planner.timer3.timerEnabled;
                }
            default:
        }
        return false;
    }

    public void enableDisableTimer(int timerId, boolean state) {
        switch (timerId) {
            case 1:
                planner.timer1.timerEnabled = state;
            case 2:
                planner.timer2.timerEnabled = state;
            case 3:
                if (type == WEEKLY) {
                    planner.timer3.timerEnabled = state;
                }
            default:
        }
    }

    public String getJson() {
        return Converter.getGson().toJson(chargeProfile);
    }

    public boolean isClimatizationEnabled() {
        return planner.climatizationEnabled;
    }

    public void enableDisableClimate(boolean b) {
        planner.climatizationEnabled = b;
    }

    public @Nullable String getChargingMode() {
        return planner.chargingMode;
    }

    public void setChargeMode(String fullString) {
        planner.chargingMode = fullString;
    }

    public void setChargePreferences(String fullString) {
        planner.chargingPreferences = fullString;
    }

    public @Nullable String getChargingPreferences() {
        return planner.chargingPreferences;
    }
}
