/*******************************************************************************
 * Copyright 2012-2015 Esri
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/
package com.esri.messagesimulator;

import com.esri.militaryapps.controller.MessageController;
import com.esri.militaryapps.util.Utilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class MessageSimulatorJFrame extends JFrame implements WindowListener {

	private static final long serialVersionUID = 1L;
        private static final Logger logger = Logger.getLogger(MessageSimulatorJFrame.class.getName());
        
        private final MessageController controller;
	
	// Instance attributes used in this example
	private JPanel tablePanel;
	private JTable table;
	private JScrollPane scrollPane;
	private int timerDelayInMillisecs = 0;
	private DefaultTableModel model;
	private Timer rowTimer;
	int count = 0;
	File file;
	NodeList nodList;
	Element elem;
	Element geomessages;
	Node nextNode;
	JButton startButton;
	JButton stopButton;
	JButton pauseButton;
	JButton changeSimFile;
	JButton sendButton;
	Document dom;
	int selectedTime;
	int selectedThrough;
	JSpinner frequencySpinner;
	JSpinner throughputSpinner;
	JSpinner portSpinner;
	Boolean simulatorRunning = false;

	JFileChooser fileChooser = new JFileChooser();
	JLabel status;
        private final JPanel jPanel_timeOverride;
        private final JTable jTable_timeOverride;
        private final HashSet<String> timeOverrideFields = new HashSet<String>();
	
	// Constructor of main frame
	public MessageSimulatorJFrame() {

		// Set the frame characteristics
		setTitle("Message Simulator");
		setSize(700, 750);
		setBackground(Color.gray);

		// creating the buttons for the gui
		startButton = new JButton("Start Simulator");
		stopButton = new JButton("Stop Simulator");
		pauseButton = new JButton("Pause Simulator");
		changeSimFile = new JButton("Load Simulation File");
		// add action to buttons
		startButton.addActionListener(new StartButtonActionListener());
		stopButton.addActionListener(new StopButtonActionListener());
		pauseButton.addActionListener(new PauseButtonActionListener());
		changeSimFile.addActionListener(new FileChangeActionListener());

		// disable/enable Buttons before file is loaded
		stopButton.setEnabled(false);
		startButton.setEnabled(false);
		pauseButton.setEnabled(false);

		// Panel for the buttons
		JPanel buttonPanel = new JPanel();
		// add the buttons to the panel
		buttonPanel.add(changeSimFile);

		buttonPanel.add(startButton);
		buttonPanel.add(pauseButton);
		buttonPanel.add(stopButton);
		// status label before a file is loaded
		JPanel statusPanel = new JPanel();
		status = new JLabel("<html><br>Please load an XML file</html>");
		statusPanel.add(status);

		// create the table to but the xml data in
		model = new DefaultTableModel();
		// Create a panel to hold the table
		tablePanel = new JPanel();
		// Create a new table instance
		table = new JTable(model);
		// create the columns for the table
		model.addColumn(" ");
		model.addColumn("Message Name");
		model.addColumn("Message ID");
		model.addColumn("Message Action");
		model.addColumn("Symbol ID");
		model.addColumn("Type");

		// Configure some of JTable's parameters
		table.setShowHorizontalLines(true);
		table.setRowSelectionAllowed(true);
		table.setColumnSelectionAllowed(true);

		// Change the selection color
		table.setSelectionForeground(Color.white);
		table.setSelectionBackground(Color.blue);
                
                jPanel_timeOverride = new JPanel();
                jPanel_timeOverride.setLayout(new BorderLayout());
                jPanel_timeOverride.add(new JLabel("Time Override Fields (use the current time in outgoing message for the value of these fields)"), BorderLayout.NORTH);
                jTable_timeOverride = new JTable();
                jTable_timeOverride.setFocusable(false);
                jTable_timeOverride.setTableHeader(null);
                jTable_timeOverride.setShowGrid(false);
                jTable_timeOverride.setFillsViewportHeight(true);
                JScrollPane toScrollPane = new JScrollPane(jTable_timeOverride);
                jPanel_timeOverride.add(toScrollPane, BorderLayout.CENTER);

		// Creating the Spinners for seconds and throughput
		JPanel spinnerPanel = new JPanel();
		
		// frequencySpinner for seconds
		SpinnerNumberModel spinmodel = new SpinnerNumberModel(1, 1, 10, 1);
		frequencySpinner = new JSpinner(spinmodel);
		frequencySpinner.addChangeListener(new FrequencyChangeListener());
		JLabel spinnerDes1 = new JLabel(
				"<html>Simulation Frequency <br> (Broadcasts Per Second) <br></html>",
				SwingConstants.CENTER);
		JLabel spinnerDes2 = new JLabel(
				"<html>Simulation Throughput <br> (Messages Per Broadcast) <br></html>",
				SwingConstants.CENTER);

		spinnerPanel.add(spinnerDes1);
		spinnerPanel.add(frequencySpinner);
		spinnerPanel.add(spinnerDes2);

		// Spinner for throughput
		SpinnerNumberModel spinmodelThroughput = new SpinnerNumberModel(1, 1,
				10, 1);
		throughputSpinner = new JSpinner(spinmodelThroughput);
		selectedThrough = 1;
		throughputSpinner.addChangeListener(new ThroughputChangeListener());
		spinnerPanel.add(throughputSpinner);
		// end frequencySpinner stuff		
		
		//Spinner for the port
		SpinnerNumberModel spinModelPort = new SpinnerNumberModel(45678, 1,
				100000, 1);
		portSpinner = new JSpinner(spinModelPort);
		portSpinner.addChangeListener(new PortChangeListener());
		
		JLabel portlabel = new JLabel("Port");
		
		JPanel portPanel = new JPanel();
		portPanel.add(portlabel);
		portPanel.add(portSpinner);		
		//end frequencySpinner for the port

		// adding a scroll pane to the table
		scrollPane = new JScrollPane(table);
		scrollPane.setAutoscrolls(true);

		tablePanel.add(scrollPane);

		// Create a main panel for the other panels to be added to
		JPanel mainPanel = new JPanel();
		// makes table panel the entire size of the gui window
		tablePanel.setLayout(new BoxLayout((tablePanel), BoxLayout.Y_AXIS));
		statusPanel.setLayout(new BoxLayout((statusPanel), BoxLayout.X_AXIS));
		//portPanel.setLayout(new BoxLayout((portPanel), BoxLayout.PAGE_AXIS));
		mainPanel.setLayout(new BoxLayout((mainPanel), BoxLayout.PAGE_AXIS));
		
		getContentPane().add(mainPanel);
		mainPanel.add(tablePanel);
                mainPanel.add(Box.createVerticalStrut(20));
                mainPanel.add(jPanel_timeOverride);
		mainPanel.add(statusPanel);
		mainPanel.add(Box.createVerticalGlue());
		mainPanel.add(spinnerPanel);
		mainPanel.add(portPanel);
		mainPanel.add(buttonPanel);

		addWindowListener(this);
		
		setVisible(true);

		// Set the row timer
		rowTimer = new Timer(1000, new TimerHandler());
		rowTimer.start();
                
                controller = new MessageController(45678, "Message Simulator");
                controller.setBindAndListen(false);
	}

	// Starts the timer when you want to start showing the rows in the table
	public void startSimulation() {
		simulatorRunning = true;
	}

	// populates the table with data when called
	public void addRow(String messageName, String messageId,
			String messageAction, String symbolId, String type) {

		count++;
		model.addRow(new Object[] { count, messageName, messageId,
				messageAction, symbolId, type });
	}

	public void parseFile(File file) {
		// get the factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		try {
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			// parse using builder to get DOM representation of the XML file
			dom = db.parse(file);
			geomessages = dom.getDocumentElement();
                        populateFieldList(geomessages);
			nextNode = geomessages.getFirstChild();

		} catch (ParserConfigurationException pce) {
                    logger.log(Level.SEVERE, "Could not read simulation file", pce);
		} catch (SAXException se) {
                    logger.log(Level.SEVERE, "Could not read simulation file", se);
		} catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Could not read simulation file", ioe);
		}
	}
        
        private void populateFieldList(Element geomessages) {
            TreeSet<String> fields = new TreeSet<String>();
            Node node = geomessages.getFirstChild();
            while (node != null) {
                if ("geomessage".equalsIgnoreCase(node.getNodeName())) {
                    NodeList childNodes = node.getChildNodes();
                    for (int i = 0; i < childNodes.getLength(); i++) {
                        Node child = childNodes.item(i);
                        String nodeName = child.getNodeName();
                        if (null != nodeName && !"#text".equals(nodeName)) {
                            fields.add(nodeName);
                        }
                    }
                }
                node = node.getNextSibling();
            }
            
            final DefaultTableModel timeOverrideTableModel = new DefaultTableModel(0, 2) {

                @Override
                public Class<?> getColumnClass(int column) {
                    switch (column) {
                    case 0:
                        return Boolean.class;
                    case 1:
                    default:
                        return String.class;
                    }
                }

            };
            timeOverrideTableModel.addTableModelListener(new TableModelListener() {

                public void tableChanged(TableModelEvent e) {
                    if (0 == e.getColumn()) {
                        boolean checked = (Boolean) timeOverrideTableModel.getValueAt(e.getFirstRow(), 0);
                        String value = (String) timeOverrideTableModel.getValueAt(e.getFirstRow(), 1);
                        synchronized (timeOverrideFields) {
                            if (checked) {
                                timeOverrideFields.add(value);
                            } else {
                                timeOverrideFields.remove(value);
                            }
                        }
                    }
                }
            });
            Iterator<String> iterator = fields.iterator();
            while (iterator.hasNext()) {
                String field = iterator.next();
                timeOverrideTableModel.addRow(new Object[] { false, field });
            }
            jTable_timeOverride.setModel(timeOverrideTableModel);
            jTable_timeOverride.getColumnModel().getColumn(1).setPreferredWidth(Integer.MAX_VALUE);
        }

	// to get the XML node as a string
	public void getNodeString(Node n) throws TransformerException, IOException {
            final String currentDateString = Utilities.DATE_FORMAT_GEOMESSAGE.format(new Date());
            
		// trying to get element as a string
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
                    logger.log(Level.SEVERE, "Could not init DocumentBuilder", e);
		}
		if (docBuilder == null) {
			return;
		}
		
        Document doc = docBuilder.newDocument();

        //create the root element and add it to the document
        Element root = doc.createElement("geomessages");
        doc.appendChild(root);
        
        Node importedNode = doc.importNode(n, true);
        
        //Time override
        synchronized (timeOverrideFields) {
            NodeList fieldNodes = importedNode.getChildNodes();
            for (int j = 0; j < fieldNodes.getLength(); j++) {
                Node field = fieldNodes.item(j);
                if (timeOverrideFields.contains(field.getNodeName())) {
                    field.getFirstChild().setNodeValue(currentDateString);
                }
            }
        }
        
        root.appendChild(importedNode);
	
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		
		String xmlString = result.getWriter().toString();
		
		final boolean DEBUG_OUTPUT = false;
		if (DEBUG_OUTPUT) {
                        logger.fine(xmlString);
		}
		
		byte[] byteString = xmlString.getBytes();

		controller.sendMessage(byteString);
	}

	/**
	 * getTextValue for an xml element tag name
	 */
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if (nl != null && nl.getLength() > 0) {
			Element el = (Element) nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}

	/**
	 * Take a GeoMessage element and read the values in, create an
	 * GeoMessage object and return it
	 * 
	 * @throws TransformerException
	 */
	private void parseGeoMessage(Element geoElement) {

		// for each element get text values
		String messageName = getTextValue(geoElement, "uniquedesignation");
		String messageId = getTextValue(geoElement, "_id");
		String messageAction = getTextValue(geoElement, "_action");
		String symbolId = getTextValue(geoElement, "_id");
		String type = getTextValue(geoElement, "type");

		addRow(messageName, messageId, messageAction, symbolId, type);
	}

	private void getNextMessage() throws TransformerException {

		nextNode = nextNode.getNextSibling();

		while (nextNode.getNodeType() != Node.ELEMENT_NODE) {
			nextNode = nextNode.getNextSibling();
			if (nextNode == null) {
				break;
			}
		}
		if (nextNode == null) {
			nextNode = geomessages.getFirstChild();
			nextNode = nextNode.getNextSibling();

		}

		Element currentNode = (Element) nextNode;
		parseGeoMessage(currentNode);

		// get the string version of the node
		try {
			getNodeString(nextNode);
		} catch (IOException e) {
                    logger.log(Level.SEVERE, "Could not getNodeString", e);
		}

	}	
	
	private class PortChangeListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			Integer i = (Integer) portSpinner.getValue(); 
                        controller.setPort(i);

                        logger.log(Level.FINE, "portSpinner = {0}", i);
		}
	}

	private class ThroughputChangeListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			Integer i = (Integer) throughputSpinner.getValue(); 			
			selectedThrough = i.intValue();

                        logger.log(Level.FINE, "selected thoughput = {0}", selectedThrough);
		}

	}

	private class FrequencyChangeListener implements ChangeListener {

		public void stateChanged(ChangeEvent e) {

			Integer i = (Integer) frequencySpinner.getValue(); 						
			selectedTime = i.intValue();

			timerDelayInMillisecs = (int) ((1.0 / (double) (selectedTime)) * 1000.0);
			logger.log(Level.FINE, "selected time = {0} add row delay =  {1}",
                                new Object[]{selectedTime, timerDelayInMillisecs});
			rowTimer.setDelay(timerDelayInMillisecs);
		}

	}

	private class StartButtonActionListener implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {

			geomessages = dom.getDocumentElement();
			nextNode = geomessages.getFirstChild();

			if ("Start Simulator".equals(startButton.getText())) {
				status.setText("Simulation Started");
				stopButton.setEnabled(true);
				pauseButton.setEnabled(true);
				count = 0;
				model.setRowCount(0);
				startSimulation();
				startButton.setText("Restart Simulator");
				pauseButton.setText("Pause Simulator");
				changeSimFile.setText("Change Simulation File");
			}

			else {
				status.setText("Simulation Restarted");
				count = 0;
				model.setRowCount(0);
				startSimulation();
				pauseButton.setText("Pause Simulator");
			}

		}

	}

	private class PauseButtonActionListener implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {

			if ("Pause Simulator".equals(pauseButton.getText())) {
				status.setText("Simulation Paused");
				simulatorRunning = false;
				pauseButton.setText("Continue Simulator");
			}

			else {
				status.setText("Simulation Continued");
				startSimulation();
				pauseButton.setText("Pause Simulator");
			}

		}

	}

	// inner class: action for the stop app button
	private class StopButtonActionListener implements ActionListener {

		public void actionPerformed(ActionEvent arg0) {
			status.setText("Simulation Stopped");
			simulatorRunning = false;

			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			pauseButton.setEnabled(false);

			startButton.setText("Start Simulator");
		}

	}

	// inner class: action for Changing the simulation file
	private class FileChangeActionListener implements ActionListener {

		public void actionPerformed(ActionEvent e) {
			// the file is now loaded, enable buttons

			stopButton.setEnabled(true);
			startButton.setEnabled(true);
			pauseButton.setEnabled(true);

			// Open a file dialog
		    final String currentDir = System.getProperty("user.dir");			
			fileChooser.setCurrentDirectory(new File(currentDir));
			
			int retval = fileChooser.showOpenDialog(changeSimFile);
			if (retval == JFileChooser.APPROVE_OPTION) {
				// The user selected a file, get it, use it.
				file = fileChooser.getSelectedFile();
				parseFile(file);

			}
			status.setText("File Loaded");
		}
	}

	// populates the gui with the table information with a call to
	// GetNextMessage() with
	// time delay
	private class TimerHandler implements ActionListener {

		public void actionPerformed(ActionEvent actionEvent) {

			// for throughput put it in a loop for the number of times 1-10.
			if (simulatorRunning == false) {
				return;
			}

			for (int i = 0; i < selectedThrough; i++) {
				try {
					getNextMessage();
				} catch (TransformerException e) {
                                    logger.log(Level.SEVERE, "Could not getNextMessage", e);
				}
			}
		}

	}

        @Override
	protected void finalize() {
            try {
                super.finalize();
            } catch (Throwable ex) {
                logger.log(Level.SEVERE, null, ex);
            } finally {
		System.exit(0);
            }
	}

	// Main entry point for this example
	public static void main(String args[]) {
		// Create an instance of the test application
		MessageSimulatorJFrame mainFrame = new MessageSimulatorJFrame();
		mainFrame.setVisible(true);
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void windowClosed(WindowEvent e) {	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		rowTimer.stop();	
		rowTimer = null;
		
		this.dispose();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub		
	}
}
