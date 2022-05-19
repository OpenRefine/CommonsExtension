package org.openrefine.extensions.commons.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.RefineServlet;
import com.google.refine.commands.Command;

public class RefineServletStub extends RefineServlet {

    private static File tempDir = null;

    // requirement of extending HttpServlet, not required for testing
    private static final long serialVersionUID = 1L;

    public void wrapService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.service(request, response);
    }

    public String wrapGetCommandName(HttpServletRequest request) {
        return super.getCommandKey(request);
    }

    @Override
    public File getTempDir() {
        if (tempDir == null) {
            try {
                Path tempDirPath = Files.createTempDirectory("refine-test-dir");
                tempDir = tempDirPath.toFile();
                tempDir.deleteOnExit();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create temp directory", e);
            }
        }
        return tempDir;
    }

    // -------------------helper methods--------------
    /**
     * Helper method for inserting a mock object
     *
     * @param commandName
     * @param command
     */
    public void insertCommand(String commandName, Command command) {
        registerOneCommand("core/" + commandName, command);
    }

    /**
     * Helper method for clearing up after testing
     *
     * @param commandName
     */
    public void removeCommand(String commandName) {
        unregisterCommand("core/" + commandName);
    }
}
