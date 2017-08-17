/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.api;

import java.util.logging.Logger;

import org.twinecoin.api.chain.ChainTracker;
import org.twinecoin.api.node.ConfigurationManager;

/**
 * TwineNode<br>
 * <br>
 * This is the root class for the
 */
public interface Node {
	/**
	 * Gets a logger.<br>
	 * <br>
	 * <br>
	 * Debug messages<br>
	 * FINEST - Verbose Debug Messages<br>
	 * FINER - Debug Messages<br>
	 * <br>
	 * Information<br>
	 * FINE - Verbose Information<br>
	 * INFO - Informative<br>
	 * <br>
	 * Warnings/Errors<br>
	 * WARN - Unexpected event or fail that doesn't require shutdown<br>
	 * SEVERE - Critical failures<br>
	 */
	public Logger getLogger();

	/**
	 * Gets the Chain Tracker<br>
	 * <br>
	 * All Nodes must have a ChainTracker
	 * @return the chain tracker
	 */
	public ChainTracker getChainTracker();

	/**
	 * Gets the ConfigurationManager
	 */
	public ConfigurationManager getConfigurationManager();
}
