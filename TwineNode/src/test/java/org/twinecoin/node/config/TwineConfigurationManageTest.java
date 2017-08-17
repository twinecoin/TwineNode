/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
import org.twinecoin.node.log.TwineLogger;
import org.twinecoin.node.test.TestUtils.FakeHandler;

public class TwineConfigurationManageTest {

	private final static AtomicInteger id = new AtomicInteger(0);

	private final static String testRunDir = "testrundir";
	private final static String configFilename = "twinecoin.conf";

	@Test
	public void testCommandLineValidArgs() throws IOException {
		try {
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);

			TwineConfigurationManager config = getConfig(twineLogger);

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Unexpected default integer parameter value", 10, config.getIntegerParameter("logcount"));
			assertEquals("Unexpected default string value", null, config.getStringParameter("rootdir"));
			assertEquals("Unexpected default boolean value", false, config.getBooleanParameter("help"));

			config = getConfig(twineLogger,
					"-rootdir",
					testRunDir + File.separator + "test_rundir",
					"-logcount",
					"500",
					"-help");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Failed to set command line integer parameter", 500, config.getIntegerParameter("logcount"));
			assertEquals("Failed to set command line string value", testRunDir + File.separator + "test_rundir", config.getStringParameter("rootdir"));
			assertEquals("Failed to set command line boolean value", true, config.getBooleanParameter("help"));

			config = getConfig(twineLogger,
					"-rootdir",
					testRunDir + "/subdir",
					"-help",
					"-logcount",
					"500");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Failed to set command line integer parameter", 500, config.getIntegerParameter("logcount"));
			assertEquals("Failed to set command line string with forward slash", testRunDir + "/subdir", config.getStringParameter("rootdir"));
			assertEquals("Failed to set command line boolean", true, config.getBooleanParameter("help"));

			config = getConfig(twineLogger,
					"-rootdir",
					testRunDir + "\\subdir",
					"-help",
					"1",
					"-logcount",
					"500");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Failed to set command line integer parameter value", 500, config.getIntegerParameter("logcount"));
			assertEquals("Failed to set command line string value with back slash", testRunDir + "\\subdir", config.getStringParameter("rootdir"));
			assertEquals("Failed to set command line boolean value", true, config.getBooleanParameter("help"));
		} finally {
			File dir1 = new File(testRunDir, "subdir");
			if (dir1.isDirectory()) {
				dir1.delete();
			}
			File dir2 = new File(testRunDir, "test_rundir");
			if (dir2.isDirectory()) {
				dir2.delete();
			}
		}
	}

	@Test
	public void testCommandLineInvalidArgs() {
		try {
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			Logger logger = twineLogger.getLogger();
			FakeHandler fakeHandler = new FakeHandler();
			logger.addHandler(fakeHandler);
			fakeHandler.setLevel(Level.WARNING);

			TwineConfigurationManager config = getConfig(twineLogger,
					"-rootdir",
					testRunDir,
					"-help",
					"notaboolean",
					"-logcount",
					"500");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Invalid boolean value caused default value to change", false, config.getBooleanParameter("help"));
			assertEquals("Unexpected number of log messages", 1, fakeHandler.getMessageCount());

			fakeHandler.resetMessageCount();

			config = getConfig(twineLogger,
					"-rootdir",
					testRunDir + File.separator + "@@@@@",
					"-help",
					"-logcount",
					"500");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Invalid string value caused default value to change", null, config.getStringParameter("rootdir"));
			assertEquals("Unexpected number of log messages", 1, fakeHandler.getMessageCount());

			fakeHandler.resetMessageCount();

			config = getConfig(twineLogger,
					"-rootdir",
					testRunDir,
					"-help",
					"-logcount",
					"0x66");

			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Invalid integer value caused default value to change", 10, config.getIntegerParameter("logcount"));
			assertEquals("Unexpected number of log messages", 1, fakeHandler.getMessageCount());

			fakeHandler.resetMessageCount();
		} finally {
			File dir1 = new File(testRunDir, "@@@@@");
			if (dir1.isDirectory()) {
				dir1.delete();
			}
		}
	}

	@Test
	public void testValidBooleans() {
		TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
		twineLogger.setConsoleLevel(Level.OFF);

		Object[][] valid = new Object[][] {
				{"-help", true}, // i.e. no value
				{"tRuE", true},
				{"true", true},
				{"1", true},
				{"false", false},
				{"FalSe", false},
				{"0", false}
		};
		for (Object[] arg : valid) {
			String value = (String) arg[0];
			Boolean exp = (Boolean) arg[1];
			TwineConfigurationManager config = getConfig(twineLogger, "-notaparamname", value);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Unexpected boolean value when setting to " + value, exp, config.getBooleanParameter("notaparamname"));
		}
	}

	@Test
	public void testValidStrings() {
		TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
		twineLogger.setConsoleLevel(Level.OFF);

		Object[][] valid = new Object[][] {
				{"testing", "testing"},
				{"under_score", "under_score"},
				{"forward/slash", "forward/slash"},
				{"back\\slash", "back\\slash"}
		};
		for (Object[] arg : valid) {
			String value = (String) arg[0];
			String exp = (String) arg[1];
			TwineConfigurationManager config = getConfig(twineLogger, "-notaparamname", value);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Unexpected string value when setting to " + value, exp, config.getStringParameter("notaparamname"));
		}
	}

	@Test
	public void testValidIntegers() {
		TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
		twineLogger.setConsoleLevel(Level.OFF);

		Object[][] valid = new Object[][] {
				{"0", 0L},
				{"1", 1L},
				{"100000000000", 100000000000L},
				{"000000010", 10L},
		};
		for (Object[] arg : valid) {
			String value = (String) arg[0];
			Long exp = (Long) arg[1];
			TwineConfigurationManager config = getConfig(twineLogger, "-notaparamname", value);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Unexpected integer value when setting to " + value, exp, (Long) config.getIntegerParameter("notaparamname"));
		}
	}

	@Test
	public void testConfigFile() throws IOException {
		String subdir = "testConfigFile";
		try {
			createConfigFile(subdir, "logcount", "\t100 ", "fileloglevel", "  finest    ", "help", "");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, "-rootdir", testRunDir + File.separator + subdir);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Configuration file failed to set integer value", (Long) 100L, (Long) config.getIntegerParameter("logcount"));
			assertEquals("Configuration file failed to set string value", "finest", config.getStringParameter("fileloglevel"));
			assertEquals("Configuration file failed to set boolean value", true, config.getBooleanParameter("help"));
		} finally {
			deleteConfigFile(subdir);
		}
	}

	@Test
	public void testConfigFilePriority() throws IOException {
		String subdir = "testConfigFilePriority";
		try {
			createConfigFile(subdir, "logcount", "\t100 ", "fileloglevel", "  finest    ", "help", "");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, 
					"-rootdir", testRunDir + File.separator + subdir,
					"-logcount", "200",
					"-fileloglevel", "finer",
					"-help", "false"
					);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Command line failed to override configuration file for integer value", (Long) 200L, (Long) config.getIntegerParameter("logcount"));
			assertEquals("Command line failed to override configuration file for string value", "finer", config.getStringParameter("fileloglevel"));
			assertEquals("Command line failed to override configuration file for boolean value", false, config.getBooleanParameter("help"));
		} finally {
			deleteConfigFile(subdir);
		}
	}

	@Test
	public void testSecondaryConfigFile() throws IOException {
		String subdir = "testSecondaryConfigFile";
		String subdir2 = "testSecondaryConfigFile2";
		try {
			createConfigFile(subdir, "rootdir", ".." + File.separator + subdir2);
			createConfigFile(subdir2, "logcount", "200");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, 
					"-rootdir", testRunDir + File.separator + subdir
					);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Secondary log file parameter not set", (Long) 200L, (Long) config.getIntegerParameter("logcount"));
		} finally {
			deleteConfigFile(subdir);
			deleteConfigFile(subdir2);
		}
	}

	@Test
	public void testSecondaryConfigAbsoluteFile() throws IOException {
		String subdir = "testSecondaryConfigAbsoluteFile";
		String subdir2 = "testSecondaryConfigAbsoluteFile2";
		try {
			File subDirname = new File(testRunDir, subdir2).getAbsoluteFile();
			createConfigFile(subdir, "rootdir", subDirname.getAbsolutePath());
			createConfigFile(subdir2, "logcount", "200");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, 
					"-rootdir", testRunDir + File.separator + subdir
					);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Secondary log file parameter not set", (Long) 200L, (Long) config.getIntegerParameter("logcount"));
		} finally {
			deleteConfigFile(subdir);
			deleteConfigFile(subdir2);
		}
	}

	@Test
	public void testSecondaryConfigFileValidLoop() throws IOException {
		String subdir = "testSecondaryConfigFileValidLoop";
		int loops = 10;
		try {
			for (int i = 0; i < loops - 1; i++) {
				String current = subdir + i;
				String next = subdir + (i + 1);
				createConfigFile(current, "rootdir", ".." + File.separator + next);
			}
			createConfigFile(subdir + (loops - 1), "logcount", "200");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, 
					"-rootdir", testRunDir + File.separator + subdir + "1"
					);
			assertTrue("Configuration setup failure", config.isSuccess());
			assertEquals("Secondary log file parameter not set", (Long) 200L, (Long) config.getIntegerParameter("logcount"));
		} finally {
			for (int i = 0; i < loops; i++) {
				deleteConfigFile(subdir + i);
			}
		}
	}

	@Test
	public void testSecondaryConfigFileInvalidLoop() throws IOException {
		String subdir = "testSecondaryConfigFileInvalidLoop";
		int loops = 11;
		try {
			for (int i = 0; i < loops - 1; i++) {
				String current = subdir + i;
				String next = subdir + (i + 1);
				createConfigFile(current, "rootdir", ".." + File.separator + next);
			}
			createConfigFile(subdir + (loops - 1), "logcount", "200");
			TwineLogger twineLogger = new TwineLogger(Integer.toString(id.getAndIncrement()));
			twineLogger.setConsoleLevel(Level.OFF);
			TwineConfigurationManager config = getConfig(twineLogger, 
					"-rootdir", testRunDir + File.separator + subdir + "1"
					);
			assertFalse("Unexpected configuration setup success", config.isSuccess());
		} finally {
			for (int i = 0; i < loops; i++) {
				deleteConfigFile(subdir + i);
			}
		}
	}

	private static TwineConfigurationManager getConfig(TwineLogger twineLogger, String ... args) {
		return new TwineConfigurationManager(twineLogger, args);
	}

	private static void createConfigFile(String subdir, String ... params) throws IOException {
		File dir = new File(testRunDir, subdir);
		dir.mkdirs();
		File file = new File(dir, configFilename);
		FileOutputStream out = new FileOutputStream(file);
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
			writer.write("# comment\n");
			writer.write("\n");
			for (int i = 0; i < params.length; i += 2) {
				writer.write("   " + params[i] + "\t =" + params[i + 1] + "   \t# Line comment\n");
			}
			writer.write("  # footer comment  \n");
			writer.write("\n");
			writer.close();
		} finally {
			out.close();
		}
	}

	private static void deleteConfigFile(String subdir) {
		File dir = new File(testRunDir, subdir);
		File file = new File(dir, configFilename);
		file.delete();
		dir.delete();
	}
}
