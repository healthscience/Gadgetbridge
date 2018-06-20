package nodomain.freeyourgadget.hsgadgetbridge.adapter;

import android.content.Context;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.hsgadgetbridge.GBApplication;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.hsgadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.hsgadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.hsgadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.hsgadgetbridge.entities.Device;
import nodomain.freeyourgadget.hsgadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.hsgadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.hsgadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.hsgadgetbridge.util.GB;

public class ActivitySummariesAdapter extends AbstractItemAdapter<BaseActivitySummary> {
    private final GBDevice device;

    public ActivitySummariesAdapter(Context context, GBDevice device) {
        super(context);
        this.device = device;
        loadItems();
    }

    @Override
    public void loadItems() {
        try (DBHandler handler = GBApplication.acquireDB()) {
            BaseActivitySummaryDao summaryDao = handler.getDaoSession().getBaseActivitySummaryDao();
            Device dbDevice = DBHelper.findDevice(device, handler.getDaoSession());

            QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
            qb.where(BaseActivitySummaryDao.Properties.DeviceId.eq(dbDevice.getId())).orderDesc(BaseActivitySummaryDao.Properties.StartTime);
            List<BaseActivitySummary> allSummaries = qb.build().list();
            setItems(allSummaries, true);
        } catch (Exception e) {
            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    @Override
    protected String getName(BaseActivitySummary item) {
        String name = item.getName();
        if (name != null && name.length() > 0) {
            return name;
        }

        Date startTime = item.getStartTime();
        if (startTime != null) {
            return DateTimeUtils.formatDateTime(startTime);
        }
        return "Unknown activity";
    }

    @Override
    protected String getDetails(BaseActivitySummary item) {
        return ActivityKind.asString(item.getActivityKind(), getContext());
    }

    @Override
    protected int getIcon(BaseActivitySummary item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }
}
