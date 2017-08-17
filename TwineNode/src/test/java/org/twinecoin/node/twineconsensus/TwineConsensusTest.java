/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.twineconsensus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.twinecoin.api.node.Consensus;
import org.twinecoin.node.log.TwineLogger;
import org.twinecoin.node.twineconsensus.jna.TwineConsensusLoader;

public class TwineConsensusTest {
	@Before
	public void before() {
		deleteNatives();
	}
	
	@Test
	public void testTwineConsensusLoader() throws IOException {
		TwineLogger twineLogger = new TwineLogger();
		twineLogger.setConsoleLevel(Level.OFF);
		Logger logger = twineLogger.getLogger();
		
		Consensus consensus = TwineConsensusLoader.getInstance(logger);

		assertFalse("Unable to load twine consensus", consensus == null);

		for (int i = 0; i < 10; i++) {
			int exp = i == 0 ? 1 : 0;
			assertEquals("Unexpected result from testVersion", exp, consensus.testVersion(i));
		}
	}
	
	private static void deleteNatives() {
		File nativesDir = new File("natives");
		if (!nativesDir.exists()) {
			return;
		}
		assertTrue("Natives directory is not a directory", nativesDir.isDirectory());
		for (File subdir : nativesDir.listFiles()) {
			if (!subdir.isDirectory()) {
				continue;
			}
			for (File file : subdir.listFiles()) {
				if (file.getName().contains(".dll") || file.getName().contains(".so")) {
					assertTrue("Unable to delete native file, " + file, file.delete());
				}
			}
		}
	}
}
