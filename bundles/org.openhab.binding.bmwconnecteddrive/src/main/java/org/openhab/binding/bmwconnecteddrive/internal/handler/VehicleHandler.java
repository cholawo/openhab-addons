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

import static org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.MetricPrefix;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.bmwconnecteddrive.internal.ConnectedDriveConstants.VehicleType;
import org.openhab.binding.bmwconnecteddrive.internal.VehicleConfiguration;
import org.openhab.binding.bmwconnecteddrive.internal.dto.DestinationContainer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.NetworkError;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.ChargeProfile;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.ChargingWindow;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.Timer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.charge.WeeklyPlanner;
import org.openhab.binding.bmwconnecteddrive.internal.dto.statistics.AllTrips;
import org.openhab.binding.bmwconnecteddrive.internal.dto.statistics.AllTripsContainer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.statistics.LastTrip;
import org.openhab.binding.bmwconnecteddrive.internal.dto.statistics.LastTripContainer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.CBSMessage;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.Doors;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.Position;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.VehicleStatus;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.VehicleStatusContainer;
import org.openhab.binding.bmwconnecteddrive.internal.dto.status.Windows;
import org.openhab.binding.bmwconnecteddrive.internal.handler.RemoteServiceHandler.ExecutionState;
import org.openhab.binding.bmwconnecteddrive.internal.handler.RemoteServiceHandler.RemoteService;
import org.openhab.binding.bmwconnecteddrive.internal.utils.Constants;
import org.openhab.binding.bmwconnecteddrive.internal.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link VehicleHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class VehicleHandler extends VehicleChannelHandler {
    private final Logger logger = LoggerFactory.getLogger(VehicleHandler.class);

    private Optional<ConnectedDriveProxy> proxy = Optional.empty();
    private Optional<RemoteServiceHandler> remote = Optional.empty();
    private Optional<VehicleConfiguration> configuration = Optional.empty();
    private Optional<ConnectedDriveBridgeHandler> bridgeHandler = Optional.empty();
    private Optional<ScheduledFuture<?>> refreshJob = Optional.empty();

    private boolean imperial = false;
    private boolean hasFuel = false;
    private boolean isElectric = false;
    private boolean isHybrid = false;

    private Position currentPosition = new Position();

    StringResponseCallback vehicleStatusCallback = new VehicleStatusCallback();
    StringResponseCallback lastTripCallback = new LastTripCallback();
    StringResponseCallback allTripsCallback = new AllTripsCallback();
    StringResponseCallback chargeProfileCallback = new ChargeProfilesCallback();
    StringResponseCallback rangeMapCallback = new RangeMapCallback();
    StringResponseCallback destinationCallback = new DestinationsCallback();
    ByteResponseCallback imageCallback = new ImageCallback();

    private Optional<String> vehicleStatusCache = Optional.empty();
    private Optional<String> lastTripCache = Optional.empty();
    private Optional<String> allTripsCache = Optional.empty();
    private Optional<String> chargeProfileCache = Optional.empty();
    private Optional<String> rangeMapCache = Optional.empty();
    private Optional<String> destinationCache = Optional.empty();

    public VehicleHandler(Thing thing, HttpClient hc, String type, boolean imperial) {
        super(thing);
        this.imperial = imperial;
        hasFuel = type.equals(VehicleType.CONVENTIONAL.toString()) || type.equals(VehicleType.PLUGIN_HYBRID.toString())
                || type.equals(VehicleType.ELECTRIC_REX.toString());
        isElectric = type.equals(VehicleType.PLUGIN_HYBRID.toString())
                || type.equals(VehicleType.ELECTRIC_REX.toString()) || type.equals(VehicleType.ELECTRIC.toString());
        isHybrid = hasFuel && isElectric;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String group = channelUID.getGroupId();

        // Refresh of Channels with cached values
        if (command instanceof RefreshType) {
            if (CHANNEL_GROUP_LAST_TRIP.equals(group)) {
                lastTripCallback.onResponse(lastTripCache);
            } else if (CHANNEL_GROUP_LIFETIME.equals(group)) {
                allTripsCallback.onResponse(allTripsCache);
            } else if (CHANNEL_GROUP_LAST_TRIP.equals(group)) {
                lastTripCallback.onResponse(lastTripCache);
            } else if (CHANNEL_GROUP_LAST_TRIP.equals(group)) {
                lastTripCallback.onResponse(lastTripCache);
            } else if (CHANNEL_GROUP_STATUS.equals(group)) {
                vehicleStatusCallback.onResponse(vehicleStatusCache);
            } else if (CHANNEL_GROUP_CHARGE.equals(group)) {
                vehicleStatusCallback.onResponse(chargeProfileCache);
            }
        } else {
            // Executing Remote Services
            if (CHANNEL_GROUP_REMOTE.equals(channelUID.getGroupId())) {
                logger.info("Remote Command {}", CHANNEL_GROUP_REMOTE);
                if (command instanceof OnOffType) {
                    if (command.equals(OnOffType.ON)) {
                        if (remote.isPresent()) {
                            switch (channelUID.getIdWithoutGroup()) {
                                case REMOTE_SERVICE_LIGHT_FLASH:
                                    updateState(remoteLightChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.LIGHT_FLASH)));
                                    break;
                                case REMOTE_SERVICE_AIR_CONDITIONING:
                                    updateState(remoteClimateChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.AIR_CONDITIONING)));
                                    break;
                                case REMOTE_SERVICE_DOOR_LOCK:
                                    updateState(remoteLockChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.DOOR_LOCK)));
                                    break;
                                case REMOTE_SERVICE_DOOR_UNLOCK:
                                    updateState(remoteUnlockChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.DOOR_UNLOCK)));
                                    break;
                                case REMOTE_SERVICE_HORN:
                                    updateState(remoteHornChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.HORN)));
                                    break;
                                case REMOTE_SERVICE_VEHICLE_FINDER:
                                    updateState(remoteFinderChannel,
                                            OnOffType.from(remote.get().execute(RemoteService.VEHICLE_FINDER)));
                                    break;
                            }
                        }
                        updateState(vehicleFingerPrint, OnOffType.OFF);
                    }
                }
            }

            // Log Troubleshoot data
            if (channelUID.getIdWithoutGroup().equals(VEHICLE_FINGERPRINT)) {
                logger.info("Trigger Vehicle Fingerprint");
                if (command instanceof OnOffType) {
                    if (command.equals(OnOffType.ON)) {
                        logger.warn(
                                "###### BMW ConnectedDrive Binding - Vehicle Troubleshoot Fingerprint Data - BEGIN ######");
                        logger.warn("### Discovery Result ###");
                        logger.warn("{}", bridgeHandler.get().getDiscoveryFingerprint());
                        if (vehicleStatusCache.isPresent()) {
                            logger.warn("### Vehicle Status ###");

                            // Anonymous data for VIN and Position
                            VehicleStatusContainer container = Converter.getGson().fromJson(vehicleStatusCache.get(),
                                    VehicleStatusContainer.class);
                            VehicleStatus status = container.vehicleStatus;
                            status.vin = Constants.ANONYMOUS;
                            if (status.position != null) {
                                status.position.lat = -1;
                                status.position.lon = -1;
                                status.position.heading = -1;
                                logger.warn("{}", Converter.getGson().toJson(container));
                            }
                        } else {
                            logger.warn("### Vehicle Status Empty ###");
                        }
                        if (lastTripCache.isPresent()) {
                            logger.warn("### Last Trip ###");
                            logger.warn("{}",
                                    lastTripCache.get().replaceAll(configuration.get().vin, Constants.ANONYMOUS));
                        } else {
                            logger.warn("### Last Trip Empty ###");
                        }
                        if (allTripsCache.isPresent()) {
                            logger.warn("### All Trips ###");
                            logger.warn("{}",
                                    allTripsCache.get().replaceAll(configuration.get().vin, Constants.ANONYMOUS));
                        } else {
                            logger.warn("### All Trips Empty ###");
                        }
                        if (isElectric) {
                            if (chargeProfileCache.isPresent()) {
                                logger.warn("### Charge Profile ###");
                                logger.warn("{}", chargeProfileCache.get().replaceAll(configuration.get().vin,
                                        Constants.ANONYMOUS));
                            } else {
                                logger.warn("### Charge Profile Empty ###");
                            }
                        }
                        if (destinationCache.isPresent()) {
                            logger.warn("### Charge Profile ###");
                            DestinationContainer container = Converter.getGson().fromJson(destinationCache.get(),
                                    DestinationContainer.class);
                            if (container.destinations != null) {
                                container.destinations.forEach(entry -> {
                                    entry.lat = 0;
                                    entry.lon = 0;
                                    entry.city = Constants.ANONYMOUS;
                                    entry.street = Constants.ANONYMOUS;
                                    entry.streetNumber = Constants.ANONYMOUS;
                                    entry.country = Constants.ANONYMOUS;
                                });
                                logger.warn("{}", Converter.getGson().toJson(container));
                            } else {
                                logger.warn("### Destinations Empty ###");
                            }
                        } else {
                            logger.warn("### Charge Profile Empty ###");
                        }
                        if (rangeMapCache.isPresent()) {
                            logger.warn("### Range Map ###");
                            logger.warn("{}",
                                    rangeMapCache.get().replaceAll(configuration.get().vin, Constants.ANONYMOUS));
                        } else {
                            logger.warn("### Range Map Empty ###");
                        }
                        logger.warn(
                                "###### BMW ConnectedDrive Binding - Vehicle Troubleshoot Fingerprint Data - END ######");
                    }
                    // Switch back to off immediately
                    updateState(channelUID, OnOffType.OFF);
                }
            }
        }
    }

    /**
     * URLs taken from https://github.com/bimmerconnected/bimmer_connected/blob/master/bimmer_connected/const.py
     *
     * """URLs for different services and error code mapping."""
     *
     * AUTH_URL = 'https://customer.bmwgroup.com/{gcdm_oauth_endpoint}/authenticate'
     * AUTH_URL_LEGACY = 'https://{server}/gcdm/oauth/token'
     * BASE_URL = 'https://{server}/webapi/v1'
     *
     * VEHICLES_URL = BASE_URL + '/user/vehicles'
     * VEHICLE_VIN_URL = VEHICLES_URL + '/{vin}'
     * VEHICLE_STATUS_URL = VEHICLE_VIN_URL + '/status'
     * REMOTE_SERVICE_STATUS_URL = VEHICLE_VIN_URL + '/serviceExecutionStatus?serviceType={service_type}'
     * REMOTE_SERVICE_URL = VEHICLE_VIN_URL + "/executeService"
     * VEHICLE_IMAGE_URL = VEHICLE_VIN_URL + "/image?width={width}&height={height}&view={view}"
     * VEHICLE_POI_URL = VEHICLE_VIN_URL + '/sendpoi'
     *
     * }
     */
    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        configuration = Optional.of(getConfigAs(VehicleConfiguration.class));
        if (configuration.isPresent()) {
            scheduler.execute(() -> {
                Bridge bridge = getBridge();
                if (bridge != null) {
                    BridgeHandler handler = bridge.getHandler();
                    if (handler != null) {
                        bridgeHandler = Optional.of(((ConnectedDriveBridgeHandler) handler));
                        proxy = bridgeHandler.get().getProxy();
                        if (proxy.isPresent()) {
                            remote = Optional.of(proxy.get().getRemoteServiceHandler(this));
                        }
                    } else {
                        logger.warn("Brdige Handler null");
                    }
                } else {
                    logger.warn("Bridge null");
                }

                // Switch all Remote Service Channels Off
                switchRemoteServicesOff();
                updateState(vehicleFingerPrint, OnOffType.OFF);

                // get Vehicle Image one time at the beginning
                proxy.get().requestImage(configuration.get(), imageCallback);

                // check imperial setting is different to AutoDetect
                if (!UNITS_AUTODETECT.equals(configuration.get().units)) {
                    imperial = UNITS_IMPERIAL.equals(configuration.get().units);
                }

                // start update schedule
                startSchedule(configuration.get().refreshInterval);
            });
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    private void switchRemoteServicesOff() {
        updateState(remoteLightChannel, OnOffType.from(false));
        updateState(remoteFinderChannel, OnOffType.from(false));
        updateState(remoteLockChannel, OnOffType.from(false));
        updateState(remoteUnlockChannel, OnOffType.from(false));
        updateState(remoteHornChannel, OnOffType.from(false));
        updateState(remoteClimateChannel, OnOffType.from(false));
        updateState(remoteStateChannel, OnOffType.from(false));
    }

    private void startSchedule(int interval) {
        if (refreshJob.isPresent()) {
            if (refreshJob.get().isCancelled()) {
                Optional.of(scheduler.scheduleWithFixedDelay(this::getData, 0, interval, TimeUnit.MINUTES));
            } // else - scheduler is already running!
        } else {
            Optional.of(scheduler.scheduleWithFixedDelay(this::getData, 0, interval, TimeUnit.MINUTES));
        }
    }

    @Override
    public void dispose() {
        if (refreshJob.isPresent()) {
            refreshJob.get().cancel(true);
        }
    }

    public void getData() {
        if (proxy.isPresent() && configuration.isPresent()) {
            proxy.get().requestVehcileStatus(configuration.get(), vehicleStatusCallback);
            if (isSupported(Constants.STATISTICS)) {
                proxy.get().requestLastTrip(configuration.get(), lastTripCallback);
                proxy.get().requestAllTrips(configuration.get(), allTripsCallback);
            }
            if (isSupported(Constants.LAST_DESTINATIONS)) {
                proxy.get().requestDestinations(configuration.get(), destinationCallback);
            }
            if (isElectric) {
                proxy.get().requestChargingProfile(configuration.get(), chargeProfileCallback);
            }
        } else {
            logger.warn("ConnectedDrive Proxy isn't present");
        }
    }

    /**
     * Don't stress ConnectedDrive with unnecessary requests. One call at the beginning is done to check the response.
     * After cache has e.g. a proper error response it will be shown in the fingerprint
     *
     * @return
     */
    private boolean isSupported(String service) {
        if (thing.getProperties().containsKey(Constants.SERVICES_SUPPORTED)) {
            if (thing.getProperties().get(Constants.SERVICES_SUPPORTED).contains(Constants.STATISTICS)) {
                return true;
            }
        }
        // if cache is empty give it a try one time to collected Troubleshoot data
        if (!lastTripCache.isPresent() || !allTripsCache.isPresent() || !destinationCache.isPresent()) {
            return true;
        } else {
            return false;
        }
    }

    private void requestRangeMap(Position p) {
        // format_string = '%Y-%m-%dT%H:%M:%S'
        // timestamp = datetime.datetime.now().strftime(format_string)
        // params = {
        // 'deviceTime': timestamp,
        // 'dlat': self._vehicle.observer_latitude,
        // 'dlon': self._vehicle.observer_longitude,
        // }
        double diff = Converter.measureDistance(p.lat, p.lon, currentPosition.lat, currentPosition.lon);
        if (diff > 1000) {
            logger.info("Difference between old {} and new Position {} = {}", currentPosition.toString(), p.toString(),
                    diff);
            LocalDateTime ldt = LocalDateTime.now();
            MultiMap<String> dataMap = new MultiMap<String>();
            dataMap.add("deviceTime", ldt.format(Converter.DATE_INPUT_PATTERN));
            dataMap.add("lat", Float.toString(p.lat));
            dataMap.add("lon", Float.toString(p.lon));
            if (configuration.isPresent()) {
                proxy.get().requestRangeMap(configuration.get(), Optional.of(dataMap), rangeMapCallback);
            }
        }
        currentPosition = p;
    }

    public void updateRemoteExecutionStatus(String service, String status) {
        updateState(remoteStateChannel, StringType
                .valueOf(Converter.toTitleCase(new StringBuffer(service).append(" ").append(status).toString())));
        if (ExecutionState.EXECUTED.toString().equals(status)) {
            switchRemoteServicesOff();
        }
    }

    public Optional<VehicleConfiguration> getConfiguration() {
        return configuration;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * Callbacks for ConnectedDrive Portal
     *
     * @author Bernd Weymann
     *
     */
    @NonNullByDefault({})
    public class ChargeProfilesCallback implements StringResponseCallback {
        @Override
        public void onResponse(Optional<String> content) {
            chargeProfileCache = content;
            if (content.isPresent()) {
                ChargeProfile cp = Converter.getGson().fromJson(content.get(), ChargeProfile.class);
                WeeklyPlanner planner = cp.weeklyPlanner;
                updateState(chargeProfileClimate, OnOffType.from(planner.climatizationEnabled));
                updateState(chargeProfileChargeMode, StringType.valueOf(Converter.toTitleCase(planner.chargingMode)));

                ChargingWindow cw = planner.preferredChargingWindow;
                updateState(chargeWindowStart, StringType.valueOf(cw.startTime));
                updateState(chargeWindowEnd, StringType.valueOf(cw.endTime));

                Timer t1 = planner.timer1;
                updateState(timer1Departure, StringType.valueOf(t1.departureTime));
                updateState(timer1Enabled, OnOffType.from(t1.timerEnabled));
                updateState(timer1Days, StringType.valueOf(t1.getDays()));

                Timer t2 = planner.timer2;
                updateState(timer2Departure, StringType.valueOf(t2.departureTime));
                updateState(timer2Enabled, OnOffType.from(t2.timerEnabled));
                updateState(timer2Days, StringType.valueOf(t2.getDays()));

                Timer t3 = planner.timer3;
                updateState(timer3Departure, StringType.valueOf(t3.departureTime));
                updateState(timer3Enabled, OnOffType.from(t3.timerEnabled));
                updateState(timer3Days, StringType.valueOf(t3.getDays()));
            }
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            chargeProfileCache = Optional.of(Converter.getGson().toJson(error));
        }
    }

    @NonNullByDefault({})
    public class RangeMapCallback implements StringResponseCallback {
        @Override
        public void onResponse(Optional<String> content) {
            rangeMapCache = content;
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            rangeMapCache = Optional.of(Converter.getGson().toJson(error));
        }
    }

    @NonNullByDefault({})
    public class DestinationsCallback implements StringResponseCallback {
        @Override
        public void onResponse(Optional<String> content) {
            destinationCache = content;
            if (content.isPresent()) {
                DestinationContainer dc = Converter.getGson().fromJson(content.get(), DestinationContainer.class);

                if (dc.destinations != null) {
                    if (!dc.destinations.isEmpty()) {
                        updateState(destinationName1, StringType.valueOf(dc.destinations.get(0).getAddress()));
                        updateState(destinationLat1, new DecimalType(dc.destinations.get(0).lat));
                        updateState(destinationLon1, new DecimalType(dc.destinations.get(0).lon));
                    }
                    if (dc.destinations.size() > 1) {
                        updateState(destinationName2, StringType.valueOf(dc.destinations.get(1).getAddress()));
                        updateState(destinationLat2, new DecimalType(dc.destinations.get(1).lat));
                        updateState(destinationLon2, new DecimalType(dc.destinations.get(1).lon));
                    }
                    if (dc.destinations.size() > 2) {
                        updateState(destinationName3, StringType.valueOf(dc.destinations.get(2).getAddress()));
                        updateState(destinationLat3, new DecimalType(dc.destinations.get(2).lat));
                        updateState(destinationLon3, new DecimalType(dc.destinations.get(2).lon));
                    }
                }
            }
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            destinationCache = Optional.of(Converter.getGson().toJson(error));
        }
    }

    @NonNullByDefault({})
    public class ImageCallback implements ByteResponseCallback {
        @Override
        public void onResponse(Optional<byte[]> content) {
            if (content.isPresent()) {
                String contentType = HttpUtil.guessContentTypeFromData(content.get());
                updateState(imageChannel, new RawType(content.get(), contentType));
            }
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
        }
    }

    @NonNullByDefault({})
    public class AllTripsCallback implements StringResponseCallback {
        @Override
        public void onResponse(Optional<String> content) {
            if (content.isPresent()) {
                allTripsCache = content;
                AllTripsContainer at = Converter.getGson().fromJson(content.get(), AllTripsContainer.class);
                AllTrips c = at.allTrips;
                if (c == null) {
                    return;
                }
                updateState(lifeTimeCumulatedDrivenDistance, QuantityType
                        .valueOf(Converter.round(c.totalElectricDistance.userTotal), MetricPrefix.KILO(SIUnits.METRE)));
                updateState(lifeTimeSingleLongestDistance, QuantityType
                        .valueOf(Converter.round(c.chargecycleRange.userHigh), MetricPrefix.KILO(SIUnits.METRE)));
                updateState(lifeTimeAverageConsumption, QuantityType
                        .valueOf(Converter.round(c.avgElectricConsumption.userAverage), SmartHomeUnits.KILOWATT_HOUR));
                updateState(lifeTimeAverageRecuperation, QuantityType
                        .valueOf(Converter.round(c.avgRecuperation.userAverage), SmartHomeUnits.KILOWATT_HOUR));
                updateState(tripDistanceSinceCharging, QuantityType.valueOf(
                        Converter.round(c.chargecycleRange.userCurrentChargeCycle), MetricPrefix.KILO(SIUnits.METRE)));
            }
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            allTripsCache = Optional.of(Converter.getGson().toJson(error));
        }
    }

    @NonNullByDefault({})
    public class LastTripCallback implements StringResponseCallback {
        @Override
        public void onResponse(Optional<String> content) {
            if (content.isPresent()) {
                lastTripCache = content;
                LastTripContainer lt = Converter.getGson().fromJson(content.get(), LastTripContainer.class);
                LastTrip trip = lt.lastTrip;
                if (trip == null) {
                    return;
                }
                updateState(tripDistance,
                        QuantityType.valueOf(Converter.round(trip.totalDistance), MetricPrefix.KILO(SIUnits.METRE)));
                // updateState(tripDistanceSinceCharging,
                // QuantityType.valueOf(entry.lastTrip, MetricPrefix.KILO(SIUnits.METRE)));
                updateState(tripAvgConsumption, QuantityType.valueOf(Converter.round(trip.avgElectricConsumption),
                        SmartHomeUnits.KILOWATT_HOUR));
                updateState(tripAvgRecuperation,
                        QuantityType.valueOf(Converter.round(trip.avgRecuperation), SmartHomeUnits.KILOWATT_HOUR));
            }
        }

        /**
         * Store Error Report in cache variable. Via Fingerprint Channel error is logged and Issue can be raised
         */
        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            lastTripCache = Optional.of(Converter.getGson().toJson(error));
        }
    }

    /**
     * The VehicleStatus is supported by all Vehicle Types so it's used to reflect the Thing Status
     */
    @NonNullByDefault({})
    public class VehicleStatusCallback implements StringResponseCallback {
        private ThingStatus thingStatus = ThingStatus.UNKNOWN;

        /**
         * Vehicle Status is supported by all Vehicles so callback result is used to report Thing Status.
         * If valid content is delivered in onResponse Thing goes online while onError Thing goes offline
         *
         * @param status
         * @param detail
         * @param reason
         */
        private void setThingStatus(ThingStatus status, ThingStatusDetail detail, String reason) {
            if (thingStatus != status) {
                updateStatus(status, detail, reason);
            }
        }

        @Override
        public void onResponse(Optional<String> content) {
            if (content.isPresent()) {
                setThingStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE, Constants.EMPTY);
                vehicleStatusCache = content;
                VehicleStatusContainer status = Converter.getGson().fromJson(content.get(),
                        VehicleStatusContainer.class);
                VehicleStatus vStatus = status.vehicleStatus;
                if (vStatus == null) {
                    return;
                }

                // Vehilce Status
                updateState(lock, StringType.valueOf(Converter.toTitleCase(vStatus.doorLockState)));
                Doors doorState = Converter.getGson().fromJson(Converter.getGson().toJson(vStatus), Doors.class);
                updateState(doors, StringType.valueOf(VehicleStatus.checkClosed(doorState)));
                Windows windowState = Converter.getGson().fromJson(Converter.getGson().toJson(vStatus), Windows.class);
                updateState(windows, StringType.valueOf(VehicleStatus.checkClosed(windowState)));
                updateState(checkControl, StringType.valueOf(vStatus.getCheckControl()));

                CBSMessage service = vStatus.getNextService(imperial);
                updateState(serviceDate, StringType.valueOf(service.getDueDate()));
                updateState(serviceDescription, StringType.valueOf(Converter.toTitleCase(service.getType())));
                if (imperial) {
                    updateState(serviceMilage,
                            QuantityType.valueOf(Converter.round(service.cbsRemainingMileage), ImperialUnits.MILE));
                } else {
                    updateState(serviceMilage, QuantityType.valueOf(Converter.round(service.cbsRemainingMileage),
                            MetricPrefix.KILO(SIUnits.METRE)));
                }

                // Range values
                // based on unit of length decide if range shall be reported in km or miles
                if (!imperial) {
                    updateState(mileage, QuantityType.valueOf(vStatus.mileage, MetricPrefix.KILO(SIUnits.METRE)));
                    float totalRange = 0;
                    if (isElectric) {
                        totalRange += vStatus.remainingRangeElectric;
                        updateState(remainingRangeElectric,
                                QuantityType.valueOf(vStatus.remainingRangeElectric, MetricPrefix.KILO(SIUnits.METRE)));
                        updateState(rangeRadiusElectric,
                                QuantityType.valueOf(Converter.guessRangeRadius(vStatus.remainingRangeElectric),
                                        MetricPrefix.KILO(SIUnits.METRE)));
                    }
                    if (hasFuel) {
                        totalRange += vStatus.remainingRangeFuel;
                        updateState(remainingRangeFuel,
                                QuantityType.valueOf(vStatus.remainingRangeFuel, MetricPrefix.KILO(SIUnits.METRE)));
                        updateState(rangeRadiusFuel,
                                QuantityType.valueOf(Converter.guessRangeRadius(vStatus.remainingRangeFuel),
                                        MetricPrefix.KILO(SIUnits.METRE)));
                    }
                    if (isHybrid) {
                        updateState(remainingRangeHybrid,
                                QuantityType.valueOf(Converter.round(totalRange), MetricPrefix.KILO(SIUnits.METRE)));
                        updateState(rangeRadiusHybrid, QuantityType.valueOf(Converter.guessRangeRadius(totalRange),
                                MetricPrefix.KILO(SIUnits.METRE)));
                    }
                } else {
                    updateState(mileage, QuantityType.valueOf(vStatus.mileage, ImperialUnits.MILE));
                    float totalRange = 0;
                    if (isElectric) {
                        totalRange += vStatus.remainingRangeElectricMls;
                        updateState(remainingRangeElectric,
                                QuantityType.valueOf(vStatus.remainingRangeElectricMls, ImperialUnits.MILE));
                        updateState(rangeRadiusElectric, QuantityType.valueOf(
                                Converter.guessRangeRadius(vStatus.remainingRangeElectricMls), ImperialUnits.MILE));
                    }
                    if (hasFuel) {
                        totalRange += vStatus.remainingRangeFuelMls;
                        updateState(remainingRangeFuel,
                                QuantityType.valueOf(vStatus.remainingRangeFuelMls, ImperialUnits.MILE));
                        updateState(rangeRadiusFuel, QuantityType.valueOf(
                                Converter.guessRangeRadius(vStatus.remainingRangeFuelMls), ImperialUnits.MILE));
                    }
                    if (isHybrid) {
                        updateState(remainingRangeHybrid,
                                QuantityType.valueOf(Converter.round(totalRange), ImperialUnits.MILE));
                        updateState(rangeRadiusHybrid,
                                QuantityType.valueOf(Converter.guessRangeRadius(totalRange), ImperialUnits.MILE));
                    }
                }
                if (isElectric) {
                    updateState(remainingSoc, QuantityType.valueOf(vStatus.chargingLevelHv, SmartHomeUnits.PERCENT));
                }
                if (hasFuel) {
                    updateState(remainingFuel, QuantityType.valueOf(vStatus.remainingFuel, SmartHomeUnits.LITRE));
                }
                // last update Time
                if (vStatus.internalDataTimeUTC != null) {
                    updateState(lastUpdate, new StringType(Converter.getLocalDateTime(vStatus.internalDataTimeUTC)));
                } else {
                    updateState(lastUpdate, new StringType(Converter.getZonedDateTime(vStatus.updateTime)));
                }

                // Charge Values
                if (isElectric) {
                    if (vStatus.chargingStatus != null) {
                        if (Constants.INVALID.equals(vStatus.chargingStatus)) {
                            updateState(chargingStatus,
                                    StringType.valueOf(Converter.toTitleCase(vStatus.lastChargingEndReason)));
                        } else {
                            // State INVALID is somehow misleading. Instead show the Last Charging End Reason
                            updateState(chargingStatus,
                                    StringType.valueOf(Converter.toTitleCase(vStatus.chargingStatus)));
                        }
                    }
                }

                Position p = vStatus.position;
                updateState(latitude, new DecimalType(p.lat));
                updateState(longitude, new DecimalType(p.lon));
                updateState(heading, QuantityType.valueOf(p.heading, SmartHomeUnits.DEGREE_ANGLE));
                requestRangeMap(p);
            }
        }

        @Override
        public void onError(NetworkError error) {
            logger.debug("{}", error.toString());
            vehicleStatusCache = Optional.of(Converter.getGson().toJson(error));
            setThingStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, error.reason);
        }
    }
}
