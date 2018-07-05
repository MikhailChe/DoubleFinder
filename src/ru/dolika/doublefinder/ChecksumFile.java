package ru.dolika.doublefinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Optional;

public class ChecksumFile {
	private final File file;
	private Long checksum = null;

	public ChecksumFile(File file) {
		if (file == null) {
			throw new NullPointerException();
		}
		this.file = file;
	}

	public Optional<Long> getChecksum() {
		if (this.checksum == null) {
			try {
				calculateChecksum();
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace();
			}
		}
		return Optional.ofNullable(this.checksum);
	}

	public void calculateChecksum() throws IOException, NoSuchAlgorithmException {
		if (this.file.exists()) {
			try (FileInputStream fis = new FileInputStream(this.file)) {
				int computeLength = (1024 * 1024);
				if (computeLength > this.file.length()) {
					computeLength = (int) this.file.length();
				}
				byte[] fileBytes = new byte[computeLength];
				int dataLength = fis.read(fileBytes);
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				byte[] checksum = sha.digest(Arrays.copyOf(fileBytes, dataLength));
				this.checksum = 0L;
				for (int i = 0; i < checksum.length; i++) {
					byte chs = checksum[i];
					this.checksum += ((long) (chs & 0xFF)) << (2 * i);
				}
			}
		}
	}

	public File getFile() {
		return this.file;
	}

	@Override
	public String toString() {
		return this.file.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChecksumFile) {
			return this.file.equals(((ChecksumFile) o).file);
		}
		return this.file.equals(o);
	}

	@Override
	public int hashCode() {
		return this.file.hashCode();
	}

	public void delete() {
		this.file.delete();
	}
}
