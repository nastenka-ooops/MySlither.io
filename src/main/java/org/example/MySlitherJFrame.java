package org.example;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.net.URI;

public class MySlitherJFrame extends JFrame {
    private static final String[] SNAKES = {"green"};

    private final JTextField server, name;
    private final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel rank, kills;
    //private final JSplitPane rightSplitPane, fullSplitPane;
    //private final JTextArea log;
    //private final JScrollBar logScrollBar;
    //private final JTable highscoreList;
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

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth * 3 / 4, screenHeight * 4 / 5);

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
