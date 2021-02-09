/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.remoteopenhab.internal.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.remoteopenhab.internal.rest.RemoteopenhabRestClient;

/**
 * Interface for listeners of general operating events generated by the {@link RemoteopenhabRestClient}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface RemoteopenhabStreamingDataListener {

    /**
     * The client successfully established a connection and received a first event.
     */
    void onConnected();

    /**
     * The client was disconnected.
     */
    void onDisconnected();

    /**
     * An error message was published.
     */
    void onError(String message);
}
