package com.kaaphi.logviewer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.log4j.Logger;

import say.swing.JFontChooser;

import com.kaaphi.logviewer.FileUtil;
import com.kaaphi.logviewer.LogFile;
import com.kaaphi.logviewer.ui.filter.FiltersPanel;
import com.kaaphi.logviewer.ui.search.SearchPanel;
import com.kaaphi.logviewer.ui.search.SearchPanel.SearchPanelListener;
import com.kaaphi.logviewer.ui.search.SearchSession;
import com.kaaphi.logviewer.ui.util.MenuUtil;
import com.kaaphi.logviewer.ui.util.MenuUtil.MenuAction;
import com.kaaphi.logviewer.ui.util.RowHeader;

public class LogFileViewer extends JPanel {
	private static final Logger log = Logger.getLogger(LogFileViewer.class);
	
	private LogFileTableModel tableModel;
	private JTable table;
	private JFrame frame;
	private SearchPanel searchPanel;
	private SearchSession searchSession;
	private JPanel footer;
	private FooterDetails footerDetails;
	private LogViewerConfiguration config;
	private List<File> loadedFiles;
	private FiltersPanel filters;
	
	public LogFileViewer() throws Exception  {
		super(new BorderLayout());
		
		config = LogViewerConfiguration.getInstance();
		config.loadConfig();
		
		tableModel = new LogFileTableModel();
		
		footerDetails = new FooterDetails();
		tableModel.addTableModelListener(footerDetails);
		
		table = new JTable();
		table.setModel(tableModel);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setColumnSelectionAllowed(false);
		table.setAutoscrolls(false);
		table.setBackground(Color.WHITE);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		final NonEditableEditor editor =  new NonEditableEditor();
		table.setDefaultEditor(String.class, editor);
		table.setFont(config.font.get());
		table.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected,
					boolean hasFocus, int row, int col) {
				return super.getTableCellRendererComponent(table, value,
						isSelected, false, row, col);
			}
		});
		table.addKeyListener(new KeyListener() {
			private boolean control = false;
			
			public void keyTyped(KeyEvent paramKeyEvent) {}
			
			public void keyReleased(KeyEvent event) {
				switch(event.getKeyCode()) {
				case KeyEvent.VK_ESCAPE:
					table.clearSelection();
					break;
					
				case KeyEvent.VK_CONTROL:
					control = false;
					break;
				}
			}
			
			public void keyPressed(KeyEvent event) {
				switch(event.getKeyCode()) {
				case KeyEvent.VK_CONTROL:
					control = true;
					break;
					
				case KeyEvent.VK_END:
					if(control == true) {
						scrollToY(table.getHeight());
					} else {
						scrollToX(table.getWidth());
					}
					break;

				case KeyEvent.VK_HOME:
					if(control == true) {
						scrollToY(0);
					} else {
						scrollToX(0);
					}
					break;

				case KeyEvent.VK_PAGE_DOWN:
					scrollPage(1);
					break;
					
				case KeyEvent.VK_PAGE_UP:
					scrollPage(-1);
					break;
					
				}
			}
		});
		table.getSelectionModel().addListSelectionListener(footerDetails);
		table.setShowGrid(false);
		
		JScrollPane tableScroller = new JScrollPane(table);
		tableScroller.setRowHeaderView(new RowHeader(table, new LogFileRowHeaderModel(tableModel)));
		tableScroller.setColumnHeader(null);
		tableScroller.setColumnHeaderView(null);
		
		JPanel corner = new JPanel();
		corner.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		tableScroller.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner);
		
		filters = new FiltersPanel();
		
		filters.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doFilter(true);
			}
		});
		
		footer = new JPanel(new BorderLayout());
		footer.add(footerDetails, BorderLayout.SOUTH);
		
		searchSession = new SearchSession(table, tableModel, editor.field);
		
		add(filters, BorderLayout.NORTH);
		add(tableScroller, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);
		
		
		frame = new JFrame("Log Viewer");
		frame.setIconImage(loadIcon());
		
		buildMenuBar(frame);
		frame.getContentPane().add(this);
		setupTransferHandler(frame);
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	private static Image loadIcon() {
	    InputStream in = null;
	    try {
	        in = ClassLoader.getSystemResourceAsStream("icon.png");
            int len = in.available();
            byte[] bytes = new byte[len];
            in.read(bytes, 0, len);
            in.close();
            return new ImageIcon(bytes).getImage();
	    } catch (Throwable th) {
	        log.error("Failed to load icon.", th);
	        return null;
	    } finally {
	        try {
	            if(in != null) in.close();
	        } catch (IOException e) {
	            log.error("Failed to close icon input stream.", e);
	        }
	    }
	}
	
	private void scrollToY(int y) {
		Rectangle visible = table.getVisibleRect();
		Rectangle show = new Rectangle(visible.x, y, 1, 1);
		table.scrollRectToVisible(show);
	}
	
	private void scrollPage(int direction) {
		Rectangle visible = table.getVisibleRect();
		Rectangle show = new Rectangle(visible.x, visible.y+(int)(visible.height*direction), visible.width, visible.height);
		table.scrollRectToVisible(show);
	}
	
	private void scrollToX(int x) {
		Rectangle visible = table.getVisibleRect();
		Rectangle show = new Rectangle(x, visible.y, 1, 1);
		table.scrollRectToVisible(show);
	}
	
	public void displayFrame() {
		frame.pack();
		Dimension d = frame.getPreferredSize();
		d.width = 800;
		frame.setPreferredSize(d);
		frame.setSize(d);
		frame.setVisible(true);
	}
	
	private void setLogFile(LogFile file) {
		file.setListener(footerDetails);
		tableModel.setLogFile(file);
		packColumns();
		doFilter(false);
	}

	private void doFilter(final boolean scrollToRow) {
		final int selectedRow = table.getSelectedRow();
		final int lastSelectedFileLine = selectedRow < 0 ? -1 : tableModel.getUnfilteredRowIndex(selectedRow);
		new Thread("FilterThread") {
			public void run() {
				if(scrollToRow) {
					
					tableModel.applyFilter(filters.getFilter());
					
					if(lastSelectedFileLine >= 0) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {

								int viewRow = 0;
								int line = lastSelectedFileLine;
								while(viewRow <= 0 && line > 0) {
									viewRow = tableModel.getFilteredRowIndex(line);
									log.trace(line + "," + viewRow);
									line--;
								}
								Rectangle visible = table.getVisibleRect();
								int y = table.getRowHeight()*viewRow;
								Rectangle show = new Rectangle(visible.x, y, visible.width, table.getRowHeight());
								log.trace(show);
								table.scrollRectToVisible(show);
							}
						});
					}
				} else {
					tableModel.applyFilter(filters.getFilter());
				}

			}
		}.start();
	}

	private void setLoading(final boolean isLoading) {
		log.debug("Start set loading " + isLoading);
		final Runnable r = new Runnable() {
			public void run() {
				log.debug("Run set loading " + isLoading);
				if(isLoading) {
			        frame.setTitle("LogViewer - loading...");
			        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			    } else {
			    	frame.setTitle("LogViewer - " + concatFileNames(loadedFiles));
			        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			    }
			}
		};
		
		if(SwingUtilities.isEventDispatchThread()) {
			r.run();
		} else {
			SwingUtilities.invokeLater(r);
		}
	}
	
	private void setLogFiles(File ... files) throws IOException {
		setLogFiles(Arrays.asList(files));
	}
	
	private void setLogFiles(List<File> files) throws IOException {
		try {
			if(files.size() > 1) {
				files = OrderFilesPanel.showOrderFilesDialog(frame, files);
				//for some reason have to set again here, otherwise mouse cursor
				//switches back
				if(files != null) {
					setLoading(true);
					LogFile logFile = new LogFile(FileUtil.readLines(files, Charset.defaultCharset()));
					setLogFile(logFile);
					loadedFiles = files;
					setLoading(false);
				}
			} else if (files.size() == 1) {
				File file = files.get(0);
				setLoading(true);
				LogFile logFile = new LogFile(FileUtil.readLines(file, Charset.defaultCharset()));
				setLogFile(logFile);
				loadedFiles = files;
				setLoading(false);
			}
		} finally {
			setLoading(false);
		}
	}
	
	private static String concatFileNames(List<File> files) {
		StringBuilder sb = new StringBuilder();
		
		File dir = null;
		boolean showParentDir = true;
		for(File f : files) {
			if(showParentDir && !f.getParentFile().equals(dir) && dir != null) {
				showParentDir = false;
			}
			dir = f.getParentFile();
			sb.append(", ");
			sb.append(f.getName());
		}
		
		String fileString = sb.toString().substring(2);
		return showParentDir ? String.format("%s (%s)", fileString, dir): fileString;
	}
	
	private void packColumns() {
	    packColumns(table, 2);
	}
	
	private void packColumns(JTable table, int margin) {
		//table.setRowHeight(rowHeight)
		
		int height = table.getRowHeight();
		log.debug("rowHeight: " + height);
		FontMetrics metrics = table.getGraphics().getFontMetrics(config.font.get());
		height = Math.max(height, metrics.getHeight()+4);
		log.debug("rowHeight from metrics: " + height);
		
		//can use an alternate, slightly faster method if using a monospace font
		/*
		int charWidth = metrics.charWidth('W');
		log.debug("charWidth: " + charWidth);
		*/
		
	    for(int i = 0; i < table.getColumnCount(); i++) {
	        TableColumn col = table.getColumnModel().getColumn(i);
	        int width = 0;
	        // Get width of column header
	        TableCellRenderer renderer = col.getHeaderRenderer();
	        if (renderer == null) {
	            renderer = table.getTableHeader().getDefaultRenderer();
	        }
	        Component comp = renderer.getTableCellRendererComponent(
	                table, col.getHeaderValue(), false, false, 0, 0);
	        Dimension preferredSize = comp.getPreferredSize();
			width = preferredSize.width;

	        // Get maximum width of column data
			
			int testRows = Math.min(10, table.getRowCount());
			int maxDiffWidth = 0;
			for(int r=0; r<testRows; r++) {
				renderer = table.getCellRenderer(r, i);
	            comp = renderer.getTableCellRendererComponent(
	                    table, table.getValueAt(r, i), false, false, r, i);
	            preferredSize = comp.getPreferredSize();
	            //int strWidth = table.getValueAt(r, i).toString().length()*charWidth;
	            int strWidth = metrics.stringWidth(table.getValueAt(r, i).toString());
	            maxDiffWidth = Math.max(maxDiffWidth, preferredSize.width - strWidth);
			}
			log.debug("maxDiffWidth: " + maxDiffWidth);
			
	        for (int r=0; r<table.getRowCount(); r++) {
	        	 //int strWidth = table.getValueAt(r, i).toString().length()*charWidth;
	            int strWidth = metrics.stringWidth(table.getValueAt(r, i).toString());
	        	width = Math.max(width, strWidth + maxDiffWidth);
	        }

	        // Add margin
	        width += 2*margin;

	        log.debug("width: " + width);

	        // Set the width
	        col.setPreferredWidth(width);
	    }
		
		table.setRowHeight(height);
	}

	private void setupTransferHandler(final JFrame frame) {
		try {
			final DataFlavor uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String; charset=Unicode");
			frame.setTransferHandler(new TransferHandler() {


				@Override
				public boolean canImport(TransferSupport support) {
					log.trace(String.format("canImport: %d", support.getSourceDropActions()));
					for(DataFlavor flavor : support.getDataFlavors()) {
						log.trace(flavor);
					}
					try {
						return 
						support.isDataFlavorSupported(uriListFlavor) 
						|| support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
					} catch (Throwable th) {
						log.error(th);
						return false;
					} 
				}

				@SuppressWarnings("unchecked")
				@Override
				public boolean importData(TransferSupport support) {
					log.debug("importData");
					if (!canImport(support)) {
						return false;
					}

					Transferable t = support.getTransferable();

					
					try {
					    List<File> files = null;
					    if(support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					        files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
					    } else if(support.isDataFlavorSupported(uriListFlavor)) {
					        String uriList = (String)t.getTransferData(uriListFlavor);
					        files = new LinkedList<File>();
					        for(String uri : uriList.split("\r\n")) {
					        	files.add(new File(new URI(uri)));
					        }
					    }
					    
					    setLogFiles(files);
					} catch (UnsupportedFlavorException e) {
						return false;
					} catch (Throwable e) {
						return false;
					}

					return true;
				}
			});
		} catch (ClassNotFoundException e1) {
			throw new Error(e1);
		}
	}
	
	private void buildMenuBar(final JFrame frame) {
		final JFileChooser chooser = new JFileChooser(new File("."));
		chooser.setMultiSelectionEnabled(true);
		
		MenuUtil menu = new MenuUtil();
		
		menu.createMenu("&File", 
				new MenuAction("&Open...", "O") {
			public void actionPerformed(ActionEvent arg0) {
				if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
					List<File> files = Arrays.asList(chooser.getSelectedFiles());
					try {
						
						setLogFiles(files);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		},
		new MenuAction("&Reload...", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0)) {
			public void actionPerformed(ActionEvent arg0) {
				if(loadedFiles != null) {
					try {
						setLogFiles(loadedFiles);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		},
		new MenuAction("&Delete Files", KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK)) {
			public void actionPerformed(ActionEvent arg0) {
				if(loadedFiles != null) {
					int option = JOptionPane.showConfirmDialog(LogFileViewer.this, "Do you want to delete the open log files?", "Delete files?", JOptionPane.YES_NO_OPTION);
					
					if(option == JOptionPane.YES_OPTION) {
						boolean couldNotDeleteAll = false;
						for(File f : loadedFiles) {
							if(f.exists() && !f.delete()) {
								couldNotDeleteAll = true;
							}
						}
						
						if(couldNotDeleteAll) {
							JOptionPane.showMessageDialog(LogFileViewer.this, "Could not delete all files!", "Could Not Delete", JOptionPane.WARNING_MESSAGE);
						}
					}
				}
			}
		}
		);

		menu.createMenu("&View",
				new MenuAction("&Font...") {
			public void actionPerformed(ActionEvent arg0) {
				JFontChooser chooser = new JFontChooser();
				chooser.setSelectedFont(table.getFont());
				int result = chooser.showDialog(LogFileViewer.this);
				if(result == JFontChooser.OK_OPTION) {
					table.setFont(chooser.getSelectedFont());
					config.font.set(chooser.getSelectedFont());
					try {
						config.storeConfig();
					} catch (Exception e) {
						log.error("Failed to save config.", e);
					}
					
					packColumns();
				}
			}
		},
		new MenuAction("Search &Color...") {
			public void actionPerformed(ActionEvent arg0) {
				Color newColor = JColorChooser.showDialog(LogFileViewer.this, "Choose Search Highlight Color", config.searchHighlight.get());
				if(newColor != null) {
					config.searchHighlight.set(newColor);
					searchSession.setSearchHighlightColor(newColor);					
					try {
						config.storeConfig();
					} catch (Exception e) {
						log.error("Failed to save config.", e);
					}
				}				
			}
		}
		);
		
		menu.createMenu("&Search", 
				new MenuAction("&Search", "S") {
			public void actionPerformed(ActionEvent arg0) {
				if(verifySearchPanel()) {
					if(searchPanel.isReverse()) {
						searchPanel.setDirection(false);
					}
					searchPanel.search();
				}
			}
		},
		new MenuAction("&Reverse Search", "R") {
			public void actionPerformed(ActionEvent arg0) {
				if(verifySearchPanel()) {
					if(!searchPanel.isReverse()) {
						searchPanel.setDirection(true);
					}
					searchPanel.search();
				}
			}
		},
		new MenuAction("Go to &Line", "L") {
			public void actionPerformed(ActionEvent arg0) {
				scrollToLine();
			}
		}
		);
		
		menu.installOn(frame);
	}
	
	private void scrollToLine() {
		String rowString = JOptionPane.showInputDialog(this, "Enter Line:");
		if(rowString != null) {
			int row = tableModel.getFilteredRowIndex(Integer.parseInt(rowString));
			if(row < 0) {
				row = -row;
			}
			row--;
			int h = table.getRowHeight();

			Rectangle visible = table.getVisibleRect();
			Rectangle rect = new Rectangle(visible.x, h*(row), visible.width, h);
			table.scrollRectToVisible(rect);
			table.setRowSelectionInterval(row, row);
		}
	}

	private boolean verifySearchPanel() {
		if(searchPanel == null) {
			searchPanel = new SearchPanel(searchSession);
			
			int selected = table.getSelectedRow();
			log.trace("Selected="+selected);
			if(selected >= 0) {
			    searchSession.setStartingRow(selected);
			}
			
			searchPanel.addSearchPanelListener(new SearchPanelListener() {
                public void searchCanceled() {
                    table.editingCanceled(null);
                    footer.remove(searchPanel);
                    searchPanel = null;
                    searchSession.reset(null);
                    footer.revalidate();
                }
            });
			
			footer.add(searchPanel, BorderLayout.NORTH);
			footer.validate();
			validate();
			searchPanel.requestFocus();
			return false;
		} else {
			return true;
		}
	}
	
	private static class SelectionInterpreter implements CaretListener, MouseMotionListener, FocusListener {
		private static final Pattern NUMBER = Pattern.compile("\\d+");
		private SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		private SimpleDateFormat local = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		{
			utc.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		}
		
		private Popup popup;
		private Point point;
		private JTextField owner;
		private JTextField tip;
		
		public SelectionInterpreter(JTextField field) {
			owner = field;
			tip = new JTextField();
			tip.setEditable(false);
			
			field.addCaretListener(this);
			field.addMouseMotionListener(this);
			field.addFocusListener(this);
			tip.addFocusListener(this);
		}

		private void showPopup(String text) {
			if(popup != null) {
				popup.hide();
				popup = null;
			}
			if(text != null && point != null) {
				tip.setText(text);
				popup = PopupFactory.getSharedInstance().getPopup(owner, tip, point.x, point.y);
				popup.show();
			}
		}
		
		private void hidePopup() {
			if(popup != null) {
				popup.hide();
				popup = null;
			}
		}
		
		@Override
		public void caretUpdate(CaretEvent e) {
			if(e.getDot() != e.getMark()) {
				String text = processText(owner.getSelectedText());
				showPopup(text);
			} else {
				hidePopup();
			}
		}
		
		private String processText(String txt) {
			if(NUMBER.matcher(txt).matches()) {
				long stamp = Long.parseLong(txt);
				if(stamp < 1000000000000l) {
					stamp *= 1000l;
				}
				return String.format("%s (%s)", utc.format(new Date(stamp)), local.format(new Date(stamp)));
			}
			return null;
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			point = e.getLocationOnScreen();
		}

		@Override
		public void mouseDragged(MouseEvent e) {}

		@Override
		public void focusGained(FocusEvent e) { }

		@Override
		public void focusLost(FocusEvent e) {
			if((e.getComponent() == owner && e.getOppositeComponent() != tip) || (e.getComponent() == tip && e.getOppositeComponent() != owner)) {
				hidePopup();
			}
		}
		
	}
	
	private static class NonEditableEditor extends AbstractCellEditor implements TableCellEditor {
		private JTextField field;
		
		public NonEditableEditor() {
			field = new JTextField();
			field.setEditable(false);
			field.setBorder(BorderFactory.createEmptyBorder());
			field.setBackground(Color.WHITE);
			field.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			new SelectionInterpreter(field);
			
		}
		
		@Override
		public boolean isCellEditable(EventObject evo) {
			if(evo instanceof MouseEvent) {
				return ((MouseEvent)evo).getClickCount() >= 2;
			}
			return evo == null;
		}

		public Object getCellEditorValue() {
			return field.getText();
		}

		@Override
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int col) {
			field.setText((String)value);
			field.setEditable(false);
			field.setFont(table.getFont());
			return field;
		}
		
	}
	
	public static void main(final String[] args) throws Exception {
		UIManager.setLookAndFeel(
	            UIManager.getSystemLookAndFeelClassName());

		class Holder {
		    LogFileViewer viewer;
		}
		final Holder viewHolder = new Holder();
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				try {
					LogFileViewer viewer = new LogFileViewer();
					viewHolder.viewer = viewer;
					
					viewer.displayFrame();
					
				} catch (Throwable th) {
					th.printStackTrace();
				}
			
			}
		});
		
		SwingUtilities.invokeAndWait(new Runnable() {
		    public void run() {
		        try {
		            if(args.length > 0) {
		                viewHolder.viewer.setLogFiles(new File(args[0]));
		            }
		        } catch (Throwable th) {
		            th.printStackTrace();
		        }
		    }
		});
	}
}
