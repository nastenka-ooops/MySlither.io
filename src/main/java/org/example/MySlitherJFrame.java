package org.example;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.TimerTask;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

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
    private final JTable highScoreList;
    private final MySlitherCanvas canvas;

    private final long startTime;
    private final Timer updateTimer;
    private Status status;
    private URI[] serverList;
    private MySlitherWebSocketClient client;
    private final Player player;
    MySlitherModel model;
    final Object modelLock = new Object();

    public MySlitherJFrame() {
        super("MySlither.io");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                updateTimer.cancel();
                if (status==Status.CONNECTING || status==Status.CONNECTED){
                    disconnect();
                }
                canvas.repaintThread.shutdown();
                try {
                    canvas.repaintThread.awaitTermination(1000, TimeUnit.NANOSECONDS);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

        getContentPane().setLayout(new BorderLayout());

        canvas = new MySlitherCanvas(this);
        player = new Player("shmasty");

        JPanel settings = new JPanel(new GridBagLayout());

        server = new JTextField(18);

        name = new JTextField("shmastya", 16);

        snake = new JComboBox<>(SNAKES);
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
                SwingUtilities.invokeLater(() ->
                        logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount()));
            }
        });
        log.getInputMap().put(KeyStroke.getKeyStroke("HOME"), "gotoStart");
        log.getActionMap().put("gotoStart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(() -> logScrollBar.setValue(logScrollBar.getMinimum()));
            }
        });

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
                canvas, new JScrollPane(highScoreList));
        rightSplitPane.setDividerSize(rightSplitPane.getDividerSize() * 4 / 3);
        rightSplitPane.setResizeWeight(0.99);

        JScrollPane logScrollPane = new JScrollPane(log);
        logScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScrollPane.setPreferredSize(new Dimension(300, logScrollPane.getPreferredSize().height));
        logScrollBar = logScrollPane.getVerticalScrollBar();
        fullSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, logScrollPane,
                rightSplitPane);
        fullSplitPane.setDividerSize(fullSplitPane.getDividerSize() * 4 / 3);
        fullSplitPane.setResizeWeight(0.1);

        getContentPane().add(fullSplitPane, BorderLayout.CENTER);

        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        int screenHeight = Toolkit.getDefaultToolkit().getScreenSize().height;
        setSize(screenWidth, screenHeight);

        validate();
        startTime = System.currentTimeMillis();
        setStatus(Status.DISCONNECTED);

        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (modelLock) {
                    if (status == Status.CONNECTED && model != null) {
                        //model.update();
                        //client.sendData(player.action(model));
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
            boolean scrollToBottom = !logScrollBar.getValueIsAdjusting() && logScrollBar.getValue() >= logScrollBar.getMaximum() - logScrollBar.getVisibleAmount();
            log.append('\n' + text);
            fullSplitPane.getLeftComponent().validate();
            if (scrollToBottom) {
                logScrollBar.setValue(logScrollBar.getMaximum() - logScrollBar.getVisibleAmount());
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

    void onClose(){
        switch (status){
            case CONNECTED, DISCONNECTING -> {
                setStatus(Status.DISCONNECTED);
                client=null;
            }
            case CONNECTING -> {
                client=null;
                trySingleConnect();
            }
            default -> throw new IllegalStateException("Disconnected while not connecting, connected or disconnecting!");
        }
    }

    public void connect(){
        new Thread(()->{
           if (status!=Status.DISCONNECTED){
               throw new IllegalStateException("Connecting while not disconnected");
           }
           setStatus(Status.CONNECTING);
           setModel(null);
            if (useRandomServer.isSelected()) {
                log("fetching server list...");
                serverList=MySlitherWebSocketClient.getServerList();
                log("resolved " + serverList.length + " servers");
                if (serverList.length == 0){
                    log("no server found");
                    setStatus(Status.DISCONNECTED);
                    return;
                }
            }
            if (status == Status.CONNECTING){
                trySingleConnect();
            }
        }).start();
    }

    void trySingleConnect(){
        if (status!=Status.CONNECTING){
            throw new IllegalStateException("Trying single connection while not connecting");
        }

        if (useRandomServer.isSelected()){
            client = new MySlitherWebSocketClient(serverList[(int) (Math.random()*serverList.length)], this);
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
        log("connecting to "+client.getURI()+" ...");
        client.connect();
    }
    public void disconnect(){
        if (status==Status.DISCONNECTED){
            throw new IllegalStateException("Already disconnected");
        }
        setStatus(Status.DISCONNECTING);
        if (client!=null){
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
    void setRank(int newRank, int playerCount){
        rank.setText(newRank+"/"+playerCount);
    }
    void setKills(int newKills){
        kills.setText(String.valueOf(newKills));
    }

    void setHighScoreData(int row, String name, int length, boolean highlighted) {
        highScoreList.setValueAt(highlighted ? "<html><b>" + length + "</b></html>" : length, row, 0);
        highScoreList.setValueAt(highlighted ? "<html><b>" + name + "</b></html>" : name, row, 1);
    }
}
