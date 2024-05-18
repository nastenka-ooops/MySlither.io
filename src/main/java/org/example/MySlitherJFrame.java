package org.example;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;

public class MySlitherJFrame extends JFrame {
    private static final String[] SNAKES = {"green"};

    private final JTextField server, name;
    private final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel rank, kills;
    private final JSplitPane rightSplitPane, fullSplitPane;
    private final JTextArea log;
    private final JScrollBar logScrollBar;
    private final JTable highscoreList;
    private final MySlitherCanvas canvas;

    //private final long startTime;
    //private final Timer updateTimer;
    private Status status;
    private URI[] serverList;
    private MySlitherWebSocketClient client;
    private final Player player;
    MySlitherModel model;
    final Object modelLock = new Object();

    public MySlitherJFrame() {
        super("MySlither.io");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        getContentPane().setLayout(new BorderLayout());

        canvas = new MySlitherCanvas(this);
        player = new Player("anasty");

        JPanel settings = new JPanel(new GridBagLayout());

        server = new JTextField(18);

        name = new JTextField("shmastya", 16);

        snake = new JComboBox<>(SNAKES);
        snake.setMaximumRowCount(snake.getItemCount());

        useRandomServer = new JCheckBox("use random server", true);
        useRandomServer.addActionListener(a -> {
            setStatus(null);
        });

        connect = new JToggleButton();
//        connect.addActionListener(a -> {
//            switch (status) {
//                case DISCONNECTED:
//                    connect();
//                    break;
//                case CONNECTING:
//                case CONNECTED:
//                    disconnect();
//                    break;
//                case DISCONNECTING:
//                    break;
//            }
//        });
            connect.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                connect.requestFocusInWindow();
                connect.removeAncestorListener(this);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });

        rank = new JLabel();

        kills = new JLabel();

        settings.add(new JLabel("server:"),
                new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(server,
                new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("name:"),
                new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(name,
                new GridBagConstraints(1, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("skin:"),
                new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(snake,
                new GridBagConstraints(1, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(useRandomServer,
                new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(connect,
                new GridBagConstraints(2, 1, 1, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JSeparator(SwingConstants.VERTICAL),
                new GridBagConstraints(3, 0, 1, 3, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 6, 0, 6), 0, 0));
        settings.add(new JLabel("kills:"),
                new GridBagConstraints(4, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(kills,
                new GridBagConstraints(5, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(new JLabel("rank:"),
                new GridBagConstraints(4, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
        settings.add(rank,
                new GridBagConstraints(5, 2, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));

        JComponent upperRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperRow.add(settings);
        getContentPane().add(upperRow, BorderLayout.NORTH);

        log = new JTextArea("hi");
        log.setEditable(false);
        log.setLineWrap(true);
        log.setFont(Font.decode("Monospaced 11"));
        log.setTabSize(4);
        log.getCaret().setSelectionVisible(false);
        log.getInputMap().clear();
        log.getActionMap().clear();
        log.getInputMap().put(KeyStroke.getKeyStroke("END"), "gotoEnd");
        log.getActionMap().put("gotoEnd", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> {
                    logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
                });
            }
        });
        log.getInputMap().put(KeyStroke.getKeyStroke("HOME"), "gotoStart");
        log.getActionMap().put("gotoStart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(() -> {
                logScrollBar.setValue(logScrollBar.getMinimum());
            });
            }
        });

        highscoreList = new JTable(10, 2);
        highscoreList.setEnabled(false);
        highscoreList.getColumnModel().getColumn(0).setMinWidth(64);
        highscoreList.getColumnModel().getColumn(1).setMinWidth(192);
        highscoreList.getColumnModel().getColumn(0).setHeaderValue("length");
        highscoreList.getColumnModel().getColumn(1).setHeaderValue("name");
        highscoreList.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        highscoreList.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        highscoreList.setPreferredScrollableViewportSize(new Dimension(64 + 192, highscoreList.getPreferredSize().height));

        // == split-panes ==
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, canvas, new JScrollPane(highscoreList));
        rightSplitPane.setDividerSize(rightSplitPane.getDividerSize() * 4 / 3);
        rightSplitPane.setResizeWeight(0.99);

        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setPreferredSize(new Dimension(300, logScrollPane.getPreferredSize().height));
        logScrollBar = logScrollPane.getVerticalScrollBar();
        fullSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, logScrollPane, rightSplitPane);
        fullSplitPane.setDividerSize(fullSplitPane.getDividerSize() * 4 / 3);
        fullSplitPane.setResizeWeight(0.1);

        getContentPane().add(fullSplitPane, BorderLayout.CENTER);

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth, screenHeight);

    }
    private void setStatus(Status newStatus) {
        if (newStatus != null) {
            status = newStatus;
        }
        connect.setText(status.buttonText);
        connect.setSelected(status.buttonSelected);
        connect.setEnabled(status.buttonEnabled);
        server.setEnabled(status.allowModifyData && !useRandomServer.isSelected());
        useRandomServer.setEnabled(status.allowModifyData);
        name.setEnabled(status.allowModifyData);
        snake.setEnabled(status.allowModifyData);
    }
}
