/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.hsgadgetbridge.devices.huami.amazfitcor;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.IOException;

import nodomain.freeyourgadget.hsgadgetbridge.devices.huami.HuamiFWHelper;
import nodomain.freeyourgadget.hsgadgetbridge.service.devices.amazfitbip.AmazfitBipFirmwareInfo;
import nodomain.freeyourgadget.hsgadgetbridge.service.devices.huami.amazfitcor.AmazfitCorFirmwareInfo;

public class AmazfitCorFWHelper extends HuamiFWHelper {

    public AmazfitCorFWHelper(Uri uri, Context context) throws IOException {
        super(uri, context);
    }

    @NonNull
    @Override
    protected void determineFirmwareInfo(byte[] wholeFirmwareBytes) {
        firmwareInfo = new AmazfitCorFirmwareInfo(wholeFirmwareBytes);
        if (!firmwareInfo.isHeaderValid()) {
            throw new IllegalArgumentException("Not a an Amazifit Bip firmware");
        }
    }
}
