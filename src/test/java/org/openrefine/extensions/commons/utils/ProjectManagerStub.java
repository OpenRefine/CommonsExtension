package org.openrefine.extensions.commons.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import com.google.refine.ProjectManager;
import com.google.refine.ProjectMetadata;
import com.google.refine.history.HistoryEntryManager;
import com.google.refine.model.Project;

public class ProjectManagerStub extends ProjectManager {

    @Override
    public void deleteProject(long projectID) {
        // empty

    }

    @Override
    public void exportProject(long projectId, TarArchiveOutputStream tos) throws IOException {
        // empty
    }

    @Override
    public HistoryEntryManager getHistoryEntryManager() {
        return new HistoryEntryManagerStub();
    }

    @Override
    public void importProject(long projectID, InputStream inputStream, boolean gziped) throws IOException {
        // empty
    }

    @Override
    protected Project loadProject(long id) {
        // empty
        return null;
    }

    @Override
    public boolean loadProjectMetadata(long projectID) {
        // empty
        return false;
    }

    @Override
    public void saveMetadata(ProjectMetadata metadata, long projectId) throws Exception {
        // empty

    }

    @Override
    public void saveProject(Project project) {
        // empty
    }

    // Overridden to make public for testing
    @Override
    public void saveProjects(boolean allModified) {
        super.saveProjects(allModified);
    }

    @Override
    protected void saveWorkspace() {
        // empty
    }

}
