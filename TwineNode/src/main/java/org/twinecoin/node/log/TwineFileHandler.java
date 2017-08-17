/**
 *               Copyright (c) 2017 Twinecoin Developers
 * The file is licenced under the MIT software license, see LICENCE
 * or http://www.opensource.org/licenses/mit-license.php.
 */
package org.twinecoin.node.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class TwineFileHandler extends Handler {

	private final Object INITIAL = new Object();
	private final Object OPENED = new Object();
	private final Object CLOSED = new Object();

	private final AtomicReference<Object> dirSetup;
	private final ConcurrentLinkedQueue<String> recordStringQueue;
	private int count;
	private long size;
	private File logDir;
	private String prefix;

	private long remaining;
	private FileOutputStream out;
	private BufferedWriter writer;
	private long lastTimeUpdate;
	private long timeUpdatePeriod;
	private long lastFlush;
	private long minFlushPeriod;
	private String dateFormatString;
	private String error;

	public TwineFileHandler() {
		this.dirSetup = new AtomicReference<Object>(INITIAL);

		// Set by setLogDir protected by dirSetup
		this.logDir = null;
		this.prefix = null;
		this.count = 0;
		this.size = 0;

		// Local thread variables
		this.error = null;
		this.out = null;
		this.remaining = 0;
		this.lastTimeUpdate = 0;
		this.timeUpdatePeriod = 60000;
		this.lastFlush = 0;
		this.minFlushPeriod = 5000;
		this.dateFormatString = "HH:mm (dd MMM yyyy)";
		this.recordStringQueue = new ConcurrentLinkedQueue<String>();
	}

	public void setLogDir(File logDir, String prefix, int count, long size) throws FileNotFoundException {
		if (dirSetup.get() != INITIAL) {
			throw new IllegalStateException("Log directory should not be set more than once");
		}
		this.logDir = logDir;
		this.prefix = prefix;
		this.count = count;
		this.size = size;
		if (!this.logDir.isDirectory()) {
			throw new FileNotFoundException("Log directory is not a directory, " + logDir);
		}
		if (!dirSetup.compareAndSet(INITIAL, OPENED)) {
			throw new IllegalStateException("Log directory should not be set more than once");
		}
	}

	@Override
	public void publish(LogRecord record) {
		Object state = dirSetup.get();
		if (state == INITIAL || state == OPENED) {
			if (System.currentTimeMillis() > lastTimeUpdate + timeUpdatePeriod) {
				printTimeUpdateMessage();
			}
			Formatter formatter = getFormatter();
			String recordString;
			if (formatter == null) {
				recordString = record.getMessage();
			} else {
				recordString = formatter.format(record);
			}
			try {
				writeString(recordString);
			} catch (IOException e) {
				error("Failed to write to log file");
			}
		}
	}

	@Override
	public void flush() {
		Object state = dirSetup.get();
		if (state == OPENED) {
			try {
				if (writer != null) {
					writer.flush();
				}
			} catch (IOException e) {
				error("Failed to flush data, " + e.getMessage());
			}
		}
	}

	@Override
	public void close() throws SecurityException {
		boolean stateUpdated = false;

		while (!stateUpdated) {
			Object state = dirSetup.get();
			if (state == INITIAL) {
				stateUpdated = dirSetup.compareAndSet(state, CLOSED);
			} else if (state == OPENED) {
				try {
					if (writerChecks()) {
						writeOpenCloseMessage(false, true);
						flush();
						writer.close();
					}
				} catch (IOException e) {
					error("Unable to write closing messages to file, " + e.getMessage());
				} finally {
					writer = null;
					try {
						if (out != null) {
							out.close();
						}
					} catch (IOException e) {
						error("Failed to close file output stream, " + e.getMessage());
					} finally {
						out = null;
						stateUpdated = dirSetup.compareAndSet(state, CLOSED);
						if (!stateUpdated) {
							error("Unexpected state change away from OPENED");
						}
					}
				}
			} else if (state == CLOSED) {
				stateUpdated = true;
			} else {
				stateUpdated = true;
				error("Unknown directory setup state, " + state);
			}
		}
	}

	private void writeString(String string) throws IOException {
		if (error != null) {
			return;
		}
		Object state = dirSetup.get();
		if (state == OPENED) {
			if (writerChecks()) {
				this.remaining -= string.length();
				writer.write(string);
				if (System.currentTimeMillis() - lastFlush > minFlushPeriod) {
					writer.flush();
					lastFlush = System.currentTimeMillis();
				}
			}
		} else if (state == INITIAL) {
			recordStringQueue.add(string);
		}
	}

	private boolean writerChecks() throws IOException {
		if (this.out == null || this.writer == null || this.remaining < 0) {
			updateWriter();
		}
		if (writer == null) {
			return false;
		}
		if (!recordStringQueue.isEmpty()) {
			String message;
			while ((message = recordStringQueue.poll()) != null) {
				writer.write(message);
				this.remaining -= message.length();
			}
		}
		return true;
	}

	private void error(String message) {
		this.error = message;
		System.err.println(message);
	}

	private void updateWriter() {
		if (this.dirSetup.get() != OPENED) {
			return;
		}
		if (this.writer != null) {
			writeOpenCloseMessage(false, true);
			try {
				this.writer.close();
			} catch (IOException e) {
				error("File log handler unable to close writer, " + e.getMessage());
			} finally {
				this.writer = null;
			}
		}
		if (this.out != null) {
			try {
				this.out.close();
			} catch (IOException e) {
				error("File log handler unable to close output stream, " + e.getMessage());
			} finally {
				this.out = null;
			}
		}

		updateDirectory();

		TreeMap<Integer, File> tree = getLogFiles();

		Entry<Integer, File> firstEntry = tree.firstEntry();

		boolean reopen = false;
		int next;
		if (firstEntry != null) {
			if (firstEntry.getValue().length() < (size * 3) / 4) {
				next = firstEntry.getKey();
				reopen = true;
			} else {
				next = firstEntry.getKey() + 1;
			}
		} else {
			next = 0;
		}

		String nextFilename = getFilename(next);

		File nextFile = new File(logDir, nextFilename);

		try {
			this.out = new FileOutputStream(nextFile, true);
		} catch (FileNotFoundException e) {
			error("File logger unable to open new output stream");
			return;
		}

		this.writer = new BufferedWriter(new OutputStreamWriter(this.out, StandardCharsets.UTF_8));

		this.remaining = size;

		writeOpenCloseMessage(true, reopen);
	}

	private void writeOpenCloseMessage(boolean open, boolean newline) {
		String openString = open ? "Opened" : "Closed";
		try {
			if (newline) {
				writer.write("\n");
			}
			writer.write("****************************************************************************\n");
			writer.write("                                Log File " + openString + "\n");
			writer.write("                              " + getDateString() + "\n");
			writer.write("****************************************************************************\n");
		} catch (IOException e) {
			error("Failed to write file open message to log file, " + e.getMessage());
		}
	}

	private void printTimeUpdateMessage() {
		lastTimeUpdate = System.currentTimeMillis();
		try {
			writeString("\n");
			writeString(getDateString());
			writeString("\n");
			writeString("\n");
			flush();
		} catch (IOException e) {
			error("Failed to write time update message to log file, " + e.getMessage());
		}
	}

	private String getDateString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
		return dateFormat.format(new Date());
	}

	private void updateDirectory() {
		if (count < 0) {
			return;
		}
		TreeMap<Integer, File> tree = getLogFiles();

		long remaining = (count - 1) * size;
		for (Entry<Integer, File> entry : tree.entrySet()) {
			remaining -= entry.getValue().length();
			if (remaining < 0) {
				entry.getValue().delete();
			}
		}
	}

	private String getFilename(int index) {
		return prefix + "." + index;
	}

	private TreeMap<Integer, File> getLogFiles() {
		TreeMap<Integer, File> tree = new TreeMap<Integer, File>(Collections.reverseOrder());

		String[] filenames = logDir.list();
		for (String filename : filenames) {
			if (!filename.startsWith(prefix)) {
				continue;
			}
			if (filename.length() < prefix.length() + 2) {
				continue;
			}
			if (filename.charAt(prefix.length()) != '.') {
				continue;
			}
			String indexString = filename.substring(prefix.length() + 1);
			int index;
			try {
				index = Integer.parseInt(indexString);
			} catch (NumberFormatException e) {
				continue;
			}
			tree.put(index, new File(logDir, filename));
		}
		return tree;
	}
}
