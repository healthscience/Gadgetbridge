/*  Copyright (C) 2017-2018 João Paulo Barraca

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.hsgadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import android.support.annotation.NonNull;

import nodomain.freeyourgadget.hsgadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.hsgadgetbridge.model.DeviceType;

/**
 * Pseudo Coordinator for the Makibes F68, a sub type of the HPLUS devices
 */
public class MakibesF68Coordinator extends HPlusCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        if(name != null && name.startsWith("SPORT")){
            return DeviceType.MAKIBESF68;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MAKIBESF68;
    }

    @Override
    public String getManufacturer() {
        return "Makibes";
    }

}
