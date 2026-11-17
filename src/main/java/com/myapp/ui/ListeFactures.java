package com.myapp.ui;

import com.myapp.db.ConnexionSQLite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import com.myapp.db.DatabaseManager;

public class ListeFactures extends JFrame {

    private JTable tableFactures;
    private DefaultTableModel model;
    
    // Composants de filtrage
    private JComboBox<String> comboMoyenPaiement;
    private JComboBox<String> comboTypeDocument;  // AJOUTÉ
    private JTextField txtRechercheNom;
    private JTextField dateDebut;
    private JTextField dateFin;
    private JButton btnRechercher;
    private JButton btnResetFiltres;
    
    // Boutons d'action bas de page
    private JButton btnVoirDetails;
    private JButton btnRafraichir;
    
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    /**
     * ★ CORRECTIF : à la création (BLUI/FactureUI), les prix HT sont arrondis
     * à 3 décimales via "formatMontantDinar" (#,##0.000). Mais les 3 méthodes
     * de réimpression ci-dessous utilisaient "df" (#,##0.00 — seulement 2
     * décimales) pour reformater ces mêmes valeurs avant de les transmettre
     * au service Jasper, tronquant leur précision (ex: 9,813 devient 9,81).
     * Répétée sur des dizaines de lignes, cette perte de précision
     * s'accumule et fausse le TOTAL TTC affiché à la réimpression par
     * rapport au document imprimé à l'origine (ex: 767,980 DT au lieu de
     * 767,985 DT). Ce formateur à 3 décimales remplace "df" uniquement pour
     * les colonnes PU HT / Total HT / PU TTC / Total TTC des tableaux
     * d'articles reconstruits, en cohérence avec la précision utilisée à la
     * création du document.
     */
    private String formatMontantDinar3(double montant) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(java.util.Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        DecimalFormat df3 = new DecimalFormat("#,##0.000", symbols);
        return df3.format(montant).replace(",", ".");
    }
    private final Map<Integer, Integer> rowToFactureIdMap = new HashMap<>();
    private Font fontAwesomeSolid;
    
    // FLAG POUR ÉVITER LES CHARGEMENTS MULTIPLES
    private boolean isLoading = false;

    public ListeFactures() {
        this.loadFontAwesome();
        this.setTitle("Liste des Documents - Chaabeni Electricité");
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setLayout(new BorderLayout(10, 10));
        this.initUI();
        this.chargerFactures();
    }

    private void loadFontAwesome() {
        try {
            String path = "/fonts/fa.ttf"; 
            InputStream fontStream = this.getClass().getResourceAsStream(path);
            if (fontStream != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(font);
                this.fontAwesomeSolid = font;
                fontStream.close();
            } else {
                this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            e.printStackTrace();
            this.fontAwesomeSolid = new Font("SansSerif", Font.PLAIN, 12);
        }
    }
   
    private String getHtmlText(String iconCode, String text) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        if (text == null || text.isEmpty()) {
            return "<html><font face=\"" + fontName + "\">" + iconCode + "</font></html>";
        }
        return "<html><font face=\"" + fontName + "\">" + iconCode + "</font> " + text + "</html>";
    }

    // --- NAVIGATION BAR ---
    private JButton createNavButtonWithMenu(String icon, String text, final boolean isActive, final String[] menuItems, final String[] menuIcons) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String chevron = "\uf078"; 
        
        String htmlContent = "<html><font face=\"" + fontName + "\">" + icon + "</font> " + 
                             text + 
                             " <font face=\"" + fontName + "\" size=\"3\">" + chevron + "</font></html>";
        
        final JButton button = new JButton(htmlContent);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));

        if (isActive) {
            button.setBackground(new Color(41, 128, 185));
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(new Color(236, 240, 241));
            button.setForeground(new Color(52, 73, 94));
        }

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        
        final Timer hoverTimer = new Timer(300, (e) -> {
            this.showMenuForButton(button, menuItems, menuIcons);
        });
        hoverTimer.setRepeats(false);
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                if (!isActive) button.setBackground(new Color(220, 220, 220));
                hoverTimer.start();
            }
            public void mouseExited(MouseEvent evt) {
                if (!isActive) button.setBackground(new Color(236, 240, 241));
                hoverTimer.stop();
            }
            public void mouseClicked(MouseEvent evt) {
                ListeFactures.this.showMenuForButton(button, menuItems, menuIcons);
            }
        });
        return button;
    }

    private void showMenuForButton(JButton button, String[] menuItems, String[] menuIcons) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        for(int i = 0; i < menuItems.length; ++i) {
            String htmlItem = this.getHtmlText(menuIcons[i], menuItems[i]);
            JMenuItem menuItem = new JMenuItem(htmlItem);
            menuItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            String item = menuItems[i];
            menuItem.addActionListener((e) -> {
                this.handleMenuAction(item);
            });
            menu.add(menuItem);
            if (i < menuItems.length - 1) {
                menu.addSeparator();
            }
        }
        menu.show(button, 0, button.getHeight());
    }

    private void handleMenuAction(String menuItem) {
        switch(menuItem) {
            case "Nouveau Client": ouvrirNouveauClient(); break;
            case "Liste Clients": ouvrirListeClients(); break;
            case "Liste Factures": break; 
            case "Gestion Entrée": ouvrirGestionEntrees(); break;
            case "Gestion Sortie": ouvrirGestionSorties(); break;
            case "Devis": ouvrirDevis(); break;
            case "Bon de Livraison": ouvrirBonLivraison(); break;
            case "Facture": ouvrirFacture(); break;
            case "Bon de Sortie": ouvrirBonSortie(); break;
            case "Consulter Stock": ouvrirGestionStock(); break;
            case "Historique Entrée": ouvrirHistoriqueEntrees(); break;
            case "Historique Sortie": ouvrirHistoriqueSorties(); break;
        }
    }

    // Méthodes de navigation
    private void ouvrirNouveauClient() { try { (new AjouterClient(null)).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirListeClients() { try { (new ListeClients()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirFacture() { try { (new FactureUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirBonLivraison() { try { (new BLUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirBonSortie() { try { (new BonSortieUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirDevis() { try { (new DevisUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirGestionStock() { try { (new GestionStock()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirGestionEntrees() { try { (new GestionEntreesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirGestionSorties() { try { (new GestionSortiesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirHistoriqueEntrees() { try { (new HistoriqueEntreesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }
    private void ouvrirHistoriqueSorties() { try { (new HistoriqueSortiesUI()).setVisible(true); this.dispose(); } catch (Exception e) {} }

    private JPanel createNavigationBar() {
        JPanel navPanel = new JPanel(new GridLayout(1, 4));
        navPanel.setBackground(new Color(52, 73, 94));
        
        JButton btnBack = new JButton(this.getHtmlText("\uf060", "Tableau de Bord"));
        btnBack.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBack.setBackground(new Color(44, 62, 80));
        btnBack.setForeground(Color.WHITE);
        btnBack.setFocusPainted(false);
        btnBack.setBorderPainted(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.addActionListener(e -> {
            this.dispose();
            try { new AdminDashboard().setVisible(true); } catch(Exception ex) {}
        });
        navPanel.add(btnBack);

        String[] clientsMenuItems = new String[]{"Liste Clients", "Nouveau Client"};
        String[] clientsMenuIcons = new String[]{"\uf0c0", "\uf067"};
        JButton btnClients = this.createNavButtonWithMenu("\uf0c0", "Clients", false, clientsMenuItems, clientsMenuIcons);
        navPanel.add(btnClients);

        String[] documentsMenuItems = new String[]{"Facture", "Liste Factures", "Bon de Livraison", "Bon de Sortie", "Devis"};
        String[] documentsMenuIcons = new String[]{"\uf15b", "\uf15b", "\uf15b", "\uf15b", "\uf15b"};
        JButton btnDocuments = this.createNavButtonWithMenu("\uf15b", "Documents", true, documentsMenuItems, documentsMenuIcons);
        navPanel.add(btnDocuments);

        String[] stockMenuItems = new String[]{"Consulter Stock", "Gestion Entrée", "Gestion Sortie", "Historique Entrée", "Historique Sortie"};
        String[] stockMenuIcons = new String[]{"\uf494", "\uf090", "\uf08b", "\uf1da", "\uf201"};
        JButton btnStock = this.createNavButtonWithMenu("\uf494", "Stock", false, stockMenuItems, stockMenuIcons);
        navPanel.add(btnStock);
        
        return navPanel;
    }

    // --- INITIALISATION UI ---
    private void initUI() {
        JPanel navBarPanel = this.createNavigationBar();
        
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(44, 62, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel(this.getHtmlText("\uf15b", "LISTE DES DOCUMENTS"), JLabel.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JPanel filterPanel = this.createFilterPanel();
        
        // NOUVEAU MODÈLE AVEC COLONNE TYPE
        this.model = new DefaultTableModel(new String[]{
            "N° Document", "Type", "Date", "Client", "Montant HT", "Remise", "TVA", "Total TTC", "Net à Payer", "Mode", "Actions"
        }, 0) {
            public boolean isCellEditable(int row, int column) {
                return column == 10;
            }
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 10 ? JPanel.class : Object.class;
            }
        };
        
        this.tableFactures = new JTable(this.model);
        this.tableFactures.setRowHeight(45);
        this.tableFactures.setSelectionBackground(new Color(41, 128, 185));
        this.tableFactures.setSelectionForeground(Color.WHITE);
        this.tableFactures.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        this.tableFactures.setGridColor(new Color(240, 240, 240));
        
        JTableHeader header = this.tableFactures.getTableHeader();
        TableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
             public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                this.setBackground(new Color(52, 73, 94));
                this.setForeground(Color.WHITE);
                this.setFont(new Font("Segoe UI", Font.BOLD, 13));
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return this;
             }
        };
        header.setDefaultRenderer(headerRenderer);
        
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                   c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                }
                
                this.setHorizontalAlignment(JLabel.CENTER);
                
                if (column >= 4 && column <= 8) {
                    if (column == 5 && value != null && !value.toString().startsWith("0,00")) {
                        this.setForeground(new Color(231, 76, 60));
                    } else if (column == 7 || column == 8) { 
                        this.setForeground(new Color(46, 204, 113));
                    } else {
                        this.setForeground(new Color(52, 73, 94));
                    }
                    this.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    this.setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    this.setForeground(Color.BLACK);
                }
                return c;
            }
        };
        
        // Appliquer les renderers aux colonnes (sauf colonne Type)
        for (int i = 0; i < 11; i++) {
            if (i != 1) {
                this.tableFactures.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }
        
        // Renderer coloré pour la colonne Type (colonne 1)
        this.tableFactures.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if (!isSelected) {
                    if ("BL".equals(value)) {
                        setForeground(new Color(230, 126, 34));
                        setBackground(new Color(255, 245, 235));
                    } else if ("FACTURE".equals(value)) {
                        setForeground(new Color(52, 152, 219));
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                    } else {
                        setForeground(Color.BLACK);
                        setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                    }
                }
                return this;
            }
        });
        
        this.tableFactures.getColumnModel().getColumn(10).setCellRenderer(new ActionsCellRenderer());
        this.tableFactures.getColumnModel().getColumn(10).setCellEditor(new ActionsCellEditor());
        this.tableFactures.getColumnModel().getColumn(10).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(this.tableFactures);
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)), "Historique des Documents"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(new Color(250, 250, 250));
        
        this.btnVoirDetails = this.createStyledButton("\uf06e", "Voir Détails & Imprimer", new Color(52, 152, 219));
        this.btnRafraichir = this.createStyledButton("\uf021", "Rafraîchir", new Color(155, 89, 182));
        
        buttonPanel.add(this.btnVoirDetails);
        buttonPanel.add(this.btnRafraichir);

        this.add(navBarPanel, BorderLayout.NORTH);
        
        JPanel mainContentPanel = new JPanel(new BorderLayout());
        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(filterPanel, BorderLayout.SOUTH);
        
        mainContentPanel.add(topContainer, BorderLayout.NORTH);
        mainContentPanel.add(scrollPane, BorderLayout.CENTER);
        mainContentPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        this.add(mainContentPanel, BorderLayout.CENTER);
        
        this.btnRechercher.addActionListener(e -> this.rechercherFactures());
        this.btnResetFiltres.addActionListener(e -> this.resetFiltres());
        this.btnRafraichir.addActionListener(e -> this.chargerFactures());
        this.btnVoirDetails.addActionListener(e -> this.voirDetailsFacture());
        
        this.txtRechercheNom.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                rechercherFactures();
            }
        });
        
        this.comboMoyenPaiement.addActionListener(e -> {
            if (!isLoading) {
                rechercherFactures();
            }
        });
        
        // AJOUTÉ : listener pour comboTypeDocument
        this.comboTypeDocument.addActionListener(e -> {
            if (!isLoading) {
                rechercherFactures();
            }
        });
        
        this.tableFactures.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    voirDetailsFacture();
                }
            }
        });
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        filterPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);

        // Nom client
        JLabel lblRechercheNom = new JLabel(this.getHtmlText("\uf007", "Nom Client:"));
        lblRechercheNom.setFont(labelFont);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(lblRechercheNom, gbc);

        this.txtRechercheNom = new JTextField();
        this.txtRechercheNom.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        this.txtRechercheNom.setToolTipText("Recherche dynamique par nom du client");
        this.txtRechercheNom.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3;
        filterPanel.add(this.txtRechercheNom, gbc);

        // Type de document (NOUVEAU)
        JLabel lblType = new JLabel(this.getHtmlText("\uf15b", "Type:"));
        lblType.setFont(labelFont);
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(lblType, gbc);

        this.comboTypeDocument = new JComboBox<>(new String[]{"Tous", "FACTURE", "BL", "TICKET"});
        this.comboTypeDocument.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.2;
        filterPanel.add(this.comboTypeDocument, gbc);

        // Mode paiement
        JLabel lblPaiement = new JLabel(this.getHtmlText("\uf0d6", "Paiement:"));
        lblPaiement.setFont(labelFont);
        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(lblPaiement, gbc);

        this.comboMoyenPaiement = new JComboBox<>(new String[]{"Tous", "Espèces", "Chèque", "Virement", "Echeance"});
        this.comboMoyenPaiement.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 5; gbc.gridy = 0; gbc.weightx = 0.2;
        filterPanel.add(this.comboMoyenPaiement, gbc);

        // Date début
        JLabel lblDateDebut = new JLabel(this.getHtmlText("\uf073", "Du:"));
        lblDateDebut.setFont(labelFont);
        gbc.gridx = 6; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(lblDateDebut, gbc);

        this.dateDebut = this.createDateField();
        gbc.gridx = 7; gbc.gridy = 0; gbc.weightx = 0.2;
        filterPanel.add(this.dateDebut, gbc);

        // Date fin
        JLabel lblDateFin = new JLabel(this.getHtmlText("\uf073", "Au:"));
        lblDateFin.setFont(labelFont);
        gbc.gridx = 8; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(lblDateFin, gbc);

        this.dateFin = this.createDateField();
        gbc.gridx = 9; gbc.gridy = 0; gbc.weightx = 0.2;
        filterPanel.add(this.dateFin, gbc);

        // Boutons
        this.btnRechercher = this.createStyledButton("\uf002", "Rechercher", new Color(46, 204, 113));
        this.btnRechercher.setPreferredSize(new Dimension(120, 35));
        gbc.gridx = 10; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(this.btnRechercher, gbc);

        this.btnResetFiltres = this.createStyledButton("\uf0e2", "Réinitialiser", new Color(231, 76, 60));
        this.btnResetFiltres.setPreferredSize(new Dimension(120, 35));
        gbc.gridx = 11; gbc.gridy = 0; gbc.weightx = 0.0;
        filterPanel.add(this.btnResetFiltres, gbc);

        return filterPanel;
    }

    private void resetFiltres() {
        this.txtRechercheNom.setText("");
        this.comboTypeDocument.setSelectedItem("Tous");   // NOUVEAU
        this.comboMoyenPaiement.setSelectedItem("Tous");
        this.dateDebut.setText("JJ/MM/AAAA");
        this.dateDebut.setForeground(Color.GRAY);
        this.dateFin.setText("JJ/MM/AAAA");
        this.dateFin.setForeground(Color.GRAY);
        this.chargerFactures();
    }

    private JTextField createDateField() {
        final JTextField dateField = new JTextField();
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateField.setToolTipText("Format: JJ/MM/AAAA");
        dateField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), 
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        dateField.setText("JJ/MM/AAAA");
        dateField.setForeground(Color.GRAY);

        dateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (dateField.getText().equals("JJ/MM/AAAA")) {
                    dateField.setText("");
                    dateField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent evt) {
                if (dateField.getText().isEmpty()) {
                    dateField.setText("JJ/MM/AAAA");
                    dateField.setForeground(Color.GRAY);
                }
            }
        });
        return dateField;
    }

    private JButton createStyledButton(String iconCode, String text, final Color backgroundColor) {
        final JButton button = new JButton(this.getHtmlText(iconCode, text));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(ListeFactures.this.darkenColor(backgroundColor, 0.9F));
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(backgroundColor);
            }
        });
        return button;
    }

    private Color darkenColor(Color color, float factor) {
        int r = Math.max((int)((float)color.getRed() * factor), 0);
        int g = Math.max((int)((float)color.getGreen() * factor), 0);
        int b = Math.max((int)((float)color.getBlue() * factor), 0);
        return new Color(r, g, b);
    }

    private boolean isValidDate(String date) {
        if (date.equals("JJ/MM/AAAA") || date.isEmpty()) return false;
        return date.matches("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$");
    }

    private String convertToSQLDate(String date) {
        try {
            if (date.equals("JJ/MM/AAAA") || date.isEmpty()) return null;
            String[] parts = date.split("/");
            if (parts.length == 3) {
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception e) {}
        return null;
    }

    private void rechercherFactures() {
        if (isLoading) return;

        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT f.id, f.numero, f.type, f.date, f.client_nom, f.montant_ht, " +
            "f.montant_remise, f.tva, f.montant_ttc, f.net_a_payer, f.moyen_paiement " +
            "FROM Factures f WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        // Filtre type
        String typeDoc = (String) this.comboTypeDocument.getSelectedItem();
        if ("FACTURE".equals(typeDoc)) {
            sql.append(" AND f.type = 'FACTURE'");
        } else if ("BL".equals(typeDoc)) {
            sql.append(" AND f.type = 'BL'");
        } else if ("TICKET".equals(typeDoc)) {
            sql.append(" AND f.type = 'TICKET'");
        } else {
            // ★ CORRECTIF : les tickets étaient bien enregistrés en base
            // (type='TICKET') mais jamais inclus dans la liste, même en
            // affichage "Tous" — ils étaient donc invisibles ici.
            sql.append(" AND f.type IN ('FACTURE', 'BL', 'TICKET')");
        }

        // Filtre nom
        String nomRecherche = this.txtRechercheNom.getText().trim();
        if (!nomRecherche.isEmpty()) {
            sql.append(" AND f.client_nom LIKE ?");
            params.add("%" + nomRecherche + "%");
        }

        // Filtre paiement
        String paiement = (String) this.comboMoyenPaiement.getSelectedItem();
        if (!"Tous".equals(paiement)) {
            sql.append(" AND f.moyen_paiement = ?");
            params.add(paiement);
        }

        // Filtre date début
        String dateDebutText = this.dateDebut.getText().trim();
        if (isValidDate(dateDebutText)) {
            String sqlDateDebut = convertToSQLDate(dateDebutText);
            if (sqlDateDebut != null) {
                sql.append(" AND f.date >= ?");
                params.add(sqlDateDebut);
            }
        }

        // Filtre date fin
        String dateFinText = this.dateFin.getText().trim();
        if (isValidDate(dateFinText)) {
            String sqlDateFin = convertToSQLDate(dateFinText);
            if (sqlDateFin != null) {
                sql.append(" AND f.date <= ?");
                params.add(sqlDateFin);
            }
        }

        sql.append(" ORDER BY f.date DESC, f.id DESC");
        this.executerRequeteRecherche(sql.toString(), params);
    }

    private void executerRequeteRecherche(String sql, List<Object> params) {
        this.model.setRowCount(0);
        this.rowToFactureIdMap.clear();

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pst.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    int id = rs.getInt("id");
                    double remise = rs.getDouble("montant_remise");
                    double netAPayer = rs.getDouble("net_a_payer");
                    Object[] rowData = new Object[]{
                        rs.getString("numero"),           // N° Document
                        rs.getString("type"),              // Type (FACTURE ou BL)
                        formaterDate(rs.getString("date")),
                        rs.getString("client_nom"),
                        df.format(rs.getDouble("montant_ht")) + " DT",
                        df.format(remise) + " DT",
                        df.format(rs.getDouble("tva")) + " DT",
                        df.format(rs.getDouble("montant_ttc")) + " DT",
                        df.format(netAPayer) + " DT",
                        rs.getString("moyen_paiement"),
                        "Actions"
                    };
                    model.addRow(rowData);
                    rowToFactureIdMap.put(count, id);
                    count++;
                }

                if (count == 0 && !params.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Aucun document trouvé avec les critères sélectionnés.",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de la recherche: " + e.getMessage(),
                "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void chargerFactures() {
        if (isLoading) return;
        isLoading = true;

        this.model.setRowCount(0);
        this.rowToFactureIdMap.clear();
        this.txtRechercheNom.setText("");
        this.comboTypeDocument.setSelectedItem("Tous");
        this.comboMoyenPaiement.setSelectedItem("Tous");
        this.dateDebut.setText("JJ/MM/AAAA");
        this.dateDebut.setForeground(Color.GRAY);
        this.dateFin.setText("JJ/MM/AAAA");
        this.dateFin.setForeground(Color.GRAY);

        String sql = "SELECT DISTINCT f.id, f.numero, f.type, f.date, f.client_nom, f.montant_ht, " +
                     "f.montant_remise, f.tva, f.montant_ttc, f.net_a_payer, f.moyen_paiement " +
                     "FROM Factures f WHERE f.type IN ('FACTURE', 'BL', 'TICKET') " +
                     "ORDER BY f.date DESC, f.id DESC";

        try (Connection conn = ConnexionSQLite.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                double remise   = rs.getDouble("montant_remise");
                double netAPayer = rs.getDouble("net_a_payer");
                Object[] rowData = new Object[]{
                    rs.getString("numero"),
                    rs.getString("type"),                              // colonne Type
                    formaterDate(rs.getString("date")),
                    rs.getString("client_nom"),
                    df.format(rs.getDouble("montant_ht"))  + " DT",
                    df.format(remise)                      + " DT",
                    df.format(rs.getDouble("tva"))         + " DT",
                    df.format(rs.getDouble("montant_ttc")) + " DT",
                    df.format(netAPayer)                   + " DT",
                    rs.getString("moyen_paiement"),
                    "Actions"
                };
                model.addRow(rowData);
                rowToFactureIdMap.put(count, rs.getInt("id"));
                count++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            isLoading = false;
        }
    }

    private String formaterDate(String date) {
        try {
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate localDate = LocalDate.parse(date);
                return localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        } catch (Exception e) {}
        return date;
    }

    private void modifierFacture(int row) {
        try {
            Integer factureId = this.rowToFactureIdMap.get(row);
            if (factureId == null) {
                JOptionPane.showMessageDialog(this, 
                    "Erreur: Impossible de trouver l'ID du document sélectionné.\nVeuillez rafraîchir la liste.", 
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!factureExiste(factureId)) {
                JOptionPane.showMessageDialog(this, 
                    "Le document n'existe plus dans la base de données.\nVeuillez rafraîchir la liste.", 
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                this.chargerFactures();
                return;
            }
            
            // Récupérer le type du document
            String type = this.model.getValueAt(row, 1).toString();
            
            if ("FACTURE".equals(type)) {
                FactureUI factureUI = new FactureUI();
                factureUI.chargerFacturePourModification(factureId);
                factureUI.setVisible(true);
            } else if ("BL".equals(type)) {
                // Pour les BL, on ouvre l'interface de modification BL
                BLUI blUI = new BLUI();
                // blUI.chargerBLPourModification(factureId); // À implémenter si nécessaire
                blUI.setVisible(true);
            } else {
                // ★ CORRECTIF : les tickets (et tout type imprévu) ne
                // faisaient rien ici, mais la fenêtre se fermait quand même
                // juste après (this.dispose() plus bas) — l'utilisateur se
                // retrouvait sans aucune fenêtre ouverte. Un ticket ne se
                // modifie pas (document de caisse déjà remis au client) —
                // message clair au lieu d'une fermeture silencieuse.
                JOptionPane.showMessageDialog(this,
                    "Un ticket de caisse ne peut pas être modifié.",
                    "Action non disponible", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            this.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Erreur lors de la modification: " + e.getMessage(), 
                "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void supprimerDocument(int row) {
        Integer factureId = this.rowToFactureIdMap.get(row);
        if (factureId == null) {
            JOptionPane.showMessageDialog(this, "Erreur: ID du document introuvable", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String numeroDocument = this.model.getValueAt(row, 0).toString();
        String typeDocument = this.model.getValueAt(row, 1).toString();
        
        int confirmation = JOptionPane.showConfirmDialog(this, 
            "Êtes-vous sûr de vouloir supprimer le document " + typeDocument + " N° " + numeroDocument + " ?\nCette action est irréversible et supprimera également tous les articles associés.", 
            "Confirmation de suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirmation == JOptionPane.YES_OPTION) {
            if (supprimerDocumentAvecCascade(factureId)) {
                JOptionPane.showMessageDialog(this, "✅ Document " + numeroDocument + " supprimé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                this.chargerFactures();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Erreur lors de la suppression du document", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * ★ CORRECTIF : la suppression faisait seulement DELETE FROM Factures
     * (lignes effacées par cascade) — le stock sorti par le document n'était
     * jamais rendu et Historique_Sorties gardait la sortie. On restaure
     * désormais le stock (FACTURE, TICKET et BL saisi directement ; le BL
     * compagnon "BL/FACT-…" n'a jamais touché au stock) dans une transaction.
     */
    private boolean supprimerDocumentAvecCascade(int factureId) {
        try (Connection conn = ConnexionSQLite.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String type = null, numero = null;
                try (PreparedStatement pst = conn.prepareStatement("SELECT type, numero FROM Factures WHERE id = ?")) {
                    pst.setInt(1, factureId);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) { conn.rollback(); return false; }
                        type = rs.getString("type");
                        numero = rs.getString("numero");
                    }
                }
                boolean aSortiDuStock = "FACTURE".equals(type) || "TICKET".equals(type)
                        || ("BL".equals(type) && numero != null && !numero.startsWith("BL/FACT-"));
                if (aSortiDuStock) {
                    try (PreparedStatement sel = conn.prepareStatement(
                             "SELECT article_designation, quantite FROM DetailsFacture WHERE facture_id = ?");
                         PreparedStatement upd = conn.prepareStatement(
                             "UPDATE Articles SET stock = stock + ? WHERE designation = ?")) {
                        sel.setInt(1, factureId);
                        try (ResultSet rs = sel.executeQuery()) {
                            while (rs.next()) {
                                upd.setInt(1, rs.getInt("quantite"));
                                upd.setString(2, rs.getString("article_designation"));
                                upd.executeUpdate();
                            }
                        }
                    }
                    if (numero != null) {
                        try (PreparedStatement del = conn.prepareStatement(
                                "DELETE FROM Historique_Sorties WHERE numero_fact = ?")) {
                            del.setString(1, numero);
                            del.executeUpdate();
                        }
                    }
                }
                int rowsAffected;
                try (PreparedStatement pst = conn.prepareStatement("DELETE FROM Factures WHERE id = ?")) {
                    pst.setInt(1, factureId);
                    rowsAffected = pst.executeUpdate();
                }
                conn.commit();
                return rowsAffected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean factureExiste(int factureId) {
        String sql = "SELECT 1 FROM Factures WHERE id = ?";
        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, factureId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void voirDetailsFacture() {
        int selectedRow = this.tableFactures.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un document", "Information", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String numeroDocument = this.model.getValueAt(selectedRow, 0).toString();
            String typeDocument = this.model.getValueAt(selectedRow, 1).toString();
            
            if ("FACTURE".equals(typeDocument)) {
                this.afficherFactureAvecImpression(numeroDocument);
            } else if ("BL".equals(typeDocument)) {
                this.afficherBLAvecImpression(numeroDocument);
            } else if ("TICKET".equals(typeDocument)) {
                this.afficherTicketAvecImpression(numeroDocument);
            }
        }
    }

    private void afficherBLAvecImpression(String numeroBL) {
        String sqlBL =
            "SELECT f.id, f.numero, f.date, f.client_id, f.client_nom, " +
            "f.montant_ht, f.tva, f.montant_ttc, f.remise_pourcentage, f.montant_remise, " +
            "f.moyen_paiement, f.timbre, f.tel_client, " +
            "c.adresse, c.Numero AS tel_client_bdd, c.matricule_fiscale, " +
            "c.prenom " +
            "FROM Factures f LEFT JOIN Clients c ON f.client_id = c.id " +
            "WHERE f.numero = ? AND f.type = 'BL'";

        String sqlArticles =
            "SELECT DISTINCT df.article_designation, df.quantite, df.prix_unitaire, df.tva " +
            "FROM DetailsFacture df WHERE df.facture_id = ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlBL)) {
            
            pst.setString(1, numeroBL);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return;

                String date             = rs.getString("date");
                String clientNom        = rs.getString("client_nom");
                String clientPrenom     = rs.getString("prenom") != null ? rs.getString("prenom") : "";
                String adresseClient    = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                String modePaiement     = rs.getString("moyen_paiement");
                String remise           = String.valueOf(rs.getDouble("remise_pourcentage"));
                double timbre           = rs.getDouble("timbre");
                int    factureId        = rs.getInt("id");
                String matriculeFiscale = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";

                String telFromFacture = rs.getString("tel_client");
                String telFromClient  = rs.getString("tel_client_bdd");
                String telClient = (telFromFacture != null && !telFromFacture.isEmpty())
                                   ? telFromFacture
                                   : (telFromClient != null ? telFromClient : "");

                DefaultTableModel modelArticles = new DefaultTableModel(new String[]{
                    "Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"
                }, 0);

                try (PreparedStatement pstArticles = conn.prepareStatement(sqlArticles)) {
                    pstArticles.setInt(1, factureId);
                    try (ResultSet rsArticles = pstArticles.executeQuery()) {
                        while (rsArticles.next()) {
                            String designation  = rsArticles.getString("article_designation");
                            int    quantite     = rsArticles.getInt("quantite");
                            double prixUnitaire = rsArticles.getDouble("prix_unitaire");
                            int    tva          = rsArticles.getInt("tva");
                            double totalHT      = quantite * prixUnitaire;
                            double totalTTC     = totalHT * (1.0 + tva / 100.0);
                            String reference    = DatabaseManager.getReferenceArticle(designation);
                            
                            modelArticles.addRow(new Object[]{
                                reference, designation, String.valueOf(quantite),
                                formatMontantDinar3(prixUnitaire), formatMontantDinar3(totalHT), tva + "%",
                                formatMontantDinar3(prixUnitaire * (1 + tva / 100.0)), "0%", formatMontantDinar3(totalTTC)
                            });
                        }
                    }
                }

                ImageIcon logoIcon = this.chargerLogo();
                // ★ MIGRATION JASPER : même principe que pour la Facture —
                // appel direct au vrai service, plus besoin de reconstruire
                // un aperçu à la main.
                try {
                    java.io.InputStream logoStreamApercu = null;
                    if (logoIcon != null) {
                        java.awt.image.BufferedImage logoBuffer = new java.awt.image.BufferedImage(
                                logoIcon.getIconWidth(), logoIcon.getIconHeight(),
                                java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D gLogo = logoBuffer.createGraphics();
                        logoIcon.paintIcon(null, gLogo, 0, 0);
                        gLogo.dispose();
                        java.io.ByteArrayOutputStream logoBytes = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(logoBuffer, "png", logoBytes);
                        logoStreamApercu = new java.io.ByteArrayInputStream(logoBytes.toByteArray());
                    }
                    net.sf.jasperreports.engine.JasperPrint jasperPrintApercu =
                        com.myapp.print.jasper.BLReportService.remplirRapport(
                            numeroBL, date, clientNom, clientPrenom,
                            adresseClient, telClient, matriculeFiscale,
                            "", "", "", "",
                            modelArticles, logoStreamApercu, modePaiement
                        );
                    com.myapp.print.jasper.BLReportService.afficherApercu(jasperPrintApercu);
                } catch (Exception exApercu) {
                    ErrorDialogHelper.afficherErreur(this, "Impossible d'afficher l'aperçu de ce Bon de Livraison", exApercu);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void afficherFactureAvecImpression(String numeroFacture) {
        String sqlFacture =
            "SELECT f.id, f.numero, f.date, f.client_id, f.client_nom, " +
            "f.montant_ht, f.tva, f.montant_ttc, f.remise_pourcentage, f.montant_remise, " +
            "f.moyen_paiement, f.timbre, f.tel_client, f.retenue_source, f.net_a_payer, " +
            "c.adresse, c.Numero AS tel_client_bdd, c.matricule_fiscale " +
            "FROM Factures f LEFT JOIN Clients c ON f.client_id = c.id WHERE f.numero = ?";

        String sqlArticles =
            "SELECT DISTINCT df.article_designation, df.quantite, df.prix_unitaire, df.tva " +
            "FROM DetailsFacture df WHERE df.facture_id = ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlFacture)) {
            
            pst.setString(1, numeroFacture);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) return;

                String date             = rs.getString("date");
                String clientNom        = rs.getString("client_nom");
                String adresseClient    = rs.getString("adresse") != null ? rs.getString("adresse") : "";
                String modePaiement     = rs.getString("moyen_paiement");
                String remise           = String.valueOf(rs.getDouble("remise_pourcentage"));
                double timbre           = rs.getDouble("timbre");
                double retenueSource    = rs.getDouble("retenue_source");
                double netAPayer        = rs.getDouble("net_a_payer");
                int    factureId        = rs.getInt("id");
                String matriculeFiscale = rs.getString("matricule_fiscale") != null ? rs.getString("matricule_fiscale") : "";

                String telFromFacture = rs.getString("tel_client");
                String telFromClient  = rs.getString("tel_client_bdd");
                String telClient = (telFromFacture != null && !telFromFacture.isEmpty())
                                   ? telFromFacture
                                   : (telFromClient != null ? telFromClient : "");

                DefaultTableModel modelArticles = new DefaultTableModel(new String[]{
                    "Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"
                }, 0);

                try (PreparedStatement pstArticles = conn.prepareStatement(sqlArticles)) {
                    pstArticles.setInt(1, factureId);
                    try (ResultSet rsArticles = pstArticles.executeQuery()) {
                        while (rsArticles.next()) {
                            String designation  = rsArticles.getString("article_designation");
                            int    quantite     = rsArticles.getInt("quantite");
                            double prixUnitaire = rsArticles.getDouble("prix_unitaire");
                            int    tva          = rsArticles.getInt("tva");
                            double totalHT      = quantite * prixUnitaire;
                            double totalTTC     = totalHT * (1.0 + tva / 100.0);
                            String reference    = DatabaseManager.getReferenceArticle(designation);
                            
                            modelArticles.addRow(new Object[]{
                                reference, designation, String.valueOf(quantite),
                                formatMontantDinar3(prixUnitaire), formatMontantDinar3(totalHT), tva + "%",
                                formatMontantDinar3(prixUnitaire * (1 + tva / 100.0)), "0%", formatMontantDinar3(totalTTC)
                            });
                        }
                    }
                }

                ImageIcon logoIcon = this.chargerLogo();
                double montantHT  = rs.getDouble("montant_ht");
                double montantTVA = rs.getDouble("tva");
                double montantTTC = rs.getDouble("montant_ttc");
                double montantRemise = rs.getDouble("montant_remise");

                // ★ MIGRATION JASPER : remplace l'ancien aperçu "peint à la
                // main" (paintComponent + FactureImpression.print) par un
                // appel direct au vrai service Jasper — JasperViewer offre
                // déjà nativement navigation, zoom et impression, inutile
                // de reconstruire cette mécanique à la main.
                try {
                    java.io.InputStream logoStreamApercu = null;
                    if (logoIcon != null) {
                        java.awt.image.BufferedImage logoBuffer = new java.awt.image.BufferedImage(
                                logoIcon.getIconWidth(), logoIcon.getIconHeight(),
                                java.awt.image.BufferedImage.TYPE_INT_ARGB);
                        java.awt.Graphics2D gLogo = logoBuffer.createGraphics();
                        logoIcon.paintIcon(null, gLogo, 0, 0);
                        gLogo.dispose();
                        java.io.ByteArrayOutputStream logoBytes = new java.io.ByteArrayOutputStream();
                        javax.imageio.ImageIO.write(logoBuffer, "png", logoBytes);
                        logoStreamApercu = new java.io.ByteArrayInputStream(logoBytes.toByteArray());
                    }
                    net.sf.jasperreports.engine.JasperPrint jasperPrintApercu =
                        com.myapp.print.jasper.FactureReportService.remplirRapport(
                            numeroFacture, date, clientNom, "",
                            adresseClient, telClient, matriculeFiscale, "",
                            modelArticles, logoStreamApercu,
                            modePaiement, timbre,
                            montantHT, montantTVA, montantTTC, montantRemise,
                            retenueSource, netAPayer
                        );
                    com.myapp.print.jasper.FactureReportService.afficherApercu(jasperPrintApercu);
                } catch (Exception exApercu) {
                    ErrorDialogHelper.afficherErreur(this, "Impossible d'afficher l'aperçu de cette facture", exApercu);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ★ CORRECTIF : cette méthode ouvrait auparavant une fenêtre Swing
     * "maison" (JDialog + JTable construits à la main) au clic sur un
     * ticket, avec un bouton "Imprimer" séparé qui, lui seul, appelait le
     * vrai service Jasper. Résultat : l'aperçu affiché ne correspondait pas
     * au rendu réel du ticket imprimé (mise en page différente, infos
     * société absentes, etc.) — voir capture d'écran signalée.
     *
     * Aligné maintenant sur le même principe que Facture/BL : appel direct
     * à TicketReportService + JasperViewer, sans écran intermédiaire.
     */
    private void afficherTicketAvecImpression(String numeroTicket) {
        String sqlTicket =
            "SELECT f.id, f.numero, f.date, f.client_nom, " +
            "f.montant_ht, f.tva, f.montant_ttc, f.montant_remise, f.moyen_paiement " +
            "FROM Factures f WHERE f.numero = ? AND f.type = 'TICKET'";

        String sqlArticles =
            "SELECT DISTINCT df.article_designation, df.quantite, df.prix_unitaire, df.tva " +
            "FROM DetailsFacture df WHERE df.facture_id = ?";

        try (Connection conn = ConnexionSQLite.getConnection();
             PreparedStatement pst = conn.prepareStatement(sqlTicket)) {

            pst.setString(1, numeroTicket);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Ticket introuvable en base.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int ticketId          = rs.getInt("id");
                String date           = rs.getString("date");
                String clientNom      = rs.getString("client_nom");
                String modePaiement   = rs.getString("moyen_paiement");
                double totalHTBdd     = rs.getDouble("montant_ht");
                double totalTTCBdd    = rs.getDouble("montant_ttc");
                double montantRemise  = rs.getDouble("montant_remise");

                // ★ Format 9 colonnes attendu par TicketReportService.convertirModel :
                // 0=Code, 1=Désignation, 2=Qté, 3=PU HT, 4=Total HT, 5=TVA,
                // 6=PU TTC, 7=Remise %, 8=Total TTC (identique à Facture/BL).
                DefaultTableModel modelArticles = new DefaultTableModel(new String[]{
                    "Code", "Désignation", "Qté", "PU HT", "Total HT", "TVA", "PU TTC", "Remise %", "Total TTC"
                }, 0);

                try (PreparedStatement pa = conn.prepareStatement(sqlArticles)) {
                    pa.setInt(1, ticketId);
                    try (ResultSet ra = pa.executeQuery()) {
                        while (ra.next()) {
                            String designation  = ra.getString("article_designation");
                            int    quantite     = ra.getInt("quantite");
                            double prixUnitaire  = ra.getDouble("prix_unitaire");
                            int    tva           = ra.getInt("tva");
                            double totalHT       = quantite * prixUnitaire;
                            double puTTC         = prixUnitaire * (1.0 + tva / 100.0);
                            double totalTTC      = totalHT * (1.0 + tva / 100.0);
                            String reference     = DatabaseManager.getReferenceArticle(designation);

                            modelArticles.addRow(new Object[]{
                                reference, designation, String.valueOf(quantite),
                                formatMontantDinar3(prixUnitaire), formatMontantDinar3(totalHT), tva + "%",
                                formatMontantDinar3(puTTC), "0%", formatMontantDinar3(totalTTC)
                            });
                        }
                    }
                }

                try {
                    net.sf.jasperreports.engine.JasperPrint jasperPrintTicket =
                        com.myapp.print.jasper.TicketReportService.remplirRapport(
                            numeroTicket, date,
                            (clientNom == null || clientNom.isEmpty()) ? "Particulier" : clientNom,
                            modelArticles, null, modePaiement,
                            totalHTBdd, totalTTCBdd, montantRemise
                        );
                    com.myapp.print.jasper.TicketReportService.afficherApercu(jasperPrintTicket);
                } catch (Exception exApercu) {
                    ErrorDialogHelper.afficherErreur(this, "Impossible d'afficher l'aperçu de ce ticket", exApercu);
                }
            }
        } catch (SQLException e) {
            ErrorDialogHelper.afficherErreur(this, "Impossible de charger le détail de ce ticket", e);
        }
    }

    private ImageIcon chargerLogo() {
        try {
            File logoFile = new File("images/logo.png");
            if (logoFile.exists()) {
                return new ImageIcon(new ImageIcon(logoFile.getAbsolutePath()).getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH));
            }
        } catch (Exception e) {}
        return null;
    }
    
    private JButton createIconActionButton(String iconCode, Color bgColor) {
        String fontName = (this.fontAwesomeSolid != null) ? this.fontAwesomeSolid.getFontName() : "SansSerif";
        String html = "<html><color='white'><font face=\"" + fontName + "\" size='4'>" + iconCode + "</font></html>";
        
        JButton btn = new JButton(html);
        btn.setBackground(bgColor);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(true);
        btn.setPreferredSize(new Dimension(40, 32));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        if (iconCode.equals("\uf044")) {
            btn.setToolTipText("✏️ Modifier");
        } else if (iconCode.equals("\uf1f8")) {
            btn.setToolTipText("🗑️ Supprimer");
        }
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(darkenColor(bgColor, 0.85f));
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }

    private class ActionsCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
            panel.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248)));
            
            JButton btnEdit = ListeFactures.this.createIconActionButton("\uf044", new Color(52, 152, 219));
            JButton btnDel  = ListeFactures.this.createIconActionButton("\uf1f8", new Color(231, 76, 60));
            
            panel.add(btnEdit);
            panel.add(btnDel);
            return panel;
        }
    }

    private class ActionsCellEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 5));
        int currentRow;

        public ActionsCellEditor() {
            panel.setOpaque(true);
            
            JButton btnEdit = ListeFactures.this.createIconActionButton("\uf044", new Color(52, 152, 219));
            JButton btnDel  = ListeFactures.this.createIconActionButton("\uf1f8", new Color(231, 76, 60));
            
            btnEdit.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> modifierFacture(currentRow));
            });
            
            btnDel.addActionListener(e -> {
                fireEditingStopped();
                SwingUtilities.invokeLater(() -> supprimerDocument(currentRow));
            });
            
            panel.add(btnEdit);
            panel.add(btnDel);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(table.getSelectionBackground());
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
            new ListeFactures().setVisible(true);
        });
    }
}