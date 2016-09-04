package ru.dolika.doublefinder;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.ProgressMonitor;

public class DoubletMain {
	static public List<File> roots = Arrays.asList(new File("C:\\"));
	static public Hashtable<Long, List<ChecksumFile>> duplicateTable = new Hashtable<>();
	static public List<ChecksumFile> files = Collections
			.synchronizedList(new ArrayList<ChecksumFile>());
	static public Path tempPath = new File("D:/temp/").toPath();

	final static public int MIN_SIZE = 2;

	public static void main(String... args) {

		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(null);
		if (chooser.getSelectedFile() != null) {
			roots = Arrays.asList(chooser.getSelectedFiles());
		}
		ProgressMonitor p = new ProgressMonitor(null, "Ищем все файлы",
				"больше " + MIN_SIZE + " байт.", 0, 1);
		p.setProgress(0);

		for (File root : roots)
			createFlat(files, root, MIN_SIZE);

		p.setProgress(1);
		p.setMinimum(0);
		p.setMaximum(files.size());

		p.setNote(
				"Теперь пора разобраться с ними.\r\nЭто самая трудоёмкая часть, нужно посчитать хэши всех файлов");

		{
			int iterator = 0;
			for (ChecksumFile f : files) {
				p.setProgress(iterator++);
				duplicateTable.putIfAbsent(f.getChecksum().orElse(-1L),
						new ArrayList<ChecksumFile>());
				duplicateTable.get(f.getChecksum().orElse(-1L)).add(f);

			}
		}
		p.setProgress(files.size());
		
		System.out.println("OK. GO");
		files.forEach(System.out::println);
		duplicateTable.values().forEach(System.out::println);
		duplicateTable.values().stream().filter(f -> f.size() > 1)
				.filter(f -> f.get(0).getFile().exists()).forEach(dup -> {
					if (dup != null) {
						new DealWithIt(dup).showDialog();
					}
				});

	}

	public static void createFlat(List<ChecksumFile> files, File f,
			int minsize) {
		if (f.isDirectory()) {
			try {
				for (File item : f.listFiles()) {
					createFlat(files, item, minsize);
				}
			} catch (NullPointerException e) {

			}
		} else {

			if (f.length() > minsize) {
				try {
					files.add(new ChecksumFile(f));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static List<ChecksumFile> findDoubleGroup(ChecksumFile f) {
		return duplicateTable.values().stream().filter(a -> a.contains(f))
				.findFirst().orElse(new ArrayList<ChecksumFile>());
	}

	public static List<ChecksumFile> findInSameDirectory(
			ChecksumFile selectedValue) {
		return files.stream()
				.filter(fil -> fil.getFile().getParentFile()
						.equals(selectedValue.getFile().getParentFile()))
				.collect(Collectors.toList());
	}
}
