package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;
import fi.helsinki.cs.tmc.coreimpl.TmcCoreSettingsImpl;

import com.google.common.base.Optional;

import org.openide.windows.WindowManager;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class LoginDialog extends javax.swing.JDialog {

    public static void display(ActionListener onOk, final Runnable onClosed) {
        LoginDialog dialog = new LoginDialog(onOk);
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onClosed.run();
            }
        });
    }

    private TmcCoreSettingsImpl settings;
    private ActionListener onLogin;
    private static boolean visible;

    /**
     * Creates new form LoginForm
     */
    public LoginDialog(ActionListener onLogin) {
        super(WindowManager.getDefault().getMainWindow(), false);
        initComponents();

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.settings = (TmcCoreSettingsImpl) TmcSettingsHolder.get();
        final Optional<String> username = settings.getUsername();
        if (username.isPresent()) {
            this.usernameField.setText(username.get());
        }

        if (!usernameField.getText().isEmpty()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    passwordField.requestFocusInWindow();
                }
            });
        }

        final String serverAddress = TmcSettingsHolder.get().getServerAddress();
        if (!serverAddress.isEmpty()) {
            this.addressLabel.setText(serverAddress);
        }

        this.onLogin = onLogin;
        this.visible = true;

        /* Add a windowlistener to the dialog to track when the dialog is closed
        * from the x-button
        */
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                visible = false;
                super.windowClosing(e);
            }
        });
    }

    public static boolean isWindowVisible() {
        return visible;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        cancelButton = new javax.swing.JButton();
        loginButton = new javax.swing.JButton();
        titleLabel = new javax.swing.JLabel();
        usernameLabel = new javax.swing.JLabel();
        passwordLabel = new javax.swing.JLabel();
        usernameField = new javax.swing.JTextField();
        passwordField = new javax.swing.JPasswordField();
        serverLabel = new javax.swing.JLabel();
        addressLabel = new javax.swing.JLabel();
        changeServerButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.title")); // NOI18N
        setBackground(new java.awt.Color(255, 255, 255));
        setResizable(false);

        cancelButton.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.cancelButton.text")); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        loginButton.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.loginButton.text")); // NOI18N
        loginButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginButtonActionPerformed(evt);
            }
        });

        titleLabel.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        titleLabel.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.titleLabel.text")); // NOI18N

        usernameLabel.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.usernameLabel.text")); // NOI18N

        passwordLabel.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.passwordLabel.text")); // NOI18N

        usernameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usernameFieldActionPerformed(evt);
            }
        });

        passwordField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passwordFieldActionPerformed(evt);
            }
        });

        serverLabel.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.serverLabel.text")); // NOI18N

        addressLabel.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.addressLabel.text")); // NOI18N

        changeServerButton.setText(org.openide.util.NbBundle.getMessage(LoginDialog.class, "LoginDialog.changeServerButton.text")); // NOI18N
        changeServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                changeServerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(titleLabel))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(serverLabel)
                .addGap(38, 38, 38)
                .addComponent(addressLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(changeServerButton))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(usernameLabel)
                .addGap(12, 12, 12)
                .addComponent(usernameField, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(passwordLabel)
                .addGap(15, 15, 15)
                .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(268, 268, 268)
                .addComponent(loginButton)
                .addGap(6, 6, 6)
                .addComponent(cancelButton))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(titleLabel)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(changeServerButton)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(serverLabel)
                            .addComponent(addressLabel))))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(usernameLabel))
                    .addComponent(usernameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(passwordLabel))
                    .addComponent(passwordField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(loginButton)
                    .addComponent(cancelButton)))
        );

        setSize(new java.awt.Dimension(404, 248));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
        this.dispose();
        visible = false;
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void loginButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginButtonActionPerformed
        settings.setUsername(usernameField.getText());
        settings.setPassword(Optional.of(new String(passwordField.getPassword())));
        settings.save();

        onLogin.actionPerformed(evt);

        this.setVisible(false);
        this.dispose();
        visible = false;
    }//GEN-LAST:event_loginButtonActionPerformed

    private void usernameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usernameFieldActionPerformed
        passwordField.requestFocusInWindow();
    }//GEN-LAST:event_usernameFieldActionPerformed

    private void passwordFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passwordFieldActionPerformed
        loginButton.doClick();
    }//GEN-LAST:event_passwordFieldActionPerformed

    private void changeServerButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_changeServerButtonActionPerformed
        String newAddress = JOptionPane.showInputDialog(this, "Server address", this.addressLabel.getText());
        if (newAddress != null && !newAddress.trim().isEmpty()) {
            this.addressLabel.setText(newAddress.trim());
            TmcSettingsHolder.get().setServerAddress(newAddress);
        }
    }//GEN-LAST:event_changeServerButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel addressLabel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton changeServerButton;
    private javax.swing.JButton loginButton;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JLabel serverLabel;
    private javax.swing.JLabel titleLabel;
    private javax.swing.JTextField usernameField;
    private javax.swing.JLabel usernameLabel;
    // End of variables declaration//GEN-END:variables
}
