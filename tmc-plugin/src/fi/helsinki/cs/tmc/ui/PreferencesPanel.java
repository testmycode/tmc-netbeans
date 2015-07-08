package fi.helsinki.cs.tmc.ui;

import com.google.common.util.concurrent.FutureCallback;
import fi.helsinki.cs.tmc.actions.RefreshCoursesAction;
import hy.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.model.NBTmcSettings;
import fi.helsinki.cs.tmc.tailoring.SelectedTailoring;
import fi.helsinki.cs.tmc.utilities.BgTaskListener;
import fi.helsinki.cs.tmc.utilities.DelayedRunner;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private ConvenientDialogDisplayer dialogs = ConvenientDialogDisplayer.getDefault();

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
        setUpErrorMsgLocaleSelection();
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
    public void setAvailableCourses(List<Course> courses) {
        setCourseListRefreshInProgress(true); // To avoid changes triggering a new reload

        String previousSelectedCourseName = getSelectedCourseName();

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
    public List<Course> getAvailableCourses() {
        List<Course> result = new ArrayList<Course>(coursesComboBox.getItemCount());
        for (int i = 0; i < coursesComboBox.getItemCount(); ++i) {
            result.add((Course)coursesComboBox.getItemAt(i));
        }
        return result;
    }

    @Override
    public void setSelectedCourseName(String courseName) {
        for (int i = 0; i < coursesComboBox.getItemCount(); ++i) {
            if (((Course)coursesComboBox.getItemAt(i)).getName().equals(courseName)) {
                coursesComboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    @Override
    public String getSelectedCourseName() {
        Object item = coursesComboBox.getSelectedItem();
        if (item instanceof Course) {
            return ((Course)item).getName();
        } else { // because the combobox isn't populated yet
            return null;
        }
    }

    @Override
    public boolean getCheckForUpdatesInTheBackground() {
        return checkForUpdatesInBackgroundCheckbox.isSelected();
    }

    @Override
    public void setCheckForUpdatesInTheBackground(boolean shouldCheck) {
        checkForUpdatesInBackgroundCheckbox.setSelected(shouldCheck);
    }

    @Override
    public boolean getCheckForUnopenedExercisesAtStartup() {
        return checkForUnopenedExercisesCheckbox.isSelected();
    }

    @Override
    public void setCheckForUnopenedExercisesAtStartup(boolean shouldCheck) {
        checkForUnopenedExercisesCheckbox.setSelected(shouldCheck);
    }

    @Override
    public boolean getSpywareEnabled() {
        return spywareEnabledCheckbox.isSelected();
    }

    @Override
    public void setSpywareEnabled(boolean enabled) {
        spywareEnabledCheckbox.setSelected(enabled);
    }

    @Override
    public Locale getErrorMsgLocale() {
        Object item = errorMsgLocaleComboBox.getSelectedItem();
        if (item != null) {
            return ((LocaleWrapper)item).getLocale();
        } else {
            return new Locale("en_US");
        }
    }

    @Override
    public void setErrorMsgLocale(Locale locale) {
        errorMsgLocaleComboBox.setSelectedItem(new LocaleWrapper(locale));
    }

    private static class LocaleWrapper {
        private Locale locale;
        public LocaleWrapper(Locale locale) {
            this.locale = locale;
        }

        public Locale getLocale() {
            return locale;
        }

        @Override
        public String toString() {
            return locale.getDisplayLanguage();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof LocaleWrapper)) {
                return false;
            }
            return this.locale.equals(((LocaleWrapper)obj).locale);
        }

        @Override
        public int hashCode() {
            return locale.hashCode();
        }
    }

    private NBTmcSettings getTransientSettingsForRefresh() {
        NBTmcSettings settings = NBTmcSettings.getTransient();
        settings.setUsername(getUsername());
        settings.setPassword(getPassword());
        settings.setServerBaseUrl(getServerBaseUrl());
        settings.setProjectRootDir(getProjectDir());
        return settings;
    }

    private RefreshSettings getRefreshSettings() {
        return new RefreshSettings(getUsername(), getPassword(), getServerBaseUrl());
    }


    private void setUpErrorMsgLocaleSelection() {

        restartMessage.setText("");

        for (Locale locale : SelectedTailoring.get().getAvailableErrorMsgLocales()) {
            errorMsgLocaleComboBox.addItem(new LocaleWrapper(locale));
        }

        errorMsgLocaleComboBox.setSelectedItem(SelectedTailoring.get().getDefaultErrorMsgLocale());
        errorMsgLocaleComboBox.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent event) {

                if (event.getStateChange() == ItemEvent.SELECTED) {

                    // Language changed, notify user about restarting
                    if (!NBTmcSettings.getDefault().getErrorMsgLocale().equals(getErrorMsgLocale())) {
                        restartMessage.setText("Changing language requires restart");
                    } else {
                        restartMessage.setText("");
                    }
                }
            }
        });
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
            startRefreshingCourseList(true, true);
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
        return (lastRefreshSettings == null || !lastRefreshSettings.equals(getRefreshSettings()));
    }

    private void startRefreshingCourseList(boolean failSilently, boolean delay) {
        final RefreshCoursesAction action = new RefreshCoursesAction(getTransientSettingsForRefresh());
        action.addDefaultListener(!failSilently, false);
        action.addListener(new FutureCallback<List<Course>>() {

            @Override
            public void onSuccess(List<Course> result) {
                setCourseListRefreshInProgress(false);
                setAvailableCourses(result);            
            }
            
            @Override
            public void onFailure(Throwable thrwbl) {
                setCourseListRefreshInProgress(false);
            }
            
        });
        
        if (delay) {
            refreshRunner.setTask(new Runnable() {
                @Override
                public void run() {
                    refreshCourseListNow(action);
                }
            });
        } else {
            refreshCourseListNow(action);
        }
    }

    private void refreshCourseListNow(RefreshCoursesAction action) {
        setCourseListRefreshInProgress(true);
        lastRefreshSettings = getRefreshSettings();
        action.run();
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
        passwordLabel = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();
        savePasswordCheckBox = new javax.swing.JCheckBox();
        courseListReloadingLabel = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        checkForUpdatesInBackgroundCheckbox = new javax.swing.JCheckBox();
        checkForUnopenedExercisesCheckbox = new javax.swing.JCheckBox();
        spywareEnabledCheckbox = new javax.swing.JCheckBox();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        errorMsgLocaleLabel = new javax.swing.JLabel();
        errorMsgLocaleComboBox = new javax.swing.JComboBox();
        restartMessage = new javax.swing.JLabel();

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

        projectFolderLabel.setLabelFor(projectFolderTextField);
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

        checkForUpdatesInBackgroundCheckbox.setSelected(true);
        checkForUpdatesInBackgroundCheckbox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUpdatesInBackgroundCheckbox.text")); // NOI18N

        checkForUnopenedExercisesCheckbox.setSelected(true);
        checkForUnopenedExercisesCheckbox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUnopenedExercisesCheckbox.text")); // NOI18N
        checkForUnopenedExercisesCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.checkForUnopenedExercisesCheckbox.toolTipText")); // NOI18N

        spywareEnabledCheckbox.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.spywareEnabledCheckbox.text")); // NOI18N
        spywareEnabledCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.spywareEnabledCheckbox.toolTipText")); // NOI18N

        errorMsgLocaleLabel.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleLabel.text")); // NOI18N
        errorMsgLocaleLabel.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleLabel.toolTipText")); // NOI18N

        errorMsgLocaleComboBox.setToolTipText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.errorMsgLocaleComboBox.toolTipText")); // NOI18N

        restartMessage.setFont(new java.awt.Font("Ubuntu", 1, 14)); // NOI18N
        restartMessage.setText(org.openide.util.NbBundle.getMessage(PreferencesPanel.class, "PreferencesPanel.restartMessage.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(adviceLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(usernameLabel)
                            .addComponent(serverAddressLabel)
                            .addComponent(coursesLabel)
                            .addComponent(passwordLabel))
                        .addGap(78, 78, 78)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(coursesComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(refreshCoursesBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(courseListReloadingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(usernameTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(passwordField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(savePasswordCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 133, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(serverAddressTextField, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(projectFolderLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(projectFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(folderChooserBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jSeparator3)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(errorMsgLocaleLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(errorMsgLocaleComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(spywareEnabledCheckbox)
                            .addComponent(checkForUnopenedExercisesCheckbox)
                            .addComponent(checkForUpdatesInBackgroundCheckbox))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(restartMessage)
                .addGap(22, 22, 22))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(adviceLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
                .addGap(18, 18, 18)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(folderChooserBtn)
                    .addComponent(projectFolderTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(projectFolderLabel))
                .addGap(18, 18, 18)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(checkForUpdatesInBackgroundCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(checkForUnopenedExercisesCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(spywareEnabledCheckbox)
                .addGap(18, 18, 18)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(errorMsgLocaleLabel)
                    .addComponent(errorMsgLocaleComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(restartMessage)
                .addContainerGap(20, Short.MAX_VALUE))
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
        NBTmcSettings settings = getTransientSettingsForRefresh();
        if (settings.getServerAddress() == null || settings.getServerAddress().trim().isEmpty()) {
            dialogs.displayError("Please set the server address first");
        }
        startRefreshingCourseList(false, false);
    }//GEN-LAST:event_refreshCoursesBtnActionPerformed

    private void serverAddressTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_serverAddressTextFieldActionPerformed
        // Pressing return in the server address field presses the refresh button
        refreshCoursesBtn.doClick();
    }//GEN-LAST:event_serverAddressTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel adviceLabel;
    private javax.swing.JCheckBox checkForUnopenedExercisesCheckbox;
    private javax.swing.JCheckBox checkForUpdatesInBackgroundCheckbox;
    private javax.swing.JLabel courseListReloadingLabel;
    private javax.swing.JComboBox coursesComboBox;
    private javax.swing.JLabel coursesLabel;
    private javax.swing.JComboBox errorMsgLocaleComboBox;
    private javax.swing.JLabel errorMsgLocaleLabel;
    private javax.swing.JButton folderChooserBtn;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel projectFolderLabel;
    private javax.swing.JTextField projectFolderTextField;
    private javax.swing.JButton refreshCoursesBtn;
    private javax.swing.JLabel restartMessage;
    private javax.swing.JCheckBox savePasswordCheckBox;
    private javax.swing.JLabel serverAddressLabel;
    private javax.swing.JTextField serverAddressTextField;
    private javax.swing.JCheckBox spywareEnabledCheckbox;
    private javax.swing.JLabel usernameLabel;
    private javax.swing.JTextField usernameTextField;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

}
