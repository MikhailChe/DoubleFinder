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
	private int exitStatus = JOptionPane.CLOSED_OPTION;

	private DealWithIt() {
		super((Window) null);

		Runnable ok = () -> {
			exitStatus = JOptionPane.OK_OPTION;
			setVisible(false);
			dispose();
		};
		Runnable cancel = () -> {
			exitStatus = JOptionPane.CANCEL_OPTION;
			setVisible(false);
			dispose();
		};
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout(5, 5));

		JPanel okCancelPanel = new JPanel();
		getContentPane().add(okCancelPanel, BorderLayout.SOUTH);
		okCancelPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JButton okButton = new JButton("OK");
		okButton.addActionListener((e) -> {
			ok.run();
		});
		okCancelPanel.add(okButton);

		JButton cancelButton = new JButton("Отмена");
		cancelButton.addActionListener((e) -> {
			cancel.run();
		});
		okCancelPanel.add(cancelButton);

		JPanel removeButtonsPanel = new JPanel();
		getContentPane().add(removeButtonsPanel, BorderLayout.WEST);
		removeButtonsPanel
				.setLayout(new BoxLayout(removeButtonsPanel, BoxLayout.Y_AXIS));

		JButton removeSelected = new JButton("Удалить выделенные");
		removeSelected.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				List<ChecksumFile> chs = list.getSelectedValuesList();
				for (ChecksumFile f : chs) {
					f.delete();
					System.out.println("Removing this: " + f);
					listModel.removeElement(f);
				}

			}
		});
		removeButtonsPanel.add(removeSelected);

		JButton removeUnselected = new JButton("Удалить все кроме выделенных");
		removeUnselected.addActionListener((e) -> {

			if (list.getSelectedIndices().length == 1) {
				List<ChecksumFile> allInDirectory = DoubletMain
						.findInSameDirectory(list.getSelectedValue());
				if (allInDirectory.size() > 2) {
					int request = JOptionPane.showConfirmDialog(DealWithIt.this,
							"Я тут нашел ещё несколько дубликатов\r\n"
									+ "в той же самой директории.\r\n"
									+ "Если хочешь, я могу показать их всех и ты сам выберешь\r\n"
									+ "все файлы, которые нужно оставить только в этой директории.\r\n"
									+ "ОК?",
							"Погоди ка", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE);
					switch (request) {
					case JOptionPane.YES_OPTION: {
						JOptionPane.showConfirmDialog(this, "За работу",
								"ОК, за работу", JOptionPane.OK_OPTION);
						new DealWithDirectory(allInDirectory, this)
								.showDialog();
						ok.run();
						break;
					}
					case JOptionPane.NO_OPTION: {
						removeUnselected();
						break;
					}
					case JOptionPane.CANCEL_OPTION: {
						cancel.run();
					}
					}
				} else {
					removeUnselected();
				}
			}
		});
		removeButtonsPanel.add(removeUnselected);

		list = new JList<>();
		getContentPane().add(list, BorderLayout.CENTER);

		setModalityType(ModalityType.TOOLKIT_MODAL);
		pack();

	}

	private void removeUnselected() {
		List<ChecksumFile> chs = new ArrayList<>();
		List<Integer> indcs = IntStream.range(0, listModel.size()).boxed()
				.collect(Collectors.toList());
		for (Integer indx : list.getSelectedIndices()) {
			indcs.remove((Integer) indx);
		}
		for (Integer idx : indcs) {
			chs.add(listModel.getElementAt(idx));
		}
		chs.stream().forEach(System.out::println);
		for (ChecksumFile f : chs) {
			f.delete();
			listModel.removeElement(f);
		}
	}

	public DealWithIt(List<ChecksumFile> files) {
		this();
		listModel = new DefaultListModel<>();
		list.setModel(listModel);
		for (ChecksumFile f : files) {
			listModel.addElement(f);
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

	private JList<ChecksumFile> list;
	private DefaultListModel<ChecksumFile> listModel;

	public int showDialog() {
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