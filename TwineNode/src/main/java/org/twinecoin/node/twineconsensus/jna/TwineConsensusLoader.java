/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.twineconsensus.jna;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.twinecoin.api.node.Consensus;
import org.twinecoin.node.twineconsensus.ResourceLoader;
import org.twinecoin.node.twineconsensus.TwineConsensus;

import com.sun.jna.Native;
import com.sun.jna.Platform;

public class TwineConsensusLoader {

	private static final int LIBRARY_VERSION = 0;

	private static final String[][] resources = new String[][] {
		{"natives", "win32-x86-64/libtwineconsensus-0.dll"},
		{"natives", "win32-x86-64/libsecp256k1-0.dll"},

		{"natives", "win32-x86/libtwineconsensus-0.dll"},
		{"natives", "win32-x86/libsecp256k1-0.dll"},

		{"natives", "linux-x86-64/libtwineconsensus.so.0"},
		{"natives", "linux-x86-64/libsecp256k1.so.0"}
	};

	private static final AtomicReference<Consensus> consensus = new AtomicReference<Consensus>();
	
	public static Consensus getInstance(Logger logger) {
		Consensus instance = consensus.get();
		if (instance == null) {
			synchronized (consensus) {
				instance = consensus.get();
				if (instance == null) {
					instance = loadConsensus(logger);
					consensus.set(instance);
				}
			}
		}
		return instance;
	}

	private static Consensus loadConsensus(Logger logger) {
		TwineConsensusLibrary library = null;
		ResourceLoader resourceLoader = new ResourceLoader(logger);
		if (resourceLoader.checkResources(resources)) {
			updateJNAPath();
			try {
				String libraryName = getLibraryFilename("libtwineconsensus", LIBRARY_VERSION).replaceFirst("\\..*", "");
				library = Native.loadLibrary(libraryName, TwineConsensusLibrary.class);
			} catch (UnsatisfiedLinkError e) {
				library = null;
			}
		}
		return library == null ? null : new TwineConsensus(library);
	}

	private static String getLibraryFilename(String name, int version) {
		return Platform.isWindows() ? (name + "-" + version + ".dll") : name + ".so." + version;
	}

	private static void updateJNAPath() {
		File natives = new File("natives");

		String path = System.getProperties().getProperty("jna.library.path");

		if (path == null) {
			path = "";
		}

		String nativePath = natives.getAbsolutePath() + File.separator + Platform.RESOURCE_PREFIX + File.separator;

		if (!pathContains(path, nativePath)) {
			if (!path.endsWith(File.pathSeparator)) {
				path += File.pathSeparator;
			}
			path += nativePath;
		}

		System.getProperties().setProperty("jna.library.path", path);
	}

	private static boolean pathContains(String path, String nativePath) {
		boolean contains = false;
		contains |= path.equals(nativePath);
		contains |= path.startsWith(nativePath + File.pathSeparator);
		contains |= path.endsWith(File.pathSeparator + nativePath);
		contains |= path.contains(File.pathSeparator + nativePath + File.pathSeparator);
		return contains;
	}
}
