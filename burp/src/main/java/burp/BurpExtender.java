///////////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2015 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s):
//              Denim Group, Ltd.
//              Secure Decisions, a division of Applied Visions, Inc
//
////////////////////////////////////////////////////////////////////////

package burp;

import burp.custombutton.LocalEndpointsButton;
import burp.extention.BurpPropertiesManager;
import com.denimgroup.threadfix.data.entities.RouteParameter;
import com.denimgroup.threadfix.data.interfaces.Endpoint;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: stran
 * Date: 12/30/13
 * Time: 2:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class BurpExtender implements IBurpExtender, ITab
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JTabbedPane tabbedPane;
    private JTextField sourceFolderField;
    private JTextField oldSourceFolderField;
    private JTextField configFileField;
    private JTextField targetHostField;
    private JTextField targetPathField;
    private JTextField targetPortField;
    private JCheckBox autoScanField;
    private JCheckBox autoSpiderField;
    private JCheckBox useHttpField;
    private JLabel profMessage;
    private JLabel autoScanText;
    //private JTextArea displayArea = new JTextArea();
    private JLabel displayArea = new JLabel();

    //
    // implement IBurpExtender
    //

    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {
        // keep a reference to our callbacks object
        this.callbacks = callbacks;

        BurpPropertiesManager.generateBurpPropertiesManager(callbacks);

        // obtain an extension helpers object
        helpers = callbacks.getHelpers();

        // set our extension name
        callbacks.setExtensionName("Attack Surface Detector");

        // create UI
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                tabbedPane = new JTabbedPane();

                JPanel mainPanel = buildMainPanel();
                JScrollPane mainScrollPane = new JScrollPane(mainPanel);
                tabbedPane.addTab("Main", mainScrollPane);

                JPanel optionsPanel = buildOptionsPanel();
                JScrollPane optionsScrollPane = new JScrollPane(optionsPanel);
                tabbedPane.addTab("Options", optionsScrollPane);

                // customize our UI components
                callbacks.customizeUiComponent(tabbedPane);

                // add the custom tab to Burp's UI
                callbacks.addSuiteTab(BurpExtender.this);
            }
        });
    }

    private JPanel buildMainPanel()
    {
        final JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new GridBagLayout());
        Insets mainPanelInsets = new Insets(10, 10, 10, 10);
        int yPosition = 0;

        JPanel importExportPanel = buildImportExportPanel();
        GridBagConstraints importExportPanelConstraints = new GridBagConstraints();
        importExportPanelConstraints.gridx = 0;
        importExportPanelConstraints.gridy = yPosition++;
        importExportPanelConstraints.ipadx = 5;
        importExportPanelConstraints.ipady = 5;
        importExportPanelConstraints.insets = mainPanelInsets;
        importExportPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        mainPanel.add(importExportPanel, importExportPanelConstraints);

        JPanel endpointTablePane = buildEndpointsTable();
        callbacks.customizeUiComponent(endpointTablePane);

        JPanel displayPane = buildDisplayPane();
        callbacks.customizeUiComponent(displayPane);
        GridBagConstraints displayConstraints = new GridBagConstraints();
        displayConstraints.gridx = 0;
        displayConstraints.gridy = yPosition++;
        displayConstraints.insets = mainPanelInsets;
        displayConstraints.fill = GridBagConstraints.BOTH;
        displayConstraints.weightx = 1.0;
        displayConstraints.weighty = 1.0;
        displayConstraints.anchor = GridBagConstraints.NORTH;

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, endpointTablePane, displayPane);
        mainPanel.add(splitPane, displayConstraints);

        return mainPanel;
    }

    private JPanel buildOptionsPanel() {
        final JPanel optionsPanel = new JPanel();
        optionsPanel.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                boolean tabIsShowing = optionsPanel.isShowing();
                if (tabIsShowing) {
                    loadOptionsProperties();
                } else {
                    BurpPropertiesManager.getBurpPropertiesManager().saveProperties();
                }
            }
        });
        optionsPanel.setLayout(new GridBagLayout());
        Insets optionsPanelInsets = new Insets(10, 10, 10, 10);
        int yPosition = 0;

        JPanel autoOptionsPanel = buildAutoOptionsPanel();
        GridBagConstraints autoOptionsPanelConstraints = new GridBagConstraints();
        autoOptionsPanelConstraints.gridx = 0;
        autoOptionsPanelConstraints.gridy = yPosition++;
        autoOptionsPanelConstraints.ipadx = 5;
        autoOptionsPanelConstraints.ipady = 5;
        autoOptionsPanelConstraints.insets = optionsPanelInsets;
        autoOptionsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        optionsPanel.add(autoOptionsPanel, autoOptionsPanelConstraints);

        JSeparator autoOptionsPanelSeparator = new JSeparator(JSeparator.HORIZONTAL);
        callbacks.customizeUiComponent(autoOptionsPanelSeparator);
        GridBagConstraints autoOptionsPanelSeparatorConstraints = new GridBagConstraints();
        autoOptionsPanelSeparatorConstraints.gridx = 0;
        autoOptionsPanelSeparatorConstraints.gridy = yPosition++;
        autoOptionsPanelSeparatorConstraints.insets = optionsPanelInsets;
        autoOptionsPanelSeparatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        autoOptionsPanelSeparatorConstraints.anchor = GridBagConstraints.NORTH;
        optionsPanel.add(autoOptionsPanelSeparator, autoOptionsPanelSeparatorConstraints);

        JPanel sourcePanel = buildSourcePanel();
        GridBagConstraints sourcePanelConstraints = new GridBagConstraints();
        sourcePanelConstraints.gridx = 0;
        sourcePanelConstraints.gridy = yPosition++;
        sourcePanelConstraints.ipadx = 5;
        sourcePanelConstraints.ipady = 5;
        sourcePanelConstraints.insets = optionsPanelInsets;
        sourcePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        optionsPanel.add(sourcePanel, sourcePanelConstraints);

        JSeparator sourcePanelSeparator = new JSeparator(JSeparator.HORIZONTAL);
        callbacks.customizeUiComponent(sourcePanelSeparator);
        GridBagConstraints sourcePanelSeparatorConstraints = new GridBagConstraints();
        sourcePanelSeparatorConstraints.gridx = 0;
        sourcePanelSeparatorConstraints.gridy = yPosition++;
        sourcePanelSeparatorConstraints.insets = optionsPanelInsets;
        sourcePanelSeparatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        sourcePanelSeparatorConstraints.anchor = GridBagConstraints.NORTH;
        optionsPanel.add(sourcePanelSeparator, sourcePanelSeparatorConstraints);

        JPanel configPanel = buildConfigPanel();
        GridBagConstraints configPanelConstraints = new GridBagConstraints();
        configPanelConstraints.gridx = 0;
        configPanelConstraints.gridy = yPosition++;
        configPanelConstraints.ipadx = 5;
        configPanelConstraints.ipady = 5;
        configPanelConstraints.insets = optionsPanelInsets;
        configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        optionsPanel.add(configPanel, configPanelConstraints);

        JSeparator configPanelSeparator = new JSeparator(JSeparator.HORIZONTAL);
        callbacks.customizeUiComponent(configPanelSeparator);
        GridBagConstraints configPanelSeparatorConstraints = new GridBagConstraints();
        configPanelSeparatorConstraints.gridx = 0;
        configPanelSeparatorConstraints.gridy = yPosition++;
        configPanelSeparatorConstraints.insets = optionsPanelInsets;
        configPanelSeparatorConstraints.fill = GridBagConstraints.HORIZONTAL;
        configPanelSeparatorConstraints.anchor = GridBagConstraints.NORTH;
        optionsPanel.add(configPanelSeparator, configPanelSeparatorConstraints);

        JPanel targetPanel = buildTargetPanel();
        GridBagConstraints targetPanelConstraints = new GridBagConstraints();
        targetPanelConstraints.gridx = 0;
        targetPanelConstraints.gridy = yPosition++;
        targetPanelConstraints.ipadx = 5;
        targetPanelConstraints.ipady = 5;
        targetPanelConstraints.insets = optionsPanelInsets;
        targetPanelConstraints.weightx = 1.0;
        targetPanelConstraints.weighty = 1.0;
        targetPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
        optionsPanel.add(targetPanel, targetPanelConstraints);

        loadOptionsProperties();

        return optionsPanel;
    }


    private JPanel buildDisplayPane()
    {
        final JLabel panelTitle = new JLabel("Selected Endpoint", JLabel.LEFT);
        panelTitle.setForeground(new Color(236, 136, 0));
        Font font = panelTitle.getFont();
        panelTitle.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() + 4));
        panelTitle.setHorizontalAlignment(SwingConstants.LEFT);
        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BorderLayout());
        //displayArea.setText("\n" + "\n" + "\n" + "\n" + "\n" + "\n");
        callbacks.customizeUiComponent(displayArea);
        //displayArea.setEditable(false);
        JLabel titleLabel = new JLabel("Selected Endpoint");
        basePanel.add(panelTitle, BorderLayout.PAGE_START);
        JScrollPane scrollPane = new JScrollPane();
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());
        displayArea.setHorizontalAlignment(SwingConstants.LEFT);
        displayPanel.add(displayArea,BorderLayout.PAGE_START);
        JScrollPane scrollPane1 = new JScrollPane();
        scrollPane.setViewportView(displayPanel);
        //basePanel.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        //basePanel.add(new JScrollPane(displayPanel), BorderLayout.CENTER);
        basePanel.add(scrollPane, BorderLayout.CENTER);
        return basePanel;
    }

    private JPanel buildEndpointsTable()
    {
        Object[][] data = {};
        String[] columnNames =
                {"Detected Endpoints",
                        "Number of Detected Parameters",
                        "GET Method",
                        "POST Method",
                        "New/Modified",
                        "Endpoint"
                };

        DefaultTableModel dtm = new DefaultTableModel(data, columnNames){

            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable endpointsTable = new JTable(dtm);
        endpointsTable.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                String displayStr = new String();
                displayArea.setText(displayStr);
                EndpointDecorator decorator = (EndpointDecorator)endpointsTable.getModel().getValueAt(endpointsTable.getSelectedRow(), 5);
                Endpoint.Info endpoint = decorator.getEndpoint();
                if(decorator.getStatus() == EndpointDecorator.Status.NEW)
                {
                    displayStr = "<html><b>New Endpoint</b><br>";
                    displayStr = displayStr + "URL:<br>";
                }
                else
                    displayStr = displayStr + "<html> URL:<br>";

                displayStr = displayStr + "" + endpoint.getUrlPath() + "<br><br>Methods:<br>";
                // TODO - Gather all Endpoint objects pointing to the same endpoint and output their HTTP methods (Endpoints only have
                //  one HTTP method at a time now)
                if(endpoint.getHttpMethod().length() >4)
                    displayStr = displayStr + endpoint.getHttpMethod().substring(14);
                else
                    displayStr = displayStr + endpoint.getHttpMethod();


                displayStr = displayStr +"<br>Parameters and type:<br>";
                if(decorator.getStatus() == EndpointDecorator.Status.CHANGED)
                {
                    for (Map.Entry<String, RouteParameter> parameter : endpoint.getParameters().entrySet())
                    {   boolean found = false;
                        for (Map.Entry<String, RouteParameter> compParameter : decorator.getComparePoint().getParameters().entrySet())
                        {
                            if (parameter.getKey().equalsIgnoreCase(compParameter.getKey()))
                            {
                                found = true;
                                if(!parameter.getValue().getDataType().getDisplayName().equals(compParameter.getValue().getDataType().getDisplayName()))
                                    displayStr = displayStr + "<strong>" + parameter.getKey() + " - " + compParameter.getValue().getDataType().getDisplayName().toUpperCase() + " -> " + parameter.getValue().getDataType().getDisplayName().toUpperCase()+"</strong> (modified parameter type) <br>";
                                else
                                    displayStr = displayStr + parameter.getKey() + " - "+ parameter.getValue().getDataType().getDisplayName() + "<br>";
                                break;
                            }
                        }
                        if (!found)
                            displayStr = displayStr + "<strong>" + parameter.getKey() + "</strong> - <strong>" + parameter.getValue().getDataType().getDisplayName().toUpperCase() + "</strong> (added parameter)<br>";
                    }
                    for (Map.Entry<String, RouteParameter> compParameter : decorator.getComparePoint().getParameters().entrySet())
                    {   boolean found = false;
                        for (Map.Entry<String, RouteParameter> parameter : endpoint.getParameters().entrySet())
                        {
                            if (parameter.getKey().equalsIgnoreCase(compParameter.getKey()))
                            {
                                found = true;
                                break;
                            }
                        }
                        if(!found)
                            displayStr = displayStr + "<span style='text-decoration: line-through;'>" +compParameter.getKey() + " - " + compParameter.getValue().getDataType().getDisplayName().toUpperCase() + "</span> (removed parameter)<br>";
                    }
                }
                else
                {
                    for (Map.Entry<String, RouteParameter> parameter : endpoint.getParameters().entrySet())
                    {
                        displayStr = displayStr + parameter.getKey() + " - "+ parameter.getValue().getDataType().getDisplayName() + "<br>";
                    }
                }

                displayStr = displayStr + "</html>";
                displayArea.setText(displayStr);

            }
            @Override
            public void mousePressed(MouseEvent e) { }
            @Override
            public void mouseReleased(MouseEvent e) { }
            @Override
            public void mouseEntered(MouseEvent e) { }
            @Override
            public void mouseExited(MouseEvent e) { }
        });

        TableColumn tc = endpointsTable.getColumnModel().getColumn(2);
        tc.setCellEditor(endpointsTable.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(endpointsTable.getDefaultRenderer(Boolean.class));
        tc = endpointsTable.getColumnModel().getColumn(3);
        tc.setCellEditor(endpointsTable.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(endpointsTable.getDefaultRenderer(Boolean.class));
        tc = endpointsTable.getColumnModel().getColumn(4);
        tc.setCellEditor(endpointsTable.getDefaultEditor(Boolean.class));
        tc.setCellRenderer(endpointsTable.getDefaultRenderer(Boolean.class));
        endpointsTable.getColumnModel().getColumn(5).setMinWidth(0);
        endpointsTable.getColumnModel().getColumn(5).setMaxWidth(0);
        endpointsTable.getColumnModel().getColumn(5).setWidth(0);
        JScrollPane endpointsTablePane = new JScrollPane(endpointsTable);

        endpointsTable.setFillsViewportHeight(true);
        callbacks.customizeUiComponent(endpointsTable);
        BurpPropertiesManager.getBurpPropertiesManager().setEndpointsTable(endpointsTable);

        JPanel basePanel = new JPanel();
        basePanel.setLayout(new BorderLayout());
        JLabel countLabel = new JLabel();
        callbacks.customizeUiComponent(countLabel);
        BurpPropertiesManager.getBurpPropertiesManager().setCountLabel(countLabel);
        countLabel.setBorder(null);
        countLabel.setText(" ");
        basePanel.add(countLabel, BorderLayout.PAGE_START);
        basePanel.add(endpointsTablePane, BorderLayout.CENTER);

        return basePanel;

    }


    private JPanel buildImportExportPanel()
    {
        JPanel importExportPanel = new JPanel();
        importExportPanel.setLayout(new GridBagLayout());
        int yPosition = 0;

        addPanelTitleToGridBagLayout("Source Code Analysis", importExportPanel, yPosition++);
        addPanelDescriptionToGridBagLayout("Use Attack Surface Detector to analyze the server side source code to detect endpoints and parameters and import them into Burp." , importExportPanel, yPosition++);
        addPanelDescriptionToGridBagLayout("These results may include URL endpoints and optional parameters a spider may not find." , importExportPanel, yPosition++);

        JButton localEndpointsButton = new LocalEndpointsButton(getUiComponent(), callbacks);
        callbacks.customizeUiComponent(localEndpointsButton);

        localEndpointsButton.setSize(300, 30);
        localEndpointsButton.setLocation(10,400);

        GridBagConstraints gridBagConstraintsLocal = new GridBagConstraints();
        gridBagConstraintsLocal.gridwidth = 1;
        gridBagConstraintsLocal.gridx = 1;
        gridBagConstraintsLocal.gridy = ++yPosition;
        gridBagConstraintsLocal.ipadx = 5;
        gridBagConstraintsLocal.ipady = 5;
        gridBagConstraintsLocal.anchor = GridBagConstraints.NORTHWEST;

        importExportPanel.add(localEndpointsButton, gridBagConstraintsLocal);
        return importExportPanel;
    }


    private JPanel buildSourcePanel() {
        final JPanel sourcePanel = new JPanel();
        sourcePanel.setLayout(new GridBagLayout());
        int yPosition = 0;
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new GridBagLayout());
        JPanel newSourceDialog = new JPanel();
        newSourceDialog.setLayout(new GridBagLayout());
        JPanel oldSourceDialog = new JPanel();
        oldSourceDialog.setLayout(new GridBagLayout());

        final JLabel sourcePanelTitle = addPanelTitleToGridBagLayout("Local Source Code", sourcePanel, yPosition++);
        final JLabel sourcePanelDescription = addPanelDescriptionToGridBagLayout("This setting lets you configure the location of your source code.", sourcePanel, yPosition++);
        final JLabel differenceGeneratorDescription = addPanelDescriptionToGridBagLayout("<html><br>You can optionally choose to compare two different versions of the source code, and the Attack Surface Detector <br>will highlight endpoints and parameters that are new or modified in the newer version of the source code.</html>", sourcePanel, yPosition++);
        final JLabel sourcePanelDescription2 = addPanelDescriptionToGridBagLayout(" ", sourcePanel, yPosition++);

        final JButton sourceFolderBrowseButton = new JButton("Select folder or zip file ...");
        sourceFolderBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                String currentDirectory = sourceFolderField.getText();
                if ((currentDirectory == null) || (currentDirectory.trim().equals(""))) {
                    currentDirectory = System.getProperty("user.home");
                }
                chooser.setCurrentDirectory(new java.io.File(currentDirectory));
                chooser.setDialogTitle("Please select the folder containing the source code");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setAcceptAllFileFilterUsed(false);
                chooser.addChoosableFileFilter( new FileNameExtensionFilter("*.zip | ZIP archive", "zip"));
                chooser.addChoosableFileFilter( new FileNameExtensionFilter("*.war | Web application archive", "war"));
                chooser.addChoosableFileFilter( new FileFilter()
                {
                    public boolean accept(File f)
                    {
                        return f.isDirectory();
                    }

                    public String getDescription()
                    {
                        return "dir | Directory/Folder";
                    }
                });
                if (chooser.showOpenDialog(sourcePanel) == JFileChooser.APPROVE_OPTION) {
                    sourceFolderField.setText(chooser.getSelectedFile().getAbsolutePath());
                    BurpPropertiesManager.getBurpPropertiesManager().setSourceFolder(sourceFolderField.getText());
                }
            }
        });
        sourceFolderField = addTextFieldToGridBagLayout("Source code to analyze:", sourcePanel, yPosition++, BurpPropertiesManager.SOURCE_FOLDER_KEY, sourceFolderBrowseButton);

        final JButton oldSourceFolderBrowseButton = new JButton("Select folder or zip file ...");
        oldSourceFolderBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JFileChooser chooser2 = new JFileChooser();
                String currentDirectory = oldSourceFolderField.getText();
                if ((currentDirectory == null) || (currentDirectory.trim().equals(""))) {
                    currentDirectory = System.getProperty("user.home");
                }
                chooser2.setCurrentDirectory(new java.io.File(currentDirectory));
                chooser2.setDialogTitle("Please select the folder containing the source code");
                chooser2.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser2.setAcceptAllFileFilterUsed(false);
                chooser2.addChoosableFileFilter( new FileNameExtensionFilter("*.zip | ZIP archive", "zip"));
                chooser2.addChoosableFileFilter( new FileNameExtensionFilter("*.war | Web application archive", "war"));
                chooser2.addChoosableFileFilter( new FileFilter()
                {
                    public boolean accept(File f)
                    {
                        return f.isDirectory();
                    }

                    public String getDescription()
                    {
                        return "dir | Directory/Folder";
                    }
                });
                if (chooser2.showOpenDialog(sourcePanel) == JFileChooser.APPROVE_OPTION) {
                    oldSourceFolderField.setText(chooser2.getSelectedFile().getAbsolutePath());
                    BurpPropertiesManager.getBurpPropertiesManager().setOldSourceFolder(oldSourceFolderField.getText());
                }
            }
        });


        oldSourceFolderField = addTextFieldToGridBagLayout("Comparison source code (optional):", sourcePanel, yPosition++, BurpPropertiesManager.OLD_SOURCE_FOLDER_KEY, oldSourceFolderBrowseButton);

        return sourcePanel;
    }

    private JPanel buildConfigPanel() {
        final JPanel configPanel = new JPanel();
        configPanel.setLayout(new GridBagLayout());
        int yPosition = 0;

        final JLabel configPanelTitle = addPanelTitleToGridBagLayout("Burp Configuration File", configPanel, yPosition++);
        final JLabel configPanelDescription = addPanelDescriptionToGridBagLayout("This setting lets you configure the location of your Burp configuration file.", configPanel, yPosition++);

        final JButton configFileBrowseButton = new JButton("Select file ...");
        configFileBrowseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                String currentDirectory = configFileField.getText();
                if ((currentDirectory == null) || (currentDirectory.trim().equals(""))) {
                    currentDirectory = System.getProperty("user.home");
                }
                chooser.setCurrentDirectory(new java.io.File(currentDirectory));
                chooser.setDialogTitle("Please select the burp configuration file");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(configPanel) == JFileChooser.APPROVE_OPTION) {
                    configFileField.setText(chooser.getSelectedFile().getAbsolutePath());
                    BurpPropertiesManager.getBurpPropertiesManager().setConfigFile(configFileField.getText());
                }
            }
        });
        configFileField = addTextFieldToGridBagLayout("Location of configuration file:", configPanel, yPosition++, BurpPropertiesManager.CONFIG_FILE_KEY, configFileBrowseButton);

        return configPanel;
    }

    private JPanel buildTargetPanel() {
        final JPanel targetPanel = new JPanel();
        targetPanel.setLayout(new GridBagLayout());
        int yPosition = 0;

        ActionListener applicationCheckBoxHttpActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BurpPropertiesManager.getBurpPropertiesManager().setUseHttps(useHttpField.isSelected());
            }
        };

        final JLabel targetPanelTitle = addPanelTitleToGridBagLayout("Target Configuration", targetPanel, yPosition++);
        targetHostField = addTextFieldToGridBagLayout("Host:", targetPanel, yPosition++, BurpPropertiesManager.TARGET_HOST_KEY);
        targetPortField = addTextFieldToGridBagLayout("Port:", targetPanel, yPosition++, BurpPropertiesManager.TARGET_PORT_KEY);
        targetPathField = addTextFieldToGridBagLayout("Path (optional):", targetPanel, yPosition++, BurpPropertiesManager.TARGET_PATH_KEY);
        useHttpField = addCheckBoxToGridBagLayout("Use HTTPS", targetPanel, yPosition++, applicationCheckBoxHttpActionListener);
        useHttpField.setSelected(BurpPropertiesManager.getBurpPropertiesManager().getUseHttps());
        BurpPropertiesManager.getBurpPropertiesManager().setUseHttpsField(useHttpField);
        PlainDocument portDoc = (PlainDocument)targetPortField.getDocument();
        portDoc.setDocumentFilter(new PortFilter());
        return targetPanel;
    }

    private JPanel buildAutoOptionsPanel() {
        final JPanel autoOptionsPanel = new JPanel();
        autoOptionsPanel.setLayout(new GridBagLayout());
        int yPosition = 0;

        final JLabel autoOptionsPanelTitle = addPanelTitleToGridBagLayout("Attack Surface Detector Plugin Behavior", autoOptionsPanel, yPosition++);
        ActionListener applicationCheckBoxSpiderActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BurpPropertiesManager.getBurpPropertiesManager().setAutoSpider(autoSpiderField.isSelected());
                if(BurpPropertiesManager.getBurpPropertiesManager().getAutoSpider() && BurpPropertiesManager.getBurpPropertiesManager().isProVersion())
                {
                    autoScanField.setEnabled(true);
                    autoScanText.setForeground(Color.BLACK);
                }
                else
                {
                    autoScanField.setEnabled(false);
                    BurpPropertiesManager.getBurpPropertiesManager().setAutoScan(false);
                    autoScanField.setSelected(false);
                    autoScanText.setForeground(Color.GRAY);
                }
            }
        };

        ActionListener applicationCheckBoxScanActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BurpPropertiesManager.getBurpPropertiesManager().setAutoScan(autoScanField.isSelected());
            }
        };
        autoScanText = new JLabel("Automatically start active scanner after automatic spider: ");
        autoSpiderField = addCheckBoxToGridBagLayout(new JLabel("Automatically start spider after importing endpoints: "), autoOptionsPanel, yPosition++, applicationCheckBoxSpiderActionListener);
        if(BurpPropertiesManager.getBurpPropertiesManager().isProVersion())
        {
            autoScanField = addCheckBoxToGridBagLayout(autoScanText, autoOptionsPanel, yPosition++, applicationCheckBoxScanActionListener);
            if (BurpPropertiesManager.getBurpPropertiesManager().getAutoSpider())
            {
                autoScanField.setEnabled(true);
                autoScanText.setForeground(Color.BLACK);
            }
            else
            {
                autoScanText.setForeground(Color.GRAY);
                autoScanField.setEnabled(false);
            }
        }
        else
        {
            autoScanField = addCheckBoxToGridBagLayout(autoScanText, autoOptionsPanel, yPosition++, applicationCheckBoxScanActionListener);
            autoScanField.setEnabled(false);
            profMessage = addPanelLabelToGridBagLayout("\t *Note this option is only available with the Pro Version", autoOptionsPanel, yPosition++);
        }

        return autoOptionsPanel;
    }


    @Override
    public String getTabCaption()
    {
        return "Attack Surface Detector";
    }

    @Override
    public Component getUiComponent()
    {
        return tabbedPane;
    }

    private class ThreadFixPropertyFieldListener implements DocumentListener, FocusListener {
        private JTextField jTextField;
        private String propertyName;
        private Runnable runnable;

        private String lastValue = null;

        public ThreadFixPropertyFieldListener(JTextField jTextField, String propertyName) {
            this(jTextField, propertyName, null);
        }

        public ThreadFixPropertyFieldListener(JTextField jTextField, String propertyName, Runnable runnable) {
            this.jTextField = jTextField;
            this.propertyName = propertyName;
            this.runnable = runnable;
        }

        protected void update() {
            BurpPropertiesManager.getBurpPropertiesManager().setPropertyValue(propertyName, jTextField.getText().trim());
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void focusGained(FocusEvent e) {
            lastValue = jTextField.getText().trim();
        }

        @Override
        public void focusLost(FocusEvent e) {
            String currentValue = jTextField.getText().trim();
            if (!currentValue.equals(lastValue)) {
                update();
            }
        }
    }

    private void loadOptionsProperties() {
        BurpPropertiesManager burpPropertiesManager = BurpPropertiesManager.getBurpPropertiesManager();
        sourceFolderField.setText(burpPropertiesManager.getSourceFolder());
        oldSourceFolderField.setText(burpPropertiesManager.getOldSourceFolder());
        configFileField.setText(burpPropertiesManager.getConfigFile());
        targetHostField.setText(burpPropertiesManager.getTargetHost());
        targetPathField.setText(burpPropertiesManager.getTargetPath());
        targetPortField.setText(burpPropertiesManager.getTargetPort());
    }


    private JLabel addPanelTitleToGridBagLayout(String titleText, Container gridBagContainer, int yPosition) {
        final JLabel panelTitle = new JLabel(titleText, JLabel.LEFT);
        panelTitle.setForeground(new Color(236, 136, 0));
        Font font = panelTitle.getFont();
        panelTitle.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize() + 4));
        panelTitle.setHorizontalAlignment(SwingConstants.LEFT);
        callbacks.customizeUiComponent(panelTitle);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagContainer.add(panelTitle, gridBagConstraints);
        return panelTitle;
    }

    private JLabel addPanelLabelToGridBagLayout(String titleText, Container gridBagContainer, int yPosition) {
        final JLabel panelTitle = new JLabel(titleText);
        panelTitle.setForeground(new Color(236, 136, 0));
        Font font = panelTitle.getFont();
        panelTitle.setFont(new Font(font.getFontName(), font.getStyle(), font.getSize()));
        panelTitle.setHorizontalAlignment(SwingConstants.LEFT);
        callbacks.customizeUiComponent(panelTitle);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagContainer.add(panelTitle, gridBagConstraints);
        return panelTitle;
    }

    private JLabel addPanelDescriptionToGridBagLayout(String descriptionText, Container gridBagContainer, int yPosition) {
        final JLabel panelDescription = new JLabel(descriptionText);
        panelDescription.setHorizontalAlignment(SwingConstants.LEFT);
        callbacks.customizeUiComponent(panelDescription);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagContainer.add(panelDescription, gridBagConstraints);
        return panelDescription;
    }

    private JLabel addErrorMessageToGridBagLayout(Container gridBagContainer, int yPosition) {
        final JLabel errorMessage = new JLabel("");
        errorMessage.setForeground(new Color(255, 0, 0));
        errorMessage.setHorizontalAlignment(SwingConstants.LEFT);
        callbacks.customizeUiComponent(errorMessage);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 10;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagContainer.add(errorMessage, gridBagConstraints);
        return errorMessage;
    }

    private JTextField addTextFieldToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, String propertyKey) {
        return addTextFieldToGridBagLayout(labelText, gridBagContainer, yPosition, propertyKey, null, null);
    }

    private JTextField addTextFieldToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, String propertyKey, Runnable threadFixPropertyFieldListenerRunnable) {
        return addTextFieldToGridBagLayout(labelText, gridBagContainer, yPosition, propertyKey, threadFixPropertyFieldListenerRunnable, null);
    }

    private JTextField addTextFieldToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, String propertyKey, JButton button) {
        return addTextFieldToGridBagLayout(labelText, gridBagContainer, yPosition, propertyKey, null, button);
    }

    private JTextField addTextFieldToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, String propertyKey, Runnable threadFixPropertyFieldListenerRunnable, JButton button) {
        JLabel textFieldLabel = new JLabel(labelText);
        callbacks.customizeUiComponent(textFieldLabel);
        textFieldLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagContainer.add(textFieldLabel, gridBagConstraints);

        JTextField textField = new JTextField(40);
        callbacks.customizeUiComponent(textField);
        textField.addFocusListener(new ThreadFixPropertyFieldListener(textField, propertyKey, threadFixPropertyFieldListenerRunnable));
        gridBagConstraints = new GridBagConstraints();
        if (button == null) {
            gridBagConstraints.gridwidth = 2;
        } else {
            gridBagConstraints.gridwidth = 1;
        }
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagContainer.add(textField, gridBagConstraints);

        if (button != null) {
            callbacks.customizeUiComponent(button);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = yPosition;
            gridBagConstraints.ipadx = 5;
            gridBagConstraints.ipady = 5;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
            gridBagContainer.add(button, gridBagConstraints);
        }

        return textField;
    }


    private JComboBox addComboBoxToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, ActionListener actionListener) {
        return addComboBoxToGridBagLayout(labelText, gridBagContainer, yPosition, actionListener, null);
    }

    private JComboBox addComboBoxToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, ActionListener actionListener, JButton button) {
        JLabel textFieldLabel = new JLabel(labelText);
        callbacks.customizeUiComponent(textFieldLabel);
        textFieldLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagContainer.add(textFieldLabel, gridBagConstraints);

        JComboBox comboBox = new JComboBox();
        comboBox.setEnabled(false);
        callbacks.customizeUiComponent(comboBox);
        gridBagConstraints = new GridBagConstraints();
        if (button == null) {
            gridBagConstraints.gridwidth = 2;
        } else {
            gridBagConstraints.gridwidth = 1;
        }
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagContainer.add(comboBox, gridBagConstraints);

        if (button != null) {
            callbacks.customizeUiComponent(button);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = yPosition;
            gridBagConstraints.ipadx = 5;
            gridBagConstraints.ipady = 5;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
            gridBagContainer.add(button, gridBagConstraints);
        }

        comboBox.addActionListener(actionListener);

        return comboBox;
    }

    private JCheckBox addCheckBoxToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, ActionListener actionListener) {
        return addCheckBoxToGridBagLayout(labelText, gridBagContainer, yPosition, actionListener, null);
    }

    private JCheckBox addCheckBoxToGridBagLayout(JLabel label, Container gridBagContainer, int yPosition, ActionListener actionListener) {
        return addCheckBoxToGridBagLayout(label, gridBagContainer, yPosition, actionListener, null);
    }

    private JCheckBox addCheckBoxToGridBagLayout(String labelText, Container gridBagContainer, int yPosition, ActionListener actionListener, JButton button) {
        JLabel textFieldLabel = new JLabel(labelText);
        callbacks.customizeUiComponent(textFieldLabel);
        JCheckBox checkBox = new JCheckBox();
        callbacks.customizeUiComponent(checkBox);

        textFieldLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        //gridBagConstraints.ipadx = 5;
        //gridBagConstraints.ipady = 5;
        //gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.insets = new Insets(0,22,0,0);
        gridBagContainer.add(textFieldLabel, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        if (button == null) {
            gridBagConstraints.gridwidth = 2;
        } else {
            gridBagConstraints.gridwidth = 1;
        }
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        //gridBagConstraints.ipadx = 5;
        //gridBagConstraints.ipady = 5;
        //gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagContainer.add(checkBox, gridBagConstraints);

        if (button != null) {
            callbacks.customizeUiComponent(button);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = yPosition;
            gridBagConstraints.ipadx = 5;
            gridBagConstraints.ipady = 5;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
            gridBagContainer.add(button, gridBagConstraints);
        }

        checkBox.addActionListener(actionListener);

        return checkBox;
    }

    private JCheckBox addCheckBoxToGridBagLayout(JLabel textFieldLabel, Container gridBagContainer, int yPosition, ActionListener actionListener, JButton button) {
        callbacks.customizeUiComponent(textFieldLabel);
        JCheckBox checkBox = new JCheckBox();
        callbacks.customizeUiComponent(checkBox);

        textFieldLabel.setHorizontalAlignment(SwingConstants.LEFT);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        //gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagContainer.add(checkBox, gridBagConstraints);


        gridBagConstraints = new GridBagConstraints();
        if (button == null) {
            gridBagConstraints.gridwidth = 2;
        } else {
            gridBagConstraints.gridwidth = 1;
        }
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = yPosition;
        gridBagConstraints.ipadx = 5;
        gridBagConstraints.ipady = 5;
        //gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagContainer.add(textFieldLabel, gridBagConstraints);

        if (button != null) {
            callbacks.customizeUiComponent(button);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.gridx = 3;
            gridBagConstraints.gridy = yPosition;
            gridBagConstraints.ipadx = 5;
            gridBagConstraints.ipady = 5;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHEAST;
            gridBagContainer.add(button, gridBagConstraints);
        }

        checkBox.addActionListener(actionListener);

        return checkBox;
    }
}

class PortFilter extends DocumentFilter {
    static final int maxLength = 5;
    @Override
    public void insertString(FilterBypass fb, int offset, String string,
                             AttributeSet attr) throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.insert(offset, string);
        int val = Integer.parseInt(sb.toString());

        if (isInteger(sb.toString()) && sb.length() <= maxLength && val <= 65535) {
            super.insertString(fb, offset, string, attr);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private boolean isInteger(String text) {
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text,
                        AttributeSet attrs) throws BadLocationException {

        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.replace(offset, offset + length, text);
        int val = Integer.parseInt(sb.toString());

        if (isInteger(sb.toString()) && (sb.length() <= maxLength) && val <= 65535) {
            super.replace(fb, offset, length, text, attrs);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

    }

    @Override
    public void remove(FilterBypass fb, int offset, int length)
            throws BadLocationException {
        Document doc = fb.getDocument();
        StringBuilder sb = new StringBuilder();
        sb.append(doc.getText(0, doc.getLength()));
        sb.delete(offset, offset + length);

        if ((isInteger(sb.toString()) && (sb.length() <= maxLength)) || (sb.length() == 0)) {
            super.remove(fb, offset, length);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }

    }
}