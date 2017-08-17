/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.twineconsensus.jna;

import com.sun.jna.Library;

/**
 * Interface for use with JNA
 */
public interface TwineConsensusLibrary extends Library {
	public int twlib_test_version(int version);
}
