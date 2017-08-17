/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.common;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA256 {

	private final static ThreadLocal<MessageDigest> localSHA256MD = new ThreadLocal<MessageDigest>() {
		protected MessageDigest initialValue() {
			return createSHA256MessageDigest();
		}
	};

	private static MessageDigest createSHA256MessageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 message digest not supported by JVM");
		}
	}

	private static MessageDigest getMessageDigest() {
		return localSHA256MD.get();
	}

	public static byte[] getDigest(InputStream in) throws IOException {
		MessageDigest md = getMessageDigest();
		byte[] buf = new byte[8192];
		int read = 0;
		while (read >= 0) {
			read = in.read(buf);
			if (read >= 0) {
				md.update(buf, 0, read);
			}
		}
		return md.digest();
	}
}
