package ru.dolika.doublefinder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
		if (checksum == null) {
			try {
				calculateChecksum();
			} catch (NoSuchAlgorithmException | IOException e) {
				e.printStackTrace();
			}
		}
		return Optional.ofNullable(checksum);
	}

	public void calculateChecksum()
			throws IOException, NoSuchAlgorithmException {
		if (file.exists()) {
			FileInputStream fis = new FileInputStream(file);
			try {
				int computeLength = (1024 * 1024);
				if (computeLength > file.length()) {
					computeLength = (int) file.length();
				}
				byte[] fileBytes = new byte[(int) computeLength];
				int dataLength = fis.read(fileBytes);
				MessageDigest sha = MessageDigest.getInstance("SHA-1");
				byte[] checksum = sha
						.digest(Arrays.copyOf(fileBytes, dataLength));
				this.checksum = 0L;
				for (int i = 0; i < checksum.length; i++) {
					byte chs = checksum[i];
					this.checksum += ((long) (chs & 0xFF)) << (2 * i);
				}
			} finally {
				fis.close();
			}
		}
	}

	public File getFile() {
		return file;
	}

	@Override
	public String toString() {
		return file.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChecksumFile) {
			return this.file.equals(((ChecksumFile) o).file);
		} else {
			return this.file.equals(o);
		}
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	public void delete() {
		try {
			DoubletMain.tempPath.toFile().mkdirs();
			Files.move(file.toPath(),
					DoubletMain.tempPath.resolve(file.getParentFile().getName()
							+ "-" + file.getName()),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
