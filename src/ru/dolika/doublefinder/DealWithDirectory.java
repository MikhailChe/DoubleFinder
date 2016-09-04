package ru.dolika.doublefinder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class DealWithDirectory extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1809508433710876427L;
	private JList<ChecksumFile> list;
	private DefaultListModel<ChecksumFile> listModel;
	private int exitStatus = JOptionPane.CLOSED_OPTION;

	private DealWithDirectory(JDialog parent) {
		super(parent);
		setTitle("Удаление дубликатов");
		getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		Runnable cancel = () -> {
			this.exitStatus = JOptionPane.CANCEL_OPTION;
			this.setVisible(false);
		};
		Runnable close = () -> {
			this.exitStatus = JOptionPane.CLOSED_OPTION;
			this.setVisible(false);
		};
		Runnable ok = () -> {
			this.exitStatus = JOptionPane.OK_OPTION;
			this.setVisible(false);
		};
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int status = JOptionPane.showConfirmDialog(
						DealWithDirectory.this,
						"Вы действительно хотите удалить\r\n"
								+ "дубликаты из других директорий\r\n"
								+ "и оставить только копии в этой директории");
				switch (status) {
				case JOptionPane.YES_OPTION: {
					List<ChecksumFile> filesInCurrentDirectory = list
							.getSelectedValuesList();
					for (ChecksumFile f : filesInCurrentDirectory) {
						List<ChecksumFile> files = DoubletMain
								.findDoubleGroup(f);
						for (ChecksumFile doub : files) {
							// If file in double group is not selected
							if (!filesInCurrentDirectory.contains(doub)) {
								doub.delete();
							}
						}
						Iterator<ChecksumFile> filesIterator = files.iterator();
						while (filesIterator.hasNext()) {
							ChecksumFile doub = filesIterator.next();
							if (!doub.equals(f)) {
								filesIterator.remove();
							}
						}
					}
					ok.run();
					break;
				}
				case JOptionPane.NO_OPTION: {

					break;
				}
				case JOptionPane.CANCEL_OPTION: {
					cancel.run();
					break;
				}
				}

			}
		});
		panel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				close.run();
			}
		});
		panel.add(cancelButton);

		list = new JList<>();
		listModel = new DefaultListModel<>();
		list.setModel(listModel);
		getContentPane().add(list, BorderLayout.CENTER);

		setModalityType(ModalityType.TOOLKIT_MODAL);
		pack();
	}

	public DealWithDirectory(List<ChecksumFile> list, JDialog parent) {
		this(parent);
		for (ChecksumFile element : list)
			listModel.addElement(element);
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
