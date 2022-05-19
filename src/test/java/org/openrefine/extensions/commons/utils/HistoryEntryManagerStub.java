package org.openrefine.extensions.commons.utils;

import java.io.File;
import java.io.Writer;
import java.util.Properties;

import com.google.refine.ProjectManager;
import com.google.refine.history.HistoryEntry;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.io.FileProjectManager;

public class HistoryEntryManagerStub implements HistoryEntryManager {

    @Override
    public void delete(HistoryEntry historyEntry) {
    }

    @Override
    public void save(HistoryEntry historyEntry, Writer writer, Properties options) {
    }

    @Override
    public void loadChange(HistoryEntry historyEntry) {
    }

    protected void loadChange(HistoryEntry historyEntry, File file) throws Exception {
    }

    @Override
    public void saveChange(HistoryEntry historyEntry) throws Exception {
    }

    protected void saveChange(HistoryEntry historyEntry, File file) throws Exception {
    }

    protected File getChangeFile(HistoryEntry historyEntry) {
        return new File(getHistoryDir(historyEntry), historyEntry.id + ".change.zip");
    }

    protected File getHistoryDir(HistoryEntry historyEntry) {
        File dir = new File(((FileProjectManager) ProjectManager.singleton)
                .getProjectDir(historyEntry.projectID),
                "history");
        dir.mkdirs();

        return dir;
    }
}
