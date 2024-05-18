package org.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MySlitherWebSocketClient extends WebSocketClient {
    private static final Map<String, String> HEADER = new LinkedHashMap<>();

    private final MySlitherJFrame view;
    private MySlitherModel model;

    private byte[] initRequest;

    static {
        HEADER.put("Origin", "http://slither.io");
        HEADER.put("Pragma", "no-cache");
        HEADER.put("Cache-Control", "no-cache");
    }

    public MySlitherWebSocketClient(URI serverUri, MySlitherJFrame view) {
        super(serverUri, new Draft_6455(), HEADER);
        this.view = view;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        view.log("connected: " + handshake.getHttpStatusMessage());
        view.onOpen();
    }

    @Override
    public void onMessage(String message) {
        view.log("message: " + message);
    }

    @Override
    public void onMessage(ByteBuffer buffer) {
        byte[] b = buffer.array();
        if (b.length > 3) {
            view.log("too short");
            return;
        }
        int[] data = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            data[i] = b[i] & 0xFF;
        }
        char cmd = (char) data[2];
        switch (cmd){
            case '6':
                processPreInitResponse(data);
                break;
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        view.log("closed: "+code+", "+remote+", "+reason);
        view.onClose();
    }

    @Override
    public void onError(Exception ex) {

    }

    void sendInitRequest(int snakeSkin, String nick) {
        // Set username and skin
        initRequest = new byte[4 + nick.length()];
        //packet-type (here 115 = 's')
        initRequest[0] = 115;
        //protocol version
        initRequest[1] = 10;
        // skin ID
        initRequest[2] = (byte) snakeSkin;
        // nick length
        initRequest[3] = (byte) nick.length();
        // nick bytes
        for (int i = 0; i < nick.length(); i++) {
            initRequest[i + 4] = (byte) nick.codePointAt(i);
        }

        //pre-init response
        view.log("sending pre-init request");
        //This is the first packet sent. The server will then respond with the Pre-init response.
        send(new byte[]{99});
    }

    void processPreInitResponse(int[] data){
        view.log("sending decrypted, manipulated secret");
        send(decodeSecret(data));

        view.log("send init-request");
        send(initRequest);
    }
    private static byte[] decodeSecret(int[] secret) {

        byte[] result = new byte[24];

        int globalValue = 0;
        for (int i = 0; i < 24; i++) {
            int value1 = secret[17 + i * 2];
            if (value1 <= 96) {
                value1 += 32;
            }
            value1 = (value1 - 98 - i * 34) % 26;
            if (value1 < 0) {
                value1 += 26;
            }

            int value2 = secret[18 + i * 2];
            if (value2 <= 96) {
                value2 += 32;
            }
            value2 = (value2 - 115 - i * 34) % 26;
            if (value2 < 0) {
                value2 += 26;
            }

            int interimResult = (value1 << 4) | value2;
            int offset = interimResult >= 97 ? 97 : 65;
            interimResult -= offset;
            if (i == 0) {
                globalValue = 2 + interimResult;
            }
            result[i] = (byte) ((interimResult + globalValue) % 26 + offset);
            globalValue += 3 + interimResult;
        }

        return result;
    }

    static URI[] getServerList(){
        String i33628_String;
        try {
            HttpURLConnection i49526_HttpURLConnection = (HttpURLConnection)
                    new URI("http://slither.io/i33628.txt").toURL().openConnection();
            i49526_HttpURLConnection.setRequestProperty("User-Agent", "java/1.8.0_72");
            InputStream i49526_InputStream = i49526_HttpURLConnection.getInputStream();
            BufferedReader i49526_BufferedReader = new BufferedReader(new InputStreamReader(i49526_InputStream));
            i33628_String = i49526_BufferedReader.lines().collect(Collectors.joining("\n"));
        } catch (IOException | URISyntaxException ex) {
            throw new Error("Error reading server-list!");
        }

        int[] data = new int[(i33628_String.length() - 1) / 2];
        for (int i = 0; i < data.length; i++) {
            int u1 = (i33628_String.codePointAt(i * 2 + 1) - 97 - 14 * i) % 26;
            if (u1 < 0) {
                u1 += 26;
            }
            int u2 = (i33628_String.codePointAt(i * 2 + 2) - 104 - 14 * i) % 26;
            if (u2 < 0) {
                u2 += 26;
            }
            data[i] = (u1 << 4) + u2;
        }

        URI[] serverList = new URI[(i33628_String.length() - 1) / 22];
        for (int i = 0; i < serverList.length; i++) {
            try {
                serverList[i] = new URI("ws://"
                        + data[i * 11 + 0] + "."
                        + data[i * 11 + 1] + "."
                        + data[i * 11 + 2] + "."
                        + data[i * 11 + 3] + ":"
                        + ((data[i * 11 + 4] << 16) + (data[i * 11 + 5] << 8) + data[i * 11 + 6])
                        + "/slither");
            } catch (URISyntaxException ex) {
                throw new Error("Error building server-address!");
            }
        }
        return serverList;
    }
}
