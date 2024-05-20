package org.example;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class MySlitherJFrame extends JFrame {
    BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("./log.txt"));
    Map<Integer, Color> SNAKES = new LinkedHashMap<>();
    private final JTextField server, name;
    public final JComboBox<String> snake;
    private final JCheckBox useRandomServer;
    private final JToggleButton connect;
    private final JLabel rank, kills;
    private final JSplitPane rightSplitPane;
    private final JTable highScoreList;
    private final MySlitherCanvas canvas;
    private final Timer updateTimer;
    private Status status;
    private URI[] serverList;
    private MySlitherWebSocketClient client;
    private final Player player;
    MySlitherModel model;
    final Object modelLock = new Object();
    private final long startTime;

    public MySlitherJFrame() throws IOException {
        super("MySlither.io");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initColorMap();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateTimer.cancel();
                if (status == Status.CONNECTING || status == Status.CONNECTED) {
                    disconnect();
                }
                canvas.repaintThread.shutdown();
                try {
                    canvas.repaintThread.awaitTermination(1000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        getContentPane().setLayout(new BorderLayout());

        canvas = new MySlitherCanvas(this);
        player = canvas.mouseInput;

        JPanel settings = new JPanel(new GridBagLayout());

        server = new JTextField(18);

        name = new JTextField("shmastya", 16);

        snake = new JComboBox<>(SNAKES.values().stream().map(this::getColorName).toArray(String[]::new));
        snake.setMaximumRowCount(snake.getItemCount());

        useRandomServer = new JCheckBox("use random server", true);
        useRandomServer.addActionListener(a -> setStatus(null));

        connect = new JToggleButton();
        connect.addActionListener(a -> {
            switch (status) {
                case DISCONNECTED -> connect();
                case CONNECTING, CONNECTED -> disconnect();
                case DISCONNECTING -> {
                }
            }
        });
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
                new GridBagConstraints(0, 0, 1, 1, 0, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(server,
                new GridBagConstraints(1, 0, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2,
                        2, 2), 0, 0));
        settings.add(new JLabel("name:"),
                new GridBagConstraints(0, 1, 1, 1, 0, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(name,
                new GridBagConstraints(1, 1, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2,
                        2, 2), 0, 0));
        settings.add(new JLabel("skin:"),
                new GridBagConstraints(0, 2, 1, 1, 0, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(snake,
                new GridBagConstraints(1, 2, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 2,
                        2, 2), 0, 0));
        settings.add(useRandomServer,
                new GridBagConstraints(2, 0, 1, 1, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(connect,
                new GridBagConstraints(2, 1, 1, 2, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(new JSeparator(SwingConstants.VERTICAL),
                new GridBagConstraints(3, 0, 1, 3, 0, 0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 6, 0,
                        6), 0, 0));
        settings.add(new JLabel("kills:"),
                new GridBagConstraints(4, 1, 1, 1, 0, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(kills,
                new GridBagConstraints(5, 1, 1, 1, 0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(new JLabel("rank:"),
                new GridBagConstraints(4, 2, 1, 1, 0, 0,
                        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));
        settings.add(rank,
                new GridBagConstraints(5, 2, 1, 1, 0, 0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2,
                        2), 0, 0));

        JComponent upperRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        upperRow.add(settings);
        getContentPane().add(upperRow, BorderLayout.NORTH);

        int columnLengthWidth = 64;
        int columnNameWidth = 64;

        highScoreList = new JTable(10, 2);
        highScoreList.setEnabled(false);
        highScoreList.getColumnModel().getColumn(0).setMinWidth(columnLengthWidth);
        highScoreList.getColumnModel().getColumn(1).setMinWidth(columnNameWidth);
        highScoreList.getColumnModel().getColumn(0).setHeaderValue("length");
        highScoreList.getColumnModel().getColumn(1).setHeaderValue("name");
        highScoreList.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        highScoreList.getColumnModel().getColumn(0).setCellRenderer(rightRenderer);
        highScoreList.setPreferredScrollableViewportSize(new Dimension(columnLengthWidth + columnNameWidth,
                highScoreList.getPreferredSize().height));

        // == split-panes ==
        rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                new JScrollPane(highScoreList), canvas);
        rightSplitPane.setDividerSize(rightSplitPane.getDividerSize());
        rightSplitPane.setResizeWeight(0.02);

        getContentPane().add(rightSplitPane, BorderLayout.CENTER);

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth, screenHeight);

        startTime = System.currentTimeMillis();
        validate();
        setStatus(Status.DISCONNECTED);

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (modelLock) {
                    if (status == Status.CONNECTED && model != null) {
                        client.sendData(player.action(model));
                    }
                }
            }
        }, 1, 10);

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

    void log(String text) {
        print(String.format("%6d\t%s", System.currentTimeMillis() - startTime, text));
    }

    private void print(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                bufferedWriter.append(text);
                bufferedWriter.append("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    void onOpen() {
        switch (status) {
            case CONNECTING -> {
                setStatus(Status.CONNECTED);
                client.sendInitRequest(snake.getSelectedIndex(), name.getText());
            }
            case DISCONNECTING -> disconnect();
            default -> throw new IllegalStateException("Connected while not connecting!");
        }
    }

    void onClose() {
        switch (status) {
            case CONNECTED, DISCONNECTING -> {
                setStatus(Status.DISCONNECTED);
                client = null;
            }
            case CONNECTING -> {
                client = null;
                trySingleConnect();
            }
            default ->
                    throw new IllegalStateException("Disconnected while not connecting, connected or disconnecting!");
        }
    }

    public void connect() {
        new Thread(() -> {
            if (status != Status.DISCONNECTED) {
                throw new IllegalStateException("Connecting while not disconnected");
            }
            setStatus(Status.CONNECTING);
            setModel(null);
            if (useRandomServer.isSelected()) {
                log("fetching server list...");
                serverList = MySlitherWebSocketClient.getServerList();
                log("resolved " + serverList.length + " servers");
                if (serverList.length == 0) {
                    log("no server found");
                    setStatus(Status.DISCONNECTED);
                    return;
                }
            }
            if (status == Status.CONNECTING) {
                trySingleConnect();
            }
        }).start();
    }

    void trySingleConnect() {
        if (status != Status.CONNECTING) {
            throw new IllegalStateException("Trying single connection while not connecting");
        }

        if (useRandomServer.isSelected()) {
            client = new MySlitherWebSocketClient(serverList[(int) (Math.random() * serverList.length)], this);
            server.setText(client.getURI().toString());
        } else {
            try {
                client = new MySlitherWebSocketClient(new URI(server.getText()), this);
            } catch (URISyntaxException e) {
                log("invalid server");
                setStatus(Status.DISCONNECTED);
                return;
            }
        }
        log("connecting to " + client.getURI() + " ...");
        client.connect();
    }

    public void disconnect() {
        if (status == Status.DISCONNECTED) {
            throw new IllegalStateException("Already disconnected");
        }
        setStatus(Status.DISCONNECTING);
        if (client != null) {
            client.close();
        }
    }

    void setModel(MySlitherModel model) {
        synchronized (modelLock) {
            this.model = model;
            rank.setText(null);
            kills.setText(null);
        }
    }

    void setRank(int newRank, int playerCount) {
        rank.setText(newRank + "/" + playerCount);
    }

    void setKills(int newKills) {
        kills.setText(String.valueOf(newKills));
    }

    void setHighScoreData(int row, String name, int length, boolean highlighted) {
        highScoreList.setValueAt(highlighted ? "<html><b>" + length + "</b></html>" : length, row, 0);
        highScoreList.setValueAt(highlighted ? "<html><b>" + name + "</b></html>" : name, row, 1);
    }

    void initColorMap() {
        SNAKES.put(0, new Color(240, 255, 255)); // Azure
        SNAKES.put(1, Color.RED);
        SNAKES.put(2, Color.GREEN);
        SNAKES.put(3, Color.BLUE);
        SNAKES.put(4, Color.YELLOW);
        SNAKES.put(5, Color.ORANGE);
        SNAKES.put(6, Color.MAGENTA);
        SNAKES.put(7, Color.CYAN);
        SNAKES.put(8, Color.PINK);
        SNAKES.put(9, Color.GRAY);
        SNAKES.put(10, Color.BLACK);
        SNAKES.put(11, new Color(128, 0, 0)); // Maroon
        SNAKES.put(12, new Color(0, 128, 0)); // Dark Green
        SNAKES.put(13, new Color(0, 0, 128)); // Navy
        SNAKES.put(14, new Color(128, 128, 0)); // Olive
        SNAKES.put(15, new Color(128, 0, 128)); // Purple
        SNAKES.put(16, new Color(0, 128, 128)); // Teal
        SNAKES.put(17, new Color(192, 192, 192)); // Silver
        SNAKES.put(18, new Color(255, 165, 0)); // Orange
        SNAKES.put(19, new Color(75, 0, 130)); // Indigo
        SNAKES.put(20, new Color(255, 20, 147)); // Deep Pink
        SNAKES.put(21, new Color(0, 255, 127)); // Spring Green
        SNAKES.put(22, new Color(70, 130, 180)); // Steel Blue
        SNAKES.put(23, new Color(255, 99, 71)); // Tomato
        SNAKES.put(24, new Color(220, 20, 60)); // Crimson
        SNAKES.put(25, new Color(46, 139, 87)); // Sea Green
        SNAKES.put(26, new Color(0, 191, 255)); // Deep Sky Blue
        SNAKES.put(27, new Color(218, 112, 214)); // Orchid
        SNAKES.put(28, new Color(255, 215, 0)); // Gold
        SNAKES.put(29, new Color(0, 100, 0)); // Dark Green
        SNAKES.put(30, new Color(139, 69, 19)); // Saddle Brown
        SNAKES.put(31, new Color(255, 140, 0)); // Dark Orange
        SNAKES.put(32, new Color(255, 105, 180)); // Hot Pink
        SNAKES.put(33, new Color(173, 216, 230)); // Light Blue
        SNAKES.put(34, new Color(255, 239, 213)); // Papaya Whip
        SNAKES.put(35, new Color(255, 228, 196)); // Bisque
        SNAKES.put(36, new Color(0, 255, 255)); // Aqua
        SNAKES.put(37, new Color(127, 255, 212)); // Aquamarine
        SNAKES.put(38, new Color(240, 230, 140)); // Khaki
        SNAKES.put(39, new Color(255, 228, 225)); // Misty Rose
    }

    public String getColorName(Color color) {
        if (Color.RED.equals(color)) return "Red";
        if (Color.GREEN.equals(color)) return "Green";
        if (Color.BLUE.equals(color)) return "Blue";
        if (Color.YELLOW.equals(color)) return "Yellow";
        if (Color.ORANGE.equals(color)) return "Orange";
        if (Color.MAGENTA.equals(color)) return "Magenta";
        if (Color.CYAN.equals(color)) return "Cyan";
        if (Color.PINK.equals(color)) return "Pink";
        if (Color.GRAY.equals(color)) return "Gray";
        if (Color.BLACK.equals(color)) return "Black";
        if (Color.WHITE.equals(color)) return "White";
        if (new Color(128, 0, 0).equals(color)) return "Maroon";
        if (new Color(0, 128, 0).equals(color)) return "Dark Green";
        if (new Color(0, 0, 128).equals(color)) return "Navy";
        if (new Color(128, 128, 0).equals(color)) return "Olive";
        if (new Color(128, 0, 128).equals(color)) return "Purple";
        if (new Color(0, 128, 128).equals(color)) return "Teal";
        if (new Color(192, 192, 192).equals(color)) return "Silver";
        if (new Color(255, 165, 0).equals(color)) return "Orange";
        if (new Color(75, 0, 130).equals(color)) return "Indigo";
        if (new Color(255, 20, 147).equals(color)) return "Deep Pink";
        if (new Color(0, 255, 127).equals(color)) return "Spring Green";
        if (new Color(70, 130, 180).equals(color)) return "Steel Blue";
        if (new Color(255, 99, 71).equals(color)) return "Tomato";
        if (new Color(220, 20, 60).equals(color)) return "Crimson";
        if (new Color(46, 139, 87).equals(color)) return "Sea Green";
        if (new Color(0, 191, 255).equals(color)) return "Deep Sky Blue";
        if (new Color(218, 112, 214).equals(color)) return "Orchid";
        if (new Color(255, 215, 0).equals(color)) return "Gold";
        if (new Color(0, 100, 0).equals(color)) return "Dark Green";
        if (new Color(139, 69, 19).equals(color)) return "Saddle Brown";
        if (new Color(255, 140, 0).equals(color)) return "Dark Orange";
        if (new Color(255, 105, 180).equals(color)) return "Hot Pink";
        if (new Color(173, 216, 230).equals(color)) return "Light Blue";
        if (new Color(255, 239, 213).equals(color)) return "Papaya Whip";
        if (new Color(255, 228, 196).equals(color)) return "Bisque";
        if (new Color(0, 255, 255).equals(color)) return "Aqua";
        if (new Color(127, 255, 212).equals(color)) return "Aquamarine";
        if (new Color(240, 230, 140).equals(color)) return "Khaki";
        if (new Color(255, 228, 225).equals(color)) return "Misty Rose";
        if (new Color(240, 255, 255).equals(color)) return "Azure";
        return "Unknown Color";
    }
}
