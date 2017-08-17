/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.log;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class TwineFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		return String.format("%-9s", record.getLevel() + ":") + formatMessage(record) + "\n";
	}
}
