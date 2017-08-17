/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.twineconsensus;

import org.twinecoin.api.node.Consensus;
import org.twinecoin.node.twineconsensus.jna.TwineConsensusLibrary;

public class TwineConsensus implements Consensus {
	private final TwineConsensusLibrary consensus;

	public TwineConsensus(TwineConsensusLibrary consensus) {
		this.consensus = consensus;
	}

	@Override
	public int testVersion(int version) {
		return consensus.twlib_test_version(version);
	}
}
