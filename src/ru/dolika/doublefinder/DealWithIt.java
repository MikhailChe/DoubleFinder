package ru.dolika.doublefinder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class DealWithIt extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 821612509658672200L;
	int exitStatus = JOptionPane.CLOSED_OPTION;

	private DealWithIt() {
		super((Window) null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(5, 5));

		JPanel okCancelPanel = new JPanel();
		getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
		okCancelPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DealWithIt.this.exitStatus = JOptionPane.OK_OPTION;
				setVisible(false);
				dispose();
			}
		});
		okCancelPanel.add(okButton);

		JButton cancelButton = new JButton("Отмена");
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DealWithIt.this.exitStatus = JOptionPane.CANCEL_OPTION;
				setVisible(false);
				dispose();
			}
		});
		okCancelPanel.add(cancelButton);

		JPanel removeButtonsPanel = new JPanel();
		getContentPane().add(removeButtonsPanel, BorderLayout.WEST);
		removeButtonsPanel.setLayout(new BoxLayout(removeButtonsPanel, BoxLayout.Y_AXIS));

		JButton removeSelected = new JButton("Удалить выделенные");
		removeSelected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<ChecksumFile> chs = new ArrayList<>();
				int[] indc = DealWithIt.this.list.getSelectedIndices();
				for (int i = 0; i < indc.length; i++) {
					chs.add(DealWithIt.this.list.getModel().getElementAt(indc[i]));
				}

				String namesOfFiles = chs
						.stream()
						.map(a -> a.getFile().getAbsolutePath())
						.collect(Collectors.joining("\r\n"));
				int option = JOptionPane
						.showConfirmDialog(DealWithIt.this,
								"Вы действительно хотите безвозвратно удалить следующие файлы?\r\n" + namesOfFiles,
								"Удалить файл", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.NO_OPTION)
					return;

				for (ChecksumFile f : chs) {
					f.delete();
					System.out.println("Removing this: " + f);
					DealWithIt.this.listModel.removeElement(f);
				}
			}
		});
		removeButtonsPanel.add(removeSelected);

		JButton removeUnselected = new JButton("Удалить все кроме выделенных");
		removeUnselected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Removing all but selected");

				System.out.println("Creating new ArrayList");
				List<ChecksumFile> chs = new ArrayList<>();

				System.out.println("Creating new IntStream and collecting it to List<Integer>indcs");
				List<Integer> indcs = IntStream
						.range(0, DealWithIt.this.listModel.size())
						.boxed()
						.collect(Collectors.toList());
				System.out
						.println("indcs now looks like this : "
								+ indcs.stream().map(a -> "" + a).collect(Collectors.joining(", ")));

				System.out.println("Now we're removing indexes that are selected");
				for (Integer indx : DealWithIt.this.list.getSelectedIndices()) {
					System.out.println("removing " + indx);
					indcs.remove(indx);
				}
				System.out.println("All done. Now it looks like this:");

				System.out
						.println("indcs now looks like this : "
								+ indcs.stream().map(a -> "" + a).collect(Collectors.joining(", ")));

				System.out.println("Now we're going to add appropriate checksumFiles to it");
				for (Integer idx : indcs) {
					chs.add(DealWithIt.this.listModel.getElementAt(idx));
				}
				chs.stream().forEach(System.out::println);

				System.out.println("Now, we're just removing them. THat's it");

				String namesOfFiles = chs
						.stream()
						.map(a -> a.getFile().getAbsolutePath())
						.collect(Collectors.joining("\r\n"));
				int option = JOptionPane
						.showConfirmDialog(DealWithIt.this,
								"Вы действительно хотите безвозвратно удалить следующие файлы?\r\n" + namesOfFiles,
								"Удалить файл", JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.NO_OPTION)
					return;
				for (ChecksumFile f : chs) {
					f.delete();
					DealWithIt.this.listModel.removeElement(f);
					System.out.println("Removing : " + f);
				}
			}
		});
		removeButtonsPanel.add(removeUnselected);

		this.list = new JList<>();
		getContentPane().add(this.list, BorderLayout.CENTER);

		setModalityType(ModalityType.TOOLKIT_MODAL);
		pack();
		setLocationRelativeTo(null);
	}

	public DealWithIt(List<ChecksumFile> files) {
		this();
		this.listModel = new DefaultListModel<>();
		this.list.setModel(this.listModel);
		for (ChecksumFile f : files) {
			this.listModel.addElement(f);
		}
		pack();
	}

	@Override
	public void dispose() {
		super.dispose();
		synchronized (this) {
			this.notifyAll();
		}
	}

	@Override
	public void setVisible(boolean v) {
		super.setVisible(v);
		synchronized (this) {
			this.notifyAll();
		}
	}

	JList<ChecksumFile> list;
	DefaultListModel<ChecksumFile> listModel;

	public int showDialog() {
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		while (isVisible()) {
			synchronized (this) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return exitStatus;
	}

}