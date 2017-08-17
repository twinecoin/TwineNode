/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node;

import java.io.File;
import java.util.logging.Logger;

import org.twinecoin.api.node.Consensus;
import org.twinecoin.node.config.TwineConfigurationManager;
import org.twinecoin.node.log.TwineLogger;
import org.twinecoin.node.twineconsensus.jna.TwineConsensusLoader;

public class TwineMain {
	public static void main(String[] args) {
		TwineLogger twineLogger = new TwineLogger();
		Logger logger = twineLogger.getLogger();

		TwineConfigurationManager config = new TwineConfigurationManager(twineLogger, args);

		if (!config.isSuccess() || config.getBooleanParameter("help")) {
			if (!config.isSuccess()) {
				logger.severe("Shuting down: Unable to process configuration");
			}
			config.printHelp();
			System.exit(-1);
		}

		File logDir = config.getLogDir();

		int count = (int) config.getIntegerParameter("logcount");
		long size = config.getIntegerParameter("logsize");

		twineLogger.setLogDir(logDir, count, size);

		if (!twineLogger.getFileSuccess()) {
			logger.severe("Shuting down: Failure to create log files");
			System.exit(-1);
		}

		if (!config.isSuccess()) {
			logger.severe("Shuting down: Failure to process configuration");
			System.exit(-1);
		}

		Consensus consensus = TwineConsensusLoader.getInstance(logger);

		if (consensus == null) {
			twineLogger.getLogger().severe("Unable to load consensus library");
			System.exit(-1);
		}
	}
}
