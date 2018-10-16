package fi.helsinki.cs.tmc.ui;

import fi.helsinki.cs.tmc.core.TmcCore;
import fi.helsinki.cs.tmc.core.domain.Organization;
import fi.helsinki.cs.tmc.core.domain.ProgressObserver;
import fi.helsinki.cs.tmc.core.holders.TmcSettingsHolder;

import com.google.common.base.Optional;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import org.openide.util.Exceptions;

public class OrganizationListWindow extends JPanel {

    private static JFrame frame;
    private final JLabel title;
    private final JList<Organization> organizations;
    private static JButton button;

    public OrganizationListWindow(List<Organization> organizations) {
        this.title = new JLabel("Select an organization:");
        Font titleFont = this.title.getFont();
        this.title.setFont(new Font(titleFont.getName(), Font.BOLD, 20));
        this.title.setBorder(new MatteBorder(new Insets(10, 10, 5, 10), getBackground()));
        Collections.sort(organizations, (a, b) -> {
            if (a.isPinned() && b.isPinned()) {
                return a.getName().compareTo(b.getName());
            }
            if (a.isPinned()) {
                return -1;
            }
            if (b.isPinned()) {
                return 1;
            }
            return a.getName().compareTo(b.getName());
        });
        Organization[] orgArray = organizations.toArray(new Organization[organizations.size()]);
        this.organizations = new JList<>(orgArray);
        this.organizations.setFixedCellHeight(107);
        this.organizations.setFixedCellWidth(346);
        this.organizations.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        this.button = new JButton("Select");
        button.addActionListener(new SelectOrganizationListener(this));

        this.organizations.setCellRenderer(new OrganizationCellRenderer(this.organizations));
        this.organizations.setVisibleRowCount(4);
        JScrollPane pane = new JScrollPane(this.organizations);
        Dimension d = pane.getPreferredSize();
        d.width = 800;
        d.height = (int) (d.height * 1.12);
        pane.setPreferredSize(d);
        pane.setBorder(new EmptyBorder(5, 0, 5, 0));
        pane.setViewportBorder(new EmptyBorder(0, 0, 0, 0));
        pane.getVerticalScrollBar().setUnitIncrement(10);
        this.organizations.setBackground(new Color(242, 241, 240));

        this.organizations.setSelectedIndex(setDefaultSelectedIndex());
        
        this.organizations.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() >= 2) {
                    button.doClick();
                }
            }
        });

        add(title);
        add(pane);
        add(button);
    }

    public static void display() throws Exception {
        if (frame == null) {
            frame = new JFrame("Organizations");
        }
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        List<Organization> organizations = TmcCore.get().getOrganizations(ProgressObserver.NULL_OBSERVER).call();
        final OrganizationListWindow organizationListWindow = new OrganizationListWindow(organizations);
        frame.setContentPane(organizationListWindow);

        button.setMinimumSize(new Dimension(organizationListWindow.getWidth(), button.getHeight()));
        button.setMaximumSize(new Dimension(organizationListWindow.getWidth(), button.getHeight()));
        organizationListWindow.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent event) {
                button.setMinimumSize(new Dimension(organizationListWindow.getWidth(), button.getHeight()));
                button.setMaximumSize(new Dimension(organizationListWindow.getWidth(), button.getHeight()));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    public static boolean isWindowVisible() {
        if (frame == null) {
            return false;
        }
        return frame.isVisible();
    }

    private int setDefaultSelectedIndex() {
        Optional<Organization> selectedOrganization = TmcSettingsHolder.get().getOrganization();
        if (!selectedOrganization.isPresent()) {
            return 0;
        }
        final ListModel<Organization> list = organizations.getModel();
        for (int i = 0; i < list.getSize(); i++) {
            if (list.getElementAt(i).getName().equals(selectedOrganization.get().getName())) {
                return i;
            }
        }
        return 0;
    }

    class SelectOrganizationListener implements ActionListener {

        public SelectOrganizationListener(OrganizationListWindow window) {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Organization organization = organizations.getSelectedValue();
            frame.setVisible(false);
            frame.dispose();
            try {
                PreferencesPanel panel;
                if (PreferencesUIFactory.getInstance().getCurrentUI() == null) {
                    panel = (PreferencesPanel) PreferencesUIFactory.getInstance().createCurrentPreferencesUI();
                } else {
                    panel = (PreferencesPanel) PreferencesUIFactory.getInstance().getCurrentUI();
                }
                panel.setOrganization(organization);
                CourseListWindow.display();
            } catch (Exception ex) {
                if (ex instanceof IOException) {
                    ConvenientDialogDisplayer.getDefault().displayError("Couldn't connect to the server! Please check your internet connection.");
                } else {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
}

class OrganizationCellRenderer extends DefaultListCellRenderer {

    private static final Color HIGHLIGHT_COLOR = new Color(240, 119, 70);
    private final JList parent;
    private final Map<Organization, OrganizationCard> cachedOrgs;

    public OrganizationCellRenderer(JList parent) {
        this.parent = parent;
        this.cachedOrgs = new HashMap<>();
    }

    @Override
    public Component getListCellRendererComponent(final JList list,
            final Object value, final int index, final boolean isSelected,
            final boolean hasFocus) {
        final Organization org = (Organization)value;
        if (!this.cachedOrgs.containsKey(org)) {
            OrganizationCard organization = new OrganizationCard(org, parent);
            this.cachedOrgs.put(org, organization);
        }
        OrganizationCard organizationCard = this.cachedOrgs.get(org);
        if (isSelected) {
            organizationCard.setColors(Color.white, HIGHLIGHT_COLOR);
        } else {
            organizationCard.setColors(new Color(76, 76, 76), Color.white);
        }
        return organizationCard;
    }
}
