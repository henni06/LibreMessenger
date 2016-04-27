package org.briarproject;

import org.briarproject.api.crypto.SecretKey;
import org.briarproject.api.db.DatabaseConfig;

import java.io.File;

public class TestDatabaseConfig implements DatabaseConfig {

	private final File dir;
	private final long maxSize;
	private volatile SecretKey key = new SecretKey(new byte[SecretKey.LENGTH]);

	public TestDatabaseConfig(File dir, long maxSize) {
		this.dir = dir;
		this.maxSize = maxSize;
	}

	public boolean databaseExists() {
		if (!dir.isDirectory()) return false;
		File[] files = dir.listFiles();
		return files != null && files.length > 0;
	}

	public File getDatabaseDirectory() {
		return dir;
	}

	public void setEncryptionKey(SecretKey key) {
		this.key = key;
	}

	public SecretKey getEncryptionKey() {
		return key;
	}

	public long getMaxSize() {
		return maxSize;
	}
}
