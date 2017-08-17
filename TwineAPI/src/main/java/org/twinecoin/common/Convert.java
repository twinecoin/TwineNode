/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.common;

public class Convert {

	public static byte[] hexToBytes(String hex) {
		if (hex == null) {
			throw new NullPointerException("Hex string may not be null");
		}
		if ((hex.length() & 1) != 0) {
			throw new IllegalArgumentException("Hex string must have an even number of characters");
		}
		byte[] bytes = new byte[hex.length() >> 1];
		int j = 0;
		for (int i = 0; i < hex.length(); i += 2) {
			int b = (hexCharToInt(hex.charAt(i)) << 4) + hexCharToInt(hex.charAt(i + 1));
			if (b < 0) {
				throw new IllegalArgumentException("Hex string must only contain hex characters, " + hex);
			}
			bytes[j++] = (byte) b;
		}
		return bytes;
	}

	public static int hexCharToInt(char hex) {
		if (hex >= '0' && hex <= '9') {
			return hex - '0';
		} else if (hex >= 'a' && hex <= 'f') {
			return 10 + hex - 'a';
		} else if (hex >= 'A' && hex <= 'F') {
			return 10 + hex - 'A';
		} else {
			return -256;
		}
	}
}
