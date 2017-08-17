/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.test.TestUtils;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class FakeHandler extends Handler {

	private int count = 0;

	public int getMessageCount() {
		return count;
	}

	public void resetMessageCount() {
		count = 0;
	}

	@Override
	public void publish(LogRecord record) {
		if (isLoggable(record)) {
			count++;
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

}
