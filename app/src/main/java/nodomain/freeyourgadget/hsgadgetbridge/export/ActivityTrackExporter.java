package nodomain.freeyourgadget.hsgadgetbridge.export;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.hsgadgetbridge.model.ActivityTrack;

public interface ActivityTrackExporter {
    @NonNull
    String getDefaultFileName(@NonNull ActivityTrack track);

    void performExport(ActivityTrack track, File targetFile) throws IOException, GPXTrackEmptyException;

    class GPXTrackEmptyException extends Exception {
    }
}
