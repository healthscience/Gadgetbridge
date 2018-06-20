/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.hsgadgetbridge.devices.pebble;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.hsgadgetbridge.GBApplication;
import nodomain.freeyourgadget.hsgadgetbridge.GBException;
import nodomain.freeyourgadget.hsgadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.hsgadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.hsgadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.hsgadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.hsgadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.hsgadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.hsgadgetbridge.entities.Device;
import nodomain.freeyourgadget.hsgadgetbridge.entities.PebbleHealthActivityOverlayDao;
import nodomain.freeyourgadget.hsgadgetbridge.entities.PebbleHealthActivitySampleDao;
import nodomain.freeyourgadget.hsgadgetbridge.entities.PebbleMisfitSampleDao;
import nodomain.freeyourgadget.hsgadgetbridge.entities.PebbleMorpheuzSampleDao;
import nodomain.freeyourgadget.hsgadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.hsgadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.hsgadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.hsgadgetbridge.util.PebbleUtils;
import nodomain.freeyourgadget.hsgadgetbridge.util.Prefs;

public class PebbleCoordinator extends AbstractDeviceCoordinator {
    public PebbleCoordinator() {
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        if (name != null && name.startsWith("Pebble")) {
            return DeviceType.PEBBLE;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.PEBBLE;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return PebblePairingActivity.class;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getPebbleHealthActivitySampleDao().queryBuilder();
        qb.where(PebbleHealthActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getPebbleHealthActivityOverlayDao().queryBuilder();
        qb.where(PebbleHealthActivityOverlayDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getPebbleMisfitSampleDao().queryBuilder();
        qb.where(PebbleMisfitSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
        qb = session.getPebbleMorpheuzSampleDao().queryBuilder();
        qb.where(PebbleMorpheuzSampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        Prefs prefs = GBApplication.getPrefs();
        int activityTracker = prefs.getInt("pebble_activitytracker", SampleProvider.PROVIDER_PEBBLE_HEALTH);
        switch (activityTracker) {
            case SampleProvider.PROVIDER_PEBBLE_HEALTH:
                return new PebbleHealthSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_MISFIT:
                return new PebbleMisfitSampleProvider(device, session);
            case SampleProvider.PROVIDER_PEBBLE_MORPHEUZ:
                return new PebbleMorpheuzSampleProvider(device, session);
            default:
                return new PebbleHealthSampleProvider(device, session);
        }
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        PBWInstallHandler installHandler = new PBWInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return false;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsScreenshots() {
        return true;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return PebbleUtils.hasHRM(device.getModel());
    }

    @Override
    public String getManufacturer() {
        return "Pebble";
    }

    @Override
    public boolean supportsAppsManagement() {
        return true;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }
}
