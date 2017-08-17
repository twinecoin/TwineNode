/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node;

import java.io.IOException;
import java.util.logging.Logger;

import org.twinecoin.api.Node;
import org.twinecoin.api.chain.ChainTracker;
import org.twinecoin.api.node.ConfigurationManager;
import org.twinecoin.node.config.TwineConfigurationManager;

public class TwineNode implements Node {
	private final ConfigurationManager config;
	private final Logger logger;

	public TwineNode(TwineConfigurationManager config, Logger logger) throws IOException {
		this.logger = logger;
		this.config = config;
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public ChainTracker getChainTracker() {
		return null;
	}

	@Override
	public ConfigurationManager getConfigurationManager() {
		return config;
	}
}
