package ru.dolika.doublefinder;

import java.awt.Dialog.ModalExclusionType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

public class DoubletMain {
	static JFrame frame;

	public static void main(String... args) {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}

		frame = new JFrame();
		frame.setUndecorated(true);
		frame.setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		searchLocateDestroy();
		frame.dispose();
	}

	public static void searchLocateDestroy() {

		Hashtable<Long, List<ChecksumFile>> duplicateTable = new Hashtable<>();
		List<File> roots = new ArrayList<>();

		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int chooserOption = chooser.showOpenDialog(frame);
		if (chooserOption == JFileChooser.CANCEL_OPTION || chooserOption == JFileChooser.ERROR_OPTION) {
			frame.dispose();
			return;
		}
		if (chooser.getSelectedFile() == null || chooser.getSelectedFiles().length == 0)
			return;

		roots = Arrays.asList(chooser.getSelectedFiles());
		AtomicInteger integer = new AtomicInteger();
		ProgressMonitor monitor = new ProgressMonitor(frame, "Ожидаем", "Распихиавем файлы по хэшам", 0, 100);
		monitor.setNote("Проходимся по всем файлам в папках. Создаём базу данных файлов.");
		monitor.setMillisToDecideToPopup(1);
		monitor.setMillisToPopup(1);
		monitor.setMaximum(roots.size() + 1);
		List<ChecksumFile> files = Collections.synchronizedList(new ArrayList<ChecksumFile>());
		System.out.println("Creating flat structure");
		for (File root : roots) {
			monitor.setProgress(integer.incrementAndGet());
			createFlat(files, root, monitor, integer);
		}
		monitor.setProgress(integer.incrementAndGet());

		System.out.println("Now, we're going to sort. That's the hardest thing, beacause we're sorting based on hash");
		{
			integer.set(0);
			monitor.setMaximum(files.size());
			monitor.setNote("Сравниваем содержимое файлов. Это может занять некоторое время");
			for (ChecksumFile f : files) {
				monitor.setProgress(integer.incrementAndGet());
				duplicateTable.putIfAbsent(f.getChecksum().orElse(-1L), new ArrayList<ChecksumFile>());
				duplicateTable.get(f.getChecksum().orElse(-1L)).add(f);
				if (monitor.isCanceled()) {
					frame.dispose();
					return;
				}
			}
		}
		monitor.close();

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
				if (dup == null)
					continue;
				int status = new DealWithIt(dup, frame).showDialog();
				if (status != JOptionPane.OK_OPTION) {
					frame.dispose();
					break;
				}
			}
			if (duplicates.isEmpty()) {
				JOptionPane.showMessageDialog(frame, "Дубликатов не найдено!");
			}
		}
	}

	public static void createFlat(List<ChecksumFile> files, File f, ProgressMonitor monitor, AtomicInteger integer) {
		if (f.isDirectory()) {
			File[] fileList = f.listFiles();
			monitor.setMaximum(monitor.getMaximum() + fileList.length);
			try {
				for (File item : fileList) {
					monitor.setProgress(integer.incrementAndGet());
					if (item.isHidden() || item.getName().startsWith("."))
						continue;
					createFlat(files, item, monitor, integer);
				}
			} catch (NullPointerException e) {
				// TODO: Nothing to do
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
