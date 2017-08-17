/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.api.chain;

import org.twinecoin.api.Node;


/**
 * The ChainTracker class maintains the primary connection to the network.   It
 * ensures that the node is synchronized to the main chain.  It provides the
 * minimal functionality required for all node types on the network.
 * <br>
 * It consists of<br>
 * - Twine Consensus Library Loader
 * - P2P Network Manager (UDP based)<br>
 * - SPV Chain Monitor (headers-only)<br>
 * - Address Manager<br>
 * - Fraud proof based block blacklisting (planned)
 */
public interface ChainTracker {
	/**
	 * Gets the Node
	 */
	public Node getNode();

	/**
	 * Gets the AddressTracker
	 */
	public AddressTracker getAddressTracker();
}
