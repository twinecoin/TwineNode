/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.log;

import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TwineLogger {
	private final Logger logger;
	private final TwineFileHandler fileHandler;
	private final Handler consoleHandler;
	private boolean fileSuccess;

	public TwineLogger() {
		this("");
	}

	public TwineLogger(String uniqueId) {
		Logger logger = Logger.getLogger(TwineLogger.class.getName() + uniqueId);
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);

		Formatter formatter = new TwineFormatter();

		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(formatter);
		consoleHandler.setLevel(Level.INFO);
		logger.addHandler(consoleHandler);

		TwineFileHandler fileHandler = new TwineFileHandler();
		fileHandler.setFormatter(formatter);
		fileHandler.setLevel(Level.FINE);
		logger.addHandler(fileHandler);

		this.logger = logger;
		this.fileHandler = fileHandler;
		this.consoleHandler = consoleHandler;

		this.fileSuccess = false;
	}

	public void setConsoleLevel(Level level) {
		this.consoleHandler.setLevel(level);
	}

	public void setFileLevel(Level level) {
		this.fileHandler.setLevel(level);
	}

	public void setLogDir(File logDir, int count, long size) {
		boolean fileSuccess = false;

		if (logDir != null) {
			logDir.mkdir();
		}

		if (logDir != null && logDir.isDirectory()) {
			try {
				((TwineFileHandler) fileHandler).setLogDir(logDir, "twinelog", count, size);
				fileSuccess = true;
			} catch (IOException e) {
				logger.severe("Error when setting log directory, " + e.getMessage());
			}
		} else {
			logger.severe("Log directory is not a directory");
		}
		if (!fileSuccess) {
			logger.severe("Unable to open log files");
		}

		this.fileSuccess = fileSuccess;
	}

	public boolean getFileSuccess() {
		return fileSuccess;
	}

	public Logger getLogger() {
		return logger;
	}
}
