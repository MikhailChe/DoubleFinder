package ru.dolika.doublefinder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;

public class DoubletMain {

	public static void main(String... args) {

		Hashtable<Long, List<ChecksumFile>> duplicateTable = new Hashtable<>();
		List<File> roots = Arrays.asList(new File("C:\\"));

		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.showOpenDialog(null);
		if (chooser.getSelectedFile() != null) {
			roots = Arrays.asList(chooser.getSelectedFiles());
		}
		System.out.println("Creating flat structure");
		List<ChecksumFile> files = Collections.synchronizedList(new ArrayList<ChecksumFile>());
		for (File root : roots)
			createFlat(files, root);
		System.out.println("Now, we're going to sort. That's the hardest thing, beacause we're sorting based on hash");

		{
			AtomicInteger integer = new AtomicInteger();
			ProgressMonitor monitor = new ProgressMonitor(null, "Ждём", "Распихиавем файлы по хэшам", 0, files.size());
			for (ChecksumFile f : files) {
				duplicateTable.putIfAbsent(f.getChecksum().orElse(-1L), new ArrayList<ChecksumFile>());
				duplicateTable.get(f.getChecksum().orElse(-1L)).add(f);
				monitor.setProgress(integer.incrementAndGet());
				if (monitor.isCanceled())
					return;
			}
			monitor.close();
		}
		{
			List<List<ChecksumFile>> duplicates = duplicateTable
					.values()
					.stream()
					.filter(f -> f.size() > 1)
					.filter(f -> {
						if (f.isEmpty())
							return false;
						File file = f.get(0).getFile();
						if (!file.exists())
							return false;
						if (file.length() < 500 * 1024)
							return false;
						return true;
					})
					.collect(Collectors.toList());
			for (List<ChecksumFile> dup : duplicates) {
				if (dup != null) {
					int status = new DealWithIt(dup).showDialog();
					if (status == JOptionPane.CANCEL_OPTION)
						break;
				}
			}
		}
	}

	public static void createFlat(List<ChecksumFile> files, File f) {
		if (f.isDirectory()) {
			try {
				for (File item : f.listFiles()) {
					if (item.isHidden() || item.getName().startsWith("."))
						continue;
					createFlat(files, item);
				}
			} catch (NullPointerException e) {

			}
		} else {
			try {
				if (f.isHidden() || f.getName().startsWith("."))
					return;

				files.add(new ChecksumFile(f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
