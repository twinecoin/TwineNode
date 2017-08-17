/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.api.node;

import com.sun.jna.Library;


/**
 * Provides access to the consensus library
 */
public interface Consensus extends Library {
	/**
	 * Tests if the library supports supports an interface version
	 *
	 * @param version the version to test
	 * @return non-zero if the version is support
	 */
	public int testVersion(int version);
}
