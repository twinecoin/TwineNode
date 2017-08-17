/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.api.node;

/**
 * Class that handles command line and configuration file parameters.
 * Command line parameters take priority
 */
public interface ConfigurationManager {
	/**
	 * Gets an integer parameter with default fallback
	 *
	 * @param name parameter name
	 * @param def default value
	 * @return
	 */
	public long getIntegerParameter(String name);

	/**
	 * Gets a String parameter with default fallback.  The value must be
	 * alpha-numeric (with '_', '/' and '\' allowed).  The return value will not be
	 * null unless null is the default value.
	 *
	 * @param name parameter name
	 * @param def default value
	 * @return
	 */
	public String getStringParameter(String name);

	/**
	 * Gets a Boolean parameter with default fallback.  If the parameter
	 * is present without a value it will be interpreted as true.  "true"
	 * and "1" are interpreted as true and "false" and "0" are interpreted
	 * as false.  Case is ignored.
	 *
	 * @param name parameter name
	 * @param def default value
	 * @return
	 */
	public boolean getBooleanParameter(String name);
}
