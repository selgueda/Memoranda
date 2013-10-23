package net.sf.memoranda.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.memoranda.CurrentProject;
import net.sf.memoranda.EventNotificationListener;
import net.sf.memoranda.EventsManager;
import net.sf.memoranda.EventsScheduler;
import net.sf.memoranda.History;
import net.sf.memoranda.NoteList;
import net.sf.memoranda.Project;
import net.sf.memoranda.ProjectListener;
import net.sf.memoranda.ProjectManager;
import net.sf.memoranda.ResourcesList;
import net.sf.memoranda.TaskList;
import net.sf.memoranda.date.CalendarDate;
import net.sf.memoranda.date.CurrentDate;
import net.sf.memoranda.date.DateListener;
import net.sf.memoranda.util.AgendaGenerator;
import net.sf.memoranda.util.CurrentStorage;
import net.sf.memoranda.util.Local;
import net.sf.memoranda.util.Util;

/*$Id: AgendaPanel.java,v 1.11 2005/02/15 16:58:02 rawsushi Exp $*/
public class AgendaPanel extends JPanel {
	BorderLayout borderLayout1 = new BorderLayout();
	JButton historyBackB = new JButton();
	JToolBar toolBar = new JToolBar();
	JButton historyForwardB = new JButton();
	JEditorPane viewer = new JEditorPane("text/html", "");

	JScrollPane scrollPane = new JScrollPane();

	DailyItemsPanel parentPanel = null;
	
//	JPopupMenu agendaPPMenu = new JPopupMenu();
//	JCheckBoxMenuItem ppShowActiveOnlyChB = new JCheckBoxMenuItem();
	
	Collection expandedTasks;
	String gotoTask = null;

	boolean isActive = true;

	public AgendaPanel(DailyItemsPanel _parentPanel) {
		try {
			parentPanel = _parentPanel;
			jbInit();
		} catch (Exception ex) {
		    new ExceptionDialog(ex);
			ex.printStackTrace();
		}
	}
	void jbInit() throws Exception {
		expandedTasks = new ArrayList();

		toolBar.setFloatable(false);
		viewer.setEditable(false);
		viewer.setOpaque(false);
		viewer.addHyperlinkListener(new HyperlinkListener() {

			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					String d = e.getDescription();
					if (d.equalsIgnoreCase("memoranda:events"))
						parentPanel.alarmB_actionPerformed(null);
					else if (d.startsWith("memoranda:tasks")) {
						String id = d.split("#")[1];
						CurrentProject.set(ProjectManager.getProject(id));
						parentPanel.taskB_actionPerformed(null);
					} else if (d.startsWith("memoranda:project")) {
						String id = d.split("#")[1];
						CurrentProject.set(ProjectManager.getProject(id));
					} else if (d.startsWith("memoranda:removesticker")) {
						String id = d.split("#")[1];
						EventsManager.removeSticker(id);
						CurrentStorage.get().storeEventsManager();
						refresh(CurrentDate.get());
					} else if (d.startsWith("memoranda:addsticker")) {
						StickerDialog dlg = new StickerDialog(App.getFrame());
						Dimension frmSize = App.getFrame().getSize();
						dlg.setSize(new Dimension(300,380));
						Point loc = App.getFrame().getLocation();
						dlg.setLocation(
							(frmSize.width - dlg.getSize().width) / 2 + loc.x,
							(frmSize.height - dlg.getSize().height) / 2
								+ loc.y);
						dlg.setVisible(true);
						if (!dlg.CANCELLED) {
							String txt = dlg.getStickerText();
							txt = txt.replaceAll("\\n", "<br>");
							txt = "<div style=\"background-color:"+dlg.getStickerColor()+"\">"+txt+"</div>";
							EventsManager.createSticker(txt);
							CurrentStorage.get().storeEventsManager();
						}
						refresh(CurrentDate.get());
					} else if (d.startsWith("memoranda:expandsubtasks")) {
						String id = d.split("#")[1];
						gotoTask = id;
						expandedTasks.add(id);
						refresh(CurrentDate.get());
					} else if (d.startsWith("memoranda:closesubtasks")) {
						String id = d.split("#")[1];
						gotoTask = id;
						expandedTasks.remove(id);
						refresh(CurrentDate.get());
					}
				}
			}
		});
		historyBackB.setAction(History.historyBackAction);
		historyBackB.setFocusable(false);
		historyBackB.setBorderPainted(false);
		historyBackB.setToolTipText(Local.getString("History back"));
		historyBackB.setRequestFocusEnabled(false);
		historyBackB.setPreferredSize(new Dimension(24, 24));
		historyBackB.setMinimumSize(new Dimension(24, 24));
		historyBackB.setMaximumSize(new Dimension(24, 24));
		historyBackB.setText("");

		historyForwardB.setAction(History.historyForwardAction);
		historyForwardB.setBorderPainted(false);
		historyForwardB.setFocusable(false);
		historyForwardB.setPreferredSize(new Dimension(24, 24));
		historyForwardB.setRequestFocusEnabled(false);
		historyForwardB.setToolTipText(Local.getString("History forward"));
		historyForwardB.setMinimumSize(new Dimension(24, 24));
		historyForwardB.setMaximumSize(new Dimension(24, 24));
		historyForwardB.setText("");

		this.setLayout(borderLayout1);
		scrollPane.getViewport().setBackground(Color.white);
		
		scrollPane.getViewport().add(viewer, null);
		this.add(scrollPane, BorderLayout.CENTER);
		toolBar.add(historyBackB, null);
		toolBar.add(historyForwardB, null);
		toolBar.addSeparator(new Dimension(8, 24));

		this.add(toolBar, BorderLayout.NORTH);

		CurrentDate.addDateListener(new DateListener() {
			public void dateChange(CalendarDate d) {
				if (isActive)
					refresh(d);
			}
		});
		CurrentProject.addProjectListener(new ProjectListener() {

			public void projectChange(
				Project prj,
				NoteList nl,
				TaskList tl,
				ResourcesList rl) {
			}

			public void projectWasChanged() {
				if (isActive)
                	refresh(CurrentDate.get());
			}});
        EventsScheduler.addListener(new EventNotificationListener() {
            public void eventIsOccured(net.sf.memoranda.Event ev) {
                if (isActive)
                	refresh(CurrentDate.get());
            }

            public void eventsChanged() {
                if (isActive)
                	refresh(CurrentDate.get());
            }
        });
        refresh(CurrentDate.get());
                
//        agendaPPMenu.setFont(new java.awt.Font("Dialog", 1, 10));
//        agendaPPMenu.add(ppShowActiveOnlyChB);
//        PopupListener ppListener = new PopupListener();
//        viewer.addMouseListener(ppListener);
//		ppShowActiveOnlyChB.setFont(new java.awt.Font("Dialog", 1, 11));
//		ppShowActiveOnlyChB.setText(
//			Local.getString("Show Active only"));
//		ppShowActiveOnlyChB.addActionListener(new java.awt.event.ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				toggleShowActiveOnly_actionPerformed(e);
//			}
//		});		
//		boolean isShao =
//			(Context.get("SHOW_ACTIVE_TASKS_ONLY") != null)
//				&& (Context.get("SHOW_ACTIVE_TASKS_ONLY").equals("true"));
//		ppShowActiveOnlyChB.setSelected(isShao);
//		toggleShowActiveOnly_actionPerformed(null);		
    }
    
    public void refresh(CalendarDate date) {
    	viewer.setText(AgendaGenerator.getAgenda(date,expandedTasks));
    	SwingUtilities.invokeLater(new Runnable() {
    		public void run() {
		        if(gotoTask != null) {
		        	viewer.scrollToReference(gotoTask);
		        	scrollPane.setViewportView(viewer);
		        	Util.debug("Set view port to " + gotoTask);
		        }
    		}
    	});
    	
    	Util.debug("Summary updated.");
    }
    
    public void setActive(boolean isa) {
    	isActive = isa;
    }
    
//	void toggleShowActiveOnly_actionPerformed(ActionEvent e) {
//		Context.put(
//			"SHOW_ACTIVE_TASKS_ONLY",
//			new Boolean(ppShowActiveOnlyChB.isSelected()));
//		/*if (taskTable.isShowActiveOnly()) {
//			// is true, toggle to false
//			taskTable.setShowActiveOnly(false);
//			//showActiveOnly.setToolTipText(Local.getString("Show Active Only"));			
//		}
//		else {
//			// is false, toggle to true
//			taskTable.setShowActiveOnly(true);
//			showActiveOnly.setToolTipText(Local.getString("Show All"));			
//		}*/	    
//		refresh(CurrentDate.get());
////		parentPanel.updateIndicators();
//		//taskTable.updateUI();
//	}

//    class PopupListener extends MouseAdapter {
//
//        public void mouseClicked(MouseEvent e) {
//        	System.out.println("mouse clicked!");
////			if ((e.getClickCount() == 2) && (taskTable.getSelectedRow() > -1))
////				editTaskB_actionPerformed(null);
//		}
//
//		public void mousePressed(MouseEvent e) {
//        	System.out.println("mouse pressed!");
//			maybeShowPopup(e);
//		}
//
//		public void mouseReleased(MouseEvent e) {
//        	System.out.println("mouse released!");
//			maybeShowPopup(e);
//		}
//
//		private void maybeShowPopup(MouseEvent e) {
//			if (e.isPopupTrigger()) {
//				agendaPPMenu.show(e.getComponent(), e.getX(), e.getY());
//			}
//		}
//
//    }
}