package techseed.TextfieldAutoSuggestor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *  * AutoSuggestor provides an auto-completion popup for a JTextField, fetching
 * suggestions dynamically from a database table based on user input.
 * <p>
 * It shows a JPopupMenu with a JList of suggestions matching the current text
 * in the JTextField. Suggestions are queried from the specified database table
 * and columns using a LIKE '%input%' search.
 * <p>
 * Usage:
 * <pre>
 *     AutoSuggestor suggestor = new AutoSuggestor(
 *      myTextField,
 *      myDbConnection,
 *      "mysql_table_name",
 *      Arrays.asList("col1", "col2"),
 *      "CustomerID",
 *      null, //Image Icon
 *      (id, label) -&gt; { System.out.println("Selected ID: " + id + ", Label: " + label); }
 *    );
 * suggestor.setSuggestionFont(new Font("Segoe UI", Font.PLAIN, 16));
 * suggestor.setPopupSize(300, 180);
 * </pre> This example listens for selections and prints the selected id and
 * label.
 *
 * @author Sapumal Bandara
 * @since 2025
 * @version 1.0
 */
public class AutoSuggestor {

    private final JTextField textField;
    private final JPopupMenu popupMenu;
    private final Timer debounceTimer;
    private final Connection connection;
    private final String tableName;
    private final List<String> searchColumns;
    private final String idColumn;
    private final Icon icon;
    private final BiConsumer<String, String> onSelectCallback;

    private final JList<String> suggestionList = new JList<>();
    private final JScrollPane scrollPane = new JScrollPane(suggestionList);
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final Map<String, String> labelToIdMap = new LinkedHashMap<>();

    private Font suggestionFont = new Font("Segoe UI", Font.PLAIN, 15);
    private int popupWidth = 250;
    private int popupHeight = 150;

    /**
     * Constructs an AutoSuggestor for a given JTextField and database
     * parameters.
     *
     * @param textField The JTextField to attach the auto-suggestion feature.
     * @param connection The active JDBC Connection to your database.
     * @param tableName The database table name to search for suggestions.
     * @param searchColumns The list of columns to search (LIKE) for matching
     * text.
     * @param idColumn The column name containing unique IDs for the records.
     * @param icon An optional Icon to show alongside suggestions (currently
     * unused).
     * @param onSelectCallback A callback function accepting (id, label) when a
     * suggestion is selected.
     */
    public AutoSuggestor(JTextField textField,
            Connection connection,
            String tableName,
            List<String> searchColumns,
            String idColumn,
            Icon icon,
            BiConsumer<String, String> onSelectCallback) {
        this.textField = textField;
        this.connection = connection;
        this.tableName = tableName;
        this.searchColumns = searchColumns;
        this.idColumn = idColumn;
        this.icon = icon;
        this.onSelectCallback = onSelectCallback;
        this.popupMenu = new JPopupMenu();
        this.debounceTimer = new Timer(300, e -> showSuggestions());
        debounceTimer.setRepeats(false);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounceTimer.restart();
            }
        });

        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popupMenu.isVisible()) {
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    suggestionList.requestFocus();
                }
            }
        });

        suggestionList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    applySelection();
                }
            }
        });

        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                applySelection();
            }
        });
    }

    private void applySelection() {
        String selected = suggestionList.getSelectedValue();
        if (selected != null) {
            textField.setText(selected);
            if (onSelectCallback != null) {
                onSelectCallback.accept(labelToIdMap.get(selected), selected);
            }
            popupMenu.setVisible(false);
            textField.requestFocus();
        }
    }

    private void showSuggestions() {
        String input = textField.getText().trim();
        if (input.isEmpty()) {
            popupMenu.setVisible(false);
            return;
        }

        labelToIdMap.clear();
        listModel.clear();

        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < searchColumns.size(); i++) {
            if (i > 0) {
                whereClause.append(" OR ");
            }
            whereClause.append(searchColumns.get(i)).append(" LIKE ?");
        }

        String query = "SELECT " + idColumn + ", " + String.join(", ", searchColumns)
                + " FROM " + tableName
                + " WHERE " + whereClause
                + " LIMIT 10";

        try (PreparedStatement pst = connection.prepareStatement(query)) {
            for (int i = 1; i <= searchColumns.size(); i++) {
                pst.setString(i, "%" + input + "%");
            }
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String id = rs.getString(idColumn);
                List<String> values = new ArrayList<>();
                for (String col : searchColumns) {
                    values.add(rs.getString(col));
                }
                String label = String.join(" | ", values);
                listModel.addElement(label);
                labelToIdMap.put(label, id);
            }

            if (!listModel.isEmpty()) {
                suggestionList.setModel(listModel);
                suggestionList.setFont(suggestionFont);
                suggestionList.setSelectedIndex(0);
                scrollPane.setPreferredSize(new Dimension(popupWidth, popupHeight));

                popupMenu.removeAll();
                popupMenu.add(scrollPane);
                popupMenu.setFocusable(false);
                popupMenu.show(textField, 0, textField.getHeight());
            } else {
                popupMenu.setVisible(false);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

     /**
     * Sets the font used in the suggestion list popup.
     * 
     * @param font The Font to use for suggestions.
     */
    public void setSuggestionFont(Font font) {
        this.suggestionFont = font;
    }

     /**
     * Sets the size of the suggestion popup.
     * 
     * @param width  Width of the popup in pixels.
     * @param height Height of the popup in pixels.
     */
    public void setPopupSize(int width, int height) {
        this.popupWidth = width;
        this.popupHeight = height;
    }
}
