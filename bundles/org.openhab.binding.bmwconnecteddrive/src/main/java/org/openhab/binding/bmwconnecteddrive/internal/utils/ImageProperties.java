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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link ImageProperties} Properties of current Vehicle Image
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class ImageProperties {
    public static final int RETRY_COUNTER = 5;
    public int failCounter = 0;
    public String viewport = Constants.EMPTY;
    public int size = -1;

    public ImageProperties(String viewport, int size) {
        this.viewport = viewport;
        this.size = size;
    }

    public ImageProperties() {
    }

    public boolean isSame(String viewport, int size) {
        return this.viewport.equals(viewport) && this.size == size;
    }

    public void failed() {
        failCounter++;
    }

    public boolean failLimitReached() {
        return failCounter > RETRY_COUNTER;
    }

    @Override
    public String toString() {
        return new StringBuffer(viewport).append(Integer.toString(size)).toString();
    }
}
