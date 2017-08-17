/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.twineconsensus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

import org.twinecoin.common.Convert;
import org.twinecoin.common.SHA256;

public class ResourceLoader {
	
	private final Logger logger;
	
	public ResourceLoader(Logger logger) {
		this.logger = logger;
	}

	public boolean checkResources(String[][] entries) {
		for (String[] entry : entries) {
			String target = entry[0];
			String resource = entry[1];
			if (!checkResource(target, resource)) {
				if (unpackResource(target, resource)) {
					if (checkResource(target, resource)) {
						continue;
					}
				}
				return false;
			}
		}
		return true;
	}

	private boolean unpackResource(final String target, final String resourcePath) {
		InputStream resourceIn = ResourceLoader.class.getResourceAsStream("/" + resourcePath);
		if (resourceIn == null) {
			logger.severe("Unable to open resource file, /" + resourcePath);
			return false;
		}

		File targetFile = getTargetFile(target, resourcePath, true);

		OutputStream targetOut = null;

		try {
			targetOut = new FileOutputStream(targetFile);
			byte[] buf = new byte[8192];
			int read = 0;
			while (read >= 0) {
				read = resourceIn.read(buf);
				if (read > 0) {
					targetOut.write(buf, 0, read);
				}
			}
		} catch (FileNotFoundException e) {
			logger.severe("Unable to find resource file, " + e.getMessage());
			return false;
		} catch (IOException e) {
			logger.severe("Unable to copying resource file, " + e.getMessage());
			return false;
		} finally {
			try {
				resourceIn.close();
			} catch (IOException e) {
			} finally {
				if (targetOut != null) {
					try {
						targetOut.close();
					} catch (IOException e) {}
				}
			}
		}

		return true;
	}

	private boolean checkResource(final String target, final String resourcePath) {
		InputStream expectedIn = ResourceLoader.class.getResourceAsStream("/" + resourcePath + ".sha256");
		if (expectedIn == null) {
			logger.severe("Unable to open sha256 expected file " + "/" + resourcePath + ".sha256");
			return false;
		}

		byte[] expectedBytes;

		BufferedReader expectedBufferedReader = null;
		try {
			expectedBufferedReader = new BufferedReader(new InputStreamReader(expectedIn, StandardCharsets.UTF_8));
			String hex = expectedBufferedReader.readLine();
			expectedBytes = Convert.hexToBytes(hex);
		} catch (IOException e) {
			logger.severe("Unable to sha256 expected file " + "/" + resourcePath + ".sha256");
			return false;
		} finally {
			try {
				expectedIn.close();
			} catch (IOException e) {}
		}

		if (expectedBytes == null) {
			logger.severe("Unable to decode sha256 expected hex bytes");
			return false;
		}

		File targetFile = getTargetFile(target, resourcePath);

		InputStream targetIn = null;
		byte[] readBytes = null;

		try {
			targetIn = new FileInputStream(targetFile);
			readBytes = SHA256.getDigest(targetIn);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			logger.severe("Unable to read file " + targetFile);
		} finally {
			if (targetIn != null) {
				try {
					targetIn.close();
				} catch (IOException e) {}
			}
		}

		return Arrays.equals(readBytes, expectedBytes);
	}

	private File getTargetFile(final String target, final String resourcePath) {
		return getTargetFile(target, resourcePath, false);
	}

	private File getTargetFile(final String target, final String resourcePath, boolean createDir) {
		File f = new File(target, resourcePath.replace("/", File.separator));
		if (createDir) {
			File dir = f.getParentFile();
			if (dir != null) {
				dir.mkdirs();
			}
		}
		return f;
	}
}
