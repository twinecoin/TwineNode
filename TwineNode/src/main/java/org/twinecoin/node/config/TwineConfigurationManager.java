/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.twinecoin.api.node.ConfigurationManager;
import org.twinecoin.common.Pair;
import org.twinecoin.node.log.TwineLogger;

public class TwineConfigurationManager implements ConfigurationManager {
	private final static String configFilename = "twinecoin.conf";

	private final static String[][] integerParamArray = new String[][] {
		{"logcount", "Target number of log files to be kept", "10"},
		{"logsize", "Target size of log files", "1048576"}
	};

	private final static String[][] stringParamArray = new String[][] {
		{"rootdir", "Root directory for node", null},
		{"consoleloglevel", "Sets level for log messages to the console", "info"},
		{"fileloglevel", "Sets level for log messages to the log file", "fine"}
	};

	private final static String[][] booleanParamArray = new String[][] {
		{"help", "Lists command line arguments", "false"}
	};

	private final TwineLogger twineLogger;
	private final Logger logger;

	private final Map<String, String> map = new HashMap<String, String>();

	private final Map<String, Pair<String, Long>> integerParamMap;
	private final Map<String, Pair<String, String>> stringParamMap;
	private final Map<String, Pair<String, Boolean>> booleanParamMap;

	private final boolean success;
	private final File rootDir;

	public TwineConfigurationManager(TwineLogger twineLogger, String[] args) {
		this.twineLogger = twineLogger;
		this.logger = twineLogger.getLogger();

		integerParamMap = doubleArrayToStringLong(integerParamArray);
		stringParamMap = doubleArrayToStringString(stringParamArray);
		booleanParamMap = doubleArrayToStringBoolean(booleanParamArray);

		Map<String, String> commandLineParams = processCommandLine(args);

		if (commandLineParams == null) {
			rootDir = null;
			success = false;
			return;
		}

		if (!updateLogLevel(commandLineParams)) {
			rootDir = null;
			success = false;
			return;
		}

		String rootDirname = commandLineParams.get("rootdir");

		Map<String, String> configFileParams = null;

		File configFile = getConfigFile(rootDirname);

		if (configFile == null) {
			rootDir = null;
			success = false;
			return;
		}

		configFileParams = processConfigFile(configFile);

		if (configFileParams == null) {
			rootDir = null;
			success = false;
			return;
		}

		int loopLimit = 10;
		String altRootDirname;
		while ((--loopLimit) > 0 && configFileParams != null && ((altRootDirname = configFileParams.get("rootdir")) != null) ) {
			logger.fine("Switching to alternative root directory, " + altRootDirname);

			if (configFileParams.size() > 1) {
				logger.severe("Redirecting configuration file has more than one parameter");
				rootDir = null;
				success = false;
				return;
			}

			configFile = getConfigFile(configFile, altRootDirname);

			if (configFile != null) {
				configFileParams = processConfigFile(configFile);
			} else {
				configFileParams = null;
			}
		}

		if (loopLimit == 0) {
			logger.severe("Configuration file redirection exceeded loop limit");
			rootDir = null;
			success = false;
			return;		}

		if (configFileParams == null) {
			rootDir = null;
			success = false;
			return;
		}

		if (!updateLogLevel(configFileParams)) {
			rootDir = null;
			success = false;
			return;
		}

		File configFileAbsolute = configFile == null ? null : configFile.getAbsoluteFile();
		rootDir = configFileAbsolute == null ? null : configFileAbsolute.getParentFile();
		success = rootDir != null && rootDir.isDirectory();

		if (success) {
			for (Entry<String, String> entry : configFileParams.entrySet()) {
				map.put(entry.getKey(), entry.getValue());
			}
			for (Entry<String, String> entry : commandLineParams.entrySet()) {
				map.put(entry.getKey(), entry.getValue());
			}
		}
	}

	@Override
	public long getIntegerParameter(String name) {
		if (!integerParamMap.containsKey(name)) {
			logger.warning("Unknown integer parameter, " + name);
		}
		String value = map.get(name);
		if (value == null) {
			return integerParamMap.get(name).getB();
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e) {
		}
		logger.warning("Unable to process integer parameter, " + value + ", using default");
		return integerParamMap.get(name).getB();
	}

	@Override
	public String getStringParameter(String name) {
		if (!stringParamMap.containsKey(name)) {
			logger.warning("Unknown string parameter, " + name);
		}
		String value = map.get(name);
		if (value == null) {
			return stringParamMap.get(name).getB();
		}
		if (value.matches("^[a-zA-Z0-9_\\\\/]*$")) {
			return value;
		}
		logger.warning("Unable to process string parameter, " + value + ", using default");
		return stringParamMap.get(name).getB();
	}

	@Override
	public boolean getBooleanParameter(String name) {
		if (!booleanParamMap.containsKey(name)) {
			logger.warning("Unknown boolean parameter, " + name);
		}
		String value = map.get(name);
		if (value == null) {
			return booleanParamMap.get(name).getB();
		}
		if ("".equals(value)) {
			return true;
		}
		if ("true".equals(value.toLowerCase())) {
			return true;
		}
		if ("false".equals(value.toLowerCase())) {
			return false;
		}
		if ("1".equals(value)) {
			return true;
		}
		if ("0".equals(value)) {
			return false;
		}
		logger.warning("Unable to process boolean parameter, " + value + ", using default");
		return booleanParamMap.get(name).getB();
	}

	public boolean isSuccess() {
		return success;
	}

	public File getRootDir() {
		return rootDir;
	}

	public File getLogDir() {
		return isSuccess() ? new File(getRootDir(), "logs") : null;
	}

	public void printHelp() {
		System.out.println("Default command line arguments");
		printParamArray(booleanParamArray);
		printParamArray(stringParamArray);
		printParamArray(integerParamArray);
	}

	private void printParamArray(String[][] params) {
		for (String[] param : params) {
			String paramName = param[0];
			String paramDescription = param[1];
			String paramDefault = param[2];

			if ("rootdir".equals(param[0])) {
				paramDefault = "." + File.separator;
			}
			System.out.println(String.format("   -%-10s %-10s : %s", paramName, paramDefault, paramDescription));
		}
	}

	private Map<String, String> processCommandLine(String[] args) {
		Map<String, String> map = new HashMap<String, String>();
		int i = 0;
		while (i < args.length) {
			String arg = args[i];
			if (!arg.startsWith("-")) {
				logger.severe("Command line arguments must be of the form -parameter <value>, " + arg);
				return null;
			}

			arg = arg.substring(1).trim();
			String value = (i + 1) < args.length ? args[i + 1] : "";
			value = value.trim();

			if (value.startsWith("-")) {
				value = "";
			}
			map.put(arg, value);

			if (!"".equals(value)) {
				i++;
			}
			i++;
		}
		return map;
	}

	private Map<String, String> processConfigFile(File configFile) {
		Map<String, String> map = new HashMap<String, String>();

		FileInputStream in = null;
		try {
			in = new FileInputStream(configFile);

			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

			String rawLine;
			while ((rawLine = reader.readLine()) != null) {
				String line = rawLine;
				String[] split = line.split("#");
				if (split.length > 1) {
					line = split[0];
				}
				line = line.trim();
				if ("".equals(line)) {
					continue;
				}
				split = line.split("=");
				if (split.length == 1) {
					map.put(split[0].trim(), "");
					continue;
				}
				if (split.length == 2) {
					map.put(split[0].trim(), split[1].trim());
					continue;
				}
				logger.warning("Unable to process config file line, " + rawLine);
			}
		} catch (FileNotFoundException e) {
			logger.fine("No configuration file found");
			return map;
		} catch (IOException e) {
			logger.warning("Error reading config file, " + configFile + ", " + e.getMessage());
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		logger.fine("Processed configuration file, " + configFile);
		return map;
	}

	private File getConfigFile(String dirname) {
		return getConfigFile(null, dirname);
	}

	private File getConfigFile(File parentConfigFile, String dirname) {
		File configDir;

		if ("".equals(dirname)) {
			dirname = null;
		}

		if (dirname == null || !new File(dirname).isAbsolute()) {
			File parentConfigDirectory;
			if (parentConfigFile == null) {
				parentConfigDirectory = null;
			} else if (parentConfigFile.getParentFile() != null) {
				parentConfigDirectory = parentConfigFile.getParentFile();
			} else {
				parentConfigDirectory = parentConfigFile.getAbsoluteFile().getParentFile();
			}

			if (parentConfigDirectory == null) {
				if (dirname == null) {
					configDir = null;
				} else {
					configDir = new File(dirname);
				}
			} else {
				if (dirname == null) {
					configDir = parentConfigDirectory;
				} else {
					configDir = new File(parentConfigDirectory, dirname);
				}
			}
		} else {
			configDir = new File(dirname);
		}

		if (configDir == null) {
			return new File(configFilename);
		}

		if (!configDir.exists()) {
			configDir.mkdirs();
		}

		if (!configDir.isDirectory()) {
			logger.severe("Root directory cannot be created or is not a directory, " + dirname);
			return null;
		}

		return new File(configDir, configFilename);
	}

	private boolean updateLogLevel(Map<String, String> args) {
		String consoleLevelString = args.get("consoleloglevel");
		if (consoleLevelString != null) {
			try {
				Level consoleLevel = Level.parse(consoleLevelString.toUpperCase());
				twineLogger.setConsoleLevel(consoleLevel);
				logger.fine("Console log level set to " + consoleLevel);
			} catch (IllegalArgumentException e) {
				logger.severe("Unable to parse console log level, " + consoleLevelString);
				return false;
			}
		}
		String fileLevelString = args.get("fileloglevel");
		if (fileLevelString != null) {
			try {
				Level fileLevel = Level.parse(fileLevelString.toUpperCase());
				twineLogger.setFileLevel(fileLevel);
				logger.fine("File log level set to " + fileLevel);
			} catch (IllegalArgumentException e) {
				logger.severe("Unable to parse file log level, " + fileLevelString);
				return false;
			}
		}
		return true;
	}

	private Map<String, Pair<String, Long>> doubleArrayToStringLong(String[][] array) {
		Map<String, Pair<String, Long>> map = new HashMap<String, Pair<String, Long>>();
		for (String[] triplet : array) {
			Long def = null;
			try {
				def = Long.parseLong(triplet[2]);
			} catch (NumberFormatException e) {
				logger.severe("Unable to process internal default for " + triplet[0]);
				def = 0L;
			}
			Pair<String, Long> pair = new Pair<String, Long>(triplet[1], def);
			map.put(triplet[0], pair);
		}
		return map;
	}

	private Map<String, Pair<String, String>> doubleArrayToStringString(String[][] array) {
		Map<String, Pair<String, String>> map = new HashMap<String, Pair<String, String>>();
		for (String[] triplet : array) {
			String def = triplet[2];
			Pair<String, String> pair = new Pair<String, String>(triplet[1], def);
			map.put(triplet[0], pair);
		}
		return map;
	}

	private Map<String, Pair<String, Boolean>> doubleArrayToStringBoolean(String[][] array) {
		Map<String, Pair<String, Boolean>> map = new HashMap<String, Pair<String, Boolean>>();
		for (String[] triplet : array) {
			Boolean def = null;
			try {
				def = Boolean.parseBoolean(triplet[2]);
			} catch (NumberFormatException e) {
				logger.severe("Unable to process internal default for " + triplet[0]);
				def = false;
			}
			Pair<String, Boolean> pair = new Pair<String, Boolean>(triplet[1], def);
			map.put(triplet[0], pair);
		}
		return map;
	}
}
