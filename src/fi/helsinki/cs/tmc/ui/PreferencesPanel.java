package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.actions.RefreshCoursesAction;
import fi.helsinki.cs.tmc.data.Course;
import fi.helsinki.cs.tmc.data.CourseList;
import fi.helsinki.cs.tmc.model.TmcSettings;
import fi.helsinki.cs.tmc.utilities.DelayedRunner;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * The settings panel.
 * 
 * This is missing the "OK" and "Cancel" buttons because it is placed in
 * a dialog that provides these.
 */
/*package*/ class PreferencesPanel extends JPanel implements PreferencesUI {
    
    private String usernameFieldName = "username";
    
    private DelayedRunner refreshRunner = new DelayedRunner();
    private RefreshSettings lastRefreshSettings = null;
    
    private static class RefreshSettings {
        private final String username;
        private final String password;
        private final String baseUrl;

        public RefreshSettings(String username, String password, String baseUrl) {
            this.username = username;
            this.password = password;
            this.baseUrl = baseUrl;
        }
        
        public boolean isAllSet() {
            return username != null && password != null && baseUrl != null;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RefreshSettings) {
                RefreshSettings that = (RefreshSettings)obj;
                return
                        ObjectUtils.equals(this.username, that.username) &&
                        ObjectUtils.equals(this.password, that.password) &&
                        ObjectUtils.equals(this.baseUrl, that.baseUrl);
            } else {
                return false;
            }
        }
        
        @Override
        public int hashCode() {
            return 0;
        }
        
    }
    
    /*package*/ PreferencesPanel() {
        initComponents();
        makeLoadingLabelNicer();
        
        setUpFieldChangeListeners();
        setUsernameFieldName(usernameFieldName);
    }
    
    @Override
    public String getUsername() {
        return usernameTextField.getText().trim();
    }
    
    @Override
    public void setUsername(String username) {
        usernameTextField.setText(username);
    }
    
    @Override
    public final void setUsernameFieldName(String usernameFieldName) {
        this.usernameFieldName = usernameFieldName;
        this.usernameLabel.setText(StringUtils.capitalize(usernameFieldName));
        fieldChanged();
    }

    @Override
    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    @Override
    public void setPassword(String password) {
        passwordField.setText(password);
    }

    @Override
    public boolean getShouldSavePassword() {
        return savePasswordCheckBox.isSelected();
    }

    @Override
    public void setShouldSavePassword(boolean shouldSavePassword) {
        savePasswordCheckBox.setSelected(shouldSavePassword);
    }
    
    @Override
    public String getServerBaseUrl() {
        return serverAddressTextField.getText().trim();
    }
    
    @Override
    public void setServerBaseUrl(String baseUrl) {
        serverAddressTextField.setText(baseUrl);
    }
    
    @Override
    public String getProjectDir() {
        return projectFolderTextField.getText().trim();
    }
    
    @Override
    public void setProjectDir(String projectDir) {
        projectFolderTextField.setText(projectDir);
    }
    
    @Override
    public void setAvailableCourses(CourseList courses) {
        setCourseListRefreshInProgress(true); // To avoid changes triggering a new reload
        
        String previousSelectedCourseName = null;
        if (getSelectedCourse() != null) {
            previousSelectedCourseName = getSelectedCourse().getName();
        }
        
        coursesComboBox.removeAllItems();
        int newSelectedIndex = -1;
        for (int i = 0; i < courses.size(); ++i) {
            Course course = courses.get(i);
            coursesComboBox.addItem(courses.get(i));
            
            if (course.getName().equals(previousSelectedCourseName)) {
                newSelectedIndex = i;
            }
        }
        
        coursesComboBox.setSelectedIndex(newSelectedIndex);
        
        // Process any change events before enabling course selection
        // to avoid triggering another refresh.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setCourseListRefreshInProgress(false);
            }
        });
    }
    
    @Override
    public void courseRefreshFailedOrCanceled() {
        setCourseListRefreshInProgress(false);
    }
    
    @Override
    public void setSelectedCourse(Course course) {
        coursesComboBox.setSelectedItem(course);
    }
    
    @Override
    public Course getSelectedCourse() {
        Object item = coursesComboBox.getSelectedItem();
        if (item instanceof Course) {
            return (Course)item;
        } else { // because the combobox isn't populated yet
            return null;
        }
    }
    
    public TmcSettings getSettings() {
        TmcSettings settings = TmcSettings.getTransient();
        settings.setUsername(getUsername());
        settings.setPassword(getPassword());
        settings.setServerBaseUrl(getServerBaseUrl());
        settings.setProjectRootDir(getProjectDir());
        return settings;
    }
    
    private RefreshSettings getRefreshSettings() {
        return new RefreshSettings(getUsername(), getPassword(), getServerBaseUrl());
    }
    
    
    private void makeLoadingLabelNicer() {
        try {
            courseListReloadingLabel.setIcon(new javax.swing.ImageIcon(this.getClass().getResource("loading-spinner.gif")));
        } catch (Exception e) {
            return;
        }
        courseListReloadingLabel.setText("");
    }
    
    private void setCourseListRefreshInProgress(boolean inProgress) {
        refreshCoursesBtn.setEnabled(!inProgress);
        coursesComboBox.setEnabled(!inProgress);
        courseListReloadingLabel.setVisible(inProgress);
    }
    
    private void setUpFieldChangeListeners() {
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fieldChanged();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                fieldChanged();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                fieldChanged();
            }
        };
        usernameTextField.getDocument().addDocumentListener(docListener);
        passwordField.getDocument().addDocumentListener(docListener);
        serverAddressTextField.getDocument().addDocumentListener(docListener);
        projectFolderTextField.getDocument().addDocumentListener(docListener);
        
        coursesComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fieldChanged();
            }
        });
    }
    
    private void fieldChanged() {
        updateAdvice();
        
        if (canProbablyRefreshCourseList() && !alreadyRefreshingCourseList() && settingsChangedSinceLastRefresh()) {
            startRefreshingCourseList(true);
        }
    }
    
    private void updateAdvice() {
        ArrayList<String> advices = new ArrayList<String>();
        if (usernameTextField.getText().isEmpty()) {
            advices.add("fill in " + StringUtils.uncapitalize(usernameFieldName));
        }
        if (passwordField.getPassword().length == 0) {
            advices.add("fill in password");
        }
        if (serverAddressTextField.getText().isEmpty()) {
            advices.add("fill in server address");
        }
        if (projectFolderTextField.getText().isEmpty()) {
            advices.add("select folder for projects");
        }
        if (coursesComboBox.getSelectedIndex() == -1) {
            advices.add("select course");
        }
        
        if (!advices.isEmpty()) {
            String advice = "Please " + joinWithCommasAndAnd(advices) + ".";
            adviceLabel.setText(advice);
        } else {
            adviceLabel.setText("");
        }
    }
    
    private String joinWithCommasAndAnd(List<String> strings) {
        if (strings.isEmpty()) {
            return "";
        } else if (strings.size() == 1) {
            return strings.get(0);
        } else {
            String s = "";
            for (int i = 0; i < strings.size() - 2; ++i) {
                s += strings.get(i) + ", ";
            }
            s += strings.get(strings.size() - 2);
            s += " and ";
            s += strings.get(strings.size() - 1);
            return s;
        }
    }
    
    private boolean canProbablyRefreshCourseList() {
        return getRefreshSettings().isAllSet();
    }
    
    private boolean alreadyRefreshingCourseList() {
        return courseListReloadingLabel.isVisible();
    }
    
    private boolean settingsChangedSinceLastRefresh() {
        return lastRefreshSettings == null || !lastRefreshSettings.equals(getRefreshSettings());
    }
    
    private void startRefreshingCourseList(boolean failSilently) {
        final RefreshCoursesAction action = new RefreshCoursesAction(getSettings());
        action.setFailSilently(failSilently);
        refreshRunner.setTask(new Runnable() {
            @Override
            public void run() {
                setCourseListRefreshInProgress(true);
                lastRefreshSettings = getRefreshSettings();
                action.actionPerformed(null);
            }
        });
    }
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        usernameLabel = new javax.swing.JLabel();
        usernameTextField = new javax.swing.JTextField();
        serverAddressLabel = new javax.swing.JLabel();
        serverAddressTextField = new javax.swing.JTextField();
        projectFolderLabel = new javax.swing.JLabel();
        projectFolderTextField = new javax.swing.JTextField();
        folderChooserBtn = new javax.swing.JButton();
        refreshCoursesBtn = new javax.swing.JButton();
        coursesLabel = new javax.swing.JLabel();
        coursesComboBox = new javax.swing.JComboBox();
        adviceLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        passwordLabel = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();
        savePasswordCheckBox = new javax.swing.JCheckBox();
        courseListReloadingLabel = new javax.swing.JLabel();

        usernameLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.usernameLabel.text")); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, usernameTextField, org.jdesktop.beansbinding.ObjectProperty.create(), usernameLabel, org.jdesktop.beansbinding.BeanProperty.create("labelFor"));
        bindingGroup.addBinding(binding);

        usernameTextField.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.usernameTextField.text")); // NOI18N
        usernameTextField.setPreferredSize(new java.awt.Dimension(150, 27));

        serverAddressLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.serverAddressLabel.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, serverAddressTextField, org.jdesktop.beansbinding.ObjectProperty.create(), serverAddressLabel, org.jdesktop.beansbinding.BeanProperty.create("labelFor"));
        bindingGroup.addBinding(binding);

        serverAddressTextField.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.serverAddressTextField.text")); // NOI18N
        serverAddressTextField.setPreferredSize(new java.awt.Dimension(250, 27));
        serverAddressTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                serverAddressTextFieldActionPerformed(evt);
            }
        });

        projectFolderLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.projectFolderLabel.text")); // NOI18N

        projectFolderTextField.setEditable(false);
        projectFolderTextField.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.projectFolderTextField.text")); // NOI18N
        projectFolderTextField.setEnabled(false);
        projectFolderTextField.setPreferredSize(new java.awt.Dimension(250, 27));

        folderChooserBtn.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.folderChooserBtn.text")); // NOI18N
        folderChooserBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderChooserBtnActionPerformed(evt);
            }
        });

        refreshCoursesBtn.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.refreshCoursesBtn.text")); // NOI18N
        refreshCoursesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshCoursesBtnActionPerformed(evt);
            }
        });

        coursesLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.coursesLabel.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, coursesComboBox, org.jdesktop.beansbinding.ObjectProperty.create(), coursesLabel, org.jdesktop.beansbinding.BeanProperty.create("labelFor"));
        bindingGroup.addBinding(binding);

        coursesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        adviceLabel.setForeground(new java.awt.Color(255, 102, 0));
        adviceLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.adviceLabel.text")); // NOI18N

        passwordLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.passwordLabel.text")); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, passwordField, org.jdesktop.beansbinding.ObjectProperty.create(), passwordLabel, org.jdesktop.beansbinding.BeanProperty.create("labelFor"));
        bindingGroup.addBinding(binding);

        passwordField.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.passwordField.text")); // NOI18N
        passwordField.setPreferredSize(new java.awt.Dimension(109, 27));

        savePasswordCheckBox.setSelected(true);
        savePasswordCheckBox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.savePasswordCheckBox.text")); // NOI18N

        courseListReloadingLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.courseListReloadingLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 679, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(projectFolderLabel)
                                .addGap(55, 55, 55)
                                .addComponent(projectFolderTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 382, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(folderChooserBtn))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(usernameLabel)
                                    .addComponent(serverAddressLabel)
                                    .addComponent(coursesLabel)
                                    .addComponent(passwordLabel))
                                .addGap(78, 78, 78)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(serverAddressTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(coursesComboBox, 0, 276, Short.MAX_VALUE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(refreshCoursesBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(courseListReloadingLabel))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(usernameTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE)
                                            .addComponent(passwordField, javax.swing.GroupLayout.DEFAULT_SIZE, 346, Short.MAX_VALUE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(savePasswordCheckBox)))))
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(adviceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE)
                        .addGap(410, 410, 410))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(adviceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(folderChooserBtn)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(projectFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(projectFolderLabel)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(14, 14, 14)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(usernameLabel)
                    .addComponent(usernameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passwordLabel)
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(savePasswordCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(serverAddressLabel)
                    .addComponent(serverAddressTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(coursesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(coursesLabel)
                    .addComponent(refreshCoursesBtn)
                    .addComponent(courseListReloadingLabel))
                .addContainerGap())
        );

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * The method is used to select the default save folder for downloaded exercises.
     * It is called when the user presses the "Browse" button on the preferences window.
     * @param evt 
     */
    private void folderChooserBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderChooserBtnActionPerformed

        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int choice = folderChooser.showOpenDialog(this);
        if (choice == JFileChooser.CANCEL_OPTION) {
            return;
        }
        File projectDefaultFolder = folderChooser.getSelectedFile();

        projectFolderTextField.setText(projectDefaultFolder.getAbsolutePath());
    }//GEN-LAST:event_folderChooserBtnActionPerformed

    private void refreshCoursesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshCoursesBtnActionPerformed
        startRefreshingCourseList(false);
    }//GEN-LAST:event_refreshCoursesBtnActionPerformed

    private void serverAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverAddressTextFieldActionPerformed
        // Pressing return in the server address field presses the refresh button
        refreshCoursesBtn.doClick();
    }//GEN-LAST:event_serverAddressTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel adviceLabel;
    private javax.swing.JLabel courseListReloadingLabel;
    private javax.swing.JComboBox coursesComboBox;
    private javax.swing.JLabel coursesLabel;
    private javax.swing.JButton folderChooserBtn;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel projectFolderLabel;
    private javax.swing.JTextField projectFolderTextField;
    private javax.swing.JButton refreshCoursesBtn;
    private javax.swing.JCheckBox savePasswordCheckBox;
    private javax.swing.JLabel serverAddressLabel;
    private javax.swing.JTextField serverAddressTextField;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JTextField usernameTextField;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
