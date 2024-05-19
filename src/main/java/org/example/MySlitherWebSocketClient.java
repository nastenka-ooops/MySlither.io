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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.MySlitherModel.PI2;

public class MySlitherWebSocketClient extends WebSocketClient {
    private static final double ANGLE_CONSTANT = 16777215;
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
        if (b.length < 3) {
            view.log("too short");
            return;
        }
        int[] data = new int[b.length];
        for (int i = 0; i < b.length; i++) {
            data[i] = b[i] & 0xFF;
        }
        char cmd = (char) data[2];
        switch (cmd) {
            case '6':
                processPreInitResponse(data);
                break;
            case 'a':
                processInitResponse(data);
                break;
            case 'e':
            case 'E':
            case '3':
            case '4':
            case '5':
                processUpdateBodyparts(data, cmd);
                break;
            case 'h':
                processUpdateFam(data);
                break;
            case 'r':

            case 'g':
            case 'G':
            case 'n':
            case 'N':
                processUpdateSnakePosition(data, cmd);
                break;
            case 'l':
            case 'v':
            case 'w':
            case 'W':
            case 'm':
            case 'p':
            case 'u':
            case 's':
                processAddRemoveSnake(data);
                break;
            case 'F':
            case 'b':
            case 'f':
            case 'c':
            case 'j':
            case 'y':
            case 'o':
            case 'k':

        }
    }

    private void processUpdateFam(int[] data) {
        if (data.length!=8){
            view.log("update fam wrong length!");
            return;
        }
        int snakeId = (data[3] << 8) | (data[4]);
        synchronized (view.modelLock){
            Snake snake = model.getSnake(snakeId);
            snake.setFam(((data[5] << 16) | (data[6] << 8) | (data[7])) / ANGLE_CONSTANT);
        }
    }

    private void processUpdateBodyparts(int[] data, char cmd) {
        if (data.length != 8 && data.length != 7 && data.length != 6) {
            view.log("update body-parts wrong length!");
            return;
        }

        int snakeId = (data[3] << 8) | (data[4]);
        int newDir = -1;
        double newAng = -1;
        double newWang = -1;
        double newSpeed = -1;

        if (data.length==8){
            newDir = cmd == 'e' ? 1 : 2;

            newAng = getNewAngle(data[5]);
            newWang = getNewAngle(data[6]);
            newSpeed = getNewSpeed(data[7]);

        } else if (data.length==7){
            switch (cmd){
                case 'e':
                    newAng = getNewAngle(data[5]);
                    newSpeed = getNewSpeed(data[6]);
                    break;
                case 'E':
                    newDir = 1;
                    newWang = getNewAngle(data[5]);
                    newSpeed = getNewSpeed(data[6]);
                case '3':
                    newDir = 1;
                    newAng = getNewAngle(data[5]);
                    newWang = getNewAngle(data[6]);
                case '4':
                    newDir = 2;
                    newWang = getNewAngle(data[5]);
                    newSpeed = getNewSpeed(data[6]);
                case '5':
                    newDir = 2;
                    newAng = getNewAngle(data[5]);
                    newWang = getNewAngle(data[6]);
                default:
                    view.log("update body-parts invalid cmd/length: " + cmd + ", " + data.length);
                    return;
            }
        } else if (data.length==6){
            switch (cmd){
                case 'e':
                    newAng = getNewAngle(data[5]);
                    break;
                case 'E':
                    newDir = 1;
                    newWang = getNewAngle(data[5]);
                case '3':
                    newSpeed = getNewSpeed(data[5]);
                case '4':
                    newDir = 2;
                    newWang = getNewAngle(data[5]);
                case '5':
                    newSpeed = getNewSpeed(data[5]);
                default:
                    view.log("update body-parts invalid cmd/length: " + cmd + ", " + data.length);
                    return;
            }
        }

        synchronized (view.modelLock){
            Snake snake = model.getSnake(snakeId);

            if (newDir!=-1){
                snake.dir=newDir;
            }
            if (newAng!=-1){
                snake.ang = newAng;
            }
            if (newWang!=-1){
                snake.wang = newWang;
            }
            if (newSpeed!=-1){
                snake.speed = newSpeed;
            }
        }
    }

    private double getNewAngle(int angle) {
        return angle * PI2 / 256;
    }
    private double getNewSpeed(int speed) {
        return speed / 18.0;
    }

    private void processUpdateSnakePosition(int[] data, char cmd) {
        boolean isAbsoluteCoordinates = (cmd == 'g' || cmd == 'n');
        boolean isNewBodyPart = (cmd == 'n' || cmd == 'N');

        if (data.length != 5 + (isAbsoluteCoordinates ? 4 : 2) + (isNewBodyPart ? 3 : 0)) {
            view.log("update snake body wrong length!");
            return;
        }

        int snakeId = (data[3] << 8) | (data[4]);

        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeId);
            SnakeBodyPart head = snake.body.getFirst();

            double newX = isAbsoluteCoordinates ? ((data[5] << 8) | (data[6])) : (data[5] - 128 + head.x);
            double newY = isAbsoluteCoordinates ? ((data[7] << 8) | (data[8])) : (data[6] - 128 + head.y);

            if (isNewBodyPart) {
                snake.setFam(((data[isAbsoluteCoordinates ? 9 : 7] << 16)
                        | (data[isAbsoluteCoordinates ? 10 : 8] << 8)
                        | data[isAbsoluteCoordinates ? 11 : 9]) / ANGLE_CONSTANT);
            } else {
                snake.body.pollLast();
            }

            snake.body.addFirst(new SnakeBodyPart(newX, newY));

            snake.x = newX;
            snake.y = newY;

            view.log("snake was moved to x " + snake.x + " y "+ snake.y);
            if (isNewBodyPart)
                view.log("new bode part was add");
        }
    }

    private void processAddRemoveSnake(int[] data) {
        if (data.length == 6) {
            int id = (data[3] << 8) | (data[4]);
            model.removeSnake(id);
            view.log("add snake with id " + id);
        } else if (data.length >= 34) {
            int snakeId = (data[3] << 8) | (data[4]);

            double ang = ((data[5] << 16) | (data[6] >> 8) | data[7]) * PI2 / ANGLE_CONSTANT;
            double wang = ((data[9] << 16) | (data[10] >> 8) | data[11]) * PI2 / ANGLE_CONSTANT;

            double speed = ((data[12] << 8) | (data[13])) / 1000.0;
            //Snake last body part fullness
            double fam = ((data[14] << 16) | (data[15] >> 8) | data[16]) / ANGLE_CONSTANT;

            //TODO do smth with this
            int skin = data[17];

            double x = ((data[18] << 16) | (data[19] << 8) | (data[20])) / 5.0;
            double y = ((data[21] << 16) | (data[22] << 8) | (data[23])) / 5.0;

            int nameLength = data[24];
            StringBuilder name = new StringBuilder(nameLength);
            for (int i = 0; i < nameLength; i++) {
                name.append((char) data[24 + i]);
            }

            //TODO and with this
            int customSkinDataLength = data[25 + nameLength];
            StringBuilder customSkinDataName = new StringBuilder(customSkinDataLength);
            for (int i = 0; i < customSkinDataLength; i++) {
                customSkinDataName.append((char) data[25 + nameLength + i]);
            }

            double currentBodyPartX = ((data[26 + nameLength + customSkinDataLength] << 16)
                    | (data[27 + nameLength + customSkinDataLength] << 8)
                    | (data[28 + nameLength + customSkinDataLength])) / 5.0;
            double currentBodyPartY = ((data[29 + nameLength + customSkinDataLength] << 16)
                    | (data[30 + nameLength + customSkinDataLength] << 8)
                    | (data[21 + nameLength + customSkinDataLength])) / 5.0;

            Deque<SnakeBodyPart> body = new ArrayDeque<>();
            body.addFirst(new SnakeBodyPart(currentBodyPartX, currentBodyPartY));

            for (int nextSnakeBodyPart = 32 + nameLength + customSkinDataLength;
                 nextSnakeBodyPart + 1 < data.length; nextSnakeBodyPart += 2) {
                currentBodyPartX += (data[nextSnakeBodyPart] - 127) / 2.0;
                currentBodyPartY += (data[nextSnakeBodyPart + 1] - 127) / 2.0;
                body.addFirst(new SnakeBodyPart(currentBodyPartX, currentBodyPartY));
            }

            model.addSnake(snakeId, name.toString(), x, y, ang, wang, speed, fam, body);
            view.log("add snake with id " + snakeId + " name " + name + " x " + x + " y " + y + " ang " + ang + " wang " + wang +
                    " speed " + speed + " fam " + fam + " and body " + body.size());
        } else {
            view.log("add/remove snake wrong length!");
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        view.log("closed: " + code + ", " + remote + ", " + reason);
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

    void processPreInitResponse(int[] data) {
        view.log("sending decrypted, manipulated secret");
        send(decodeSecret(data));

        view.log("send init-request");
        send(initRequest);
    }

    void processInitResponse(int[] data) {
        if (data.length != 26) {
            view.log("init response wrong length!");
            return;
        }
        int gameRadius = (data[3] << 16) | (data[4] << 8) | (data[5]);
        //get mscps (maximum snake length in body parts units)
        int mscps = (data[6] << 8) | data[7];
        int sectorSize = (data[8] << 8) | data[9];
        // get spangdv (value / 10) (coef. to calculate angular speed change depending snake speed)
        double spangdv = data[12] / 10.0;
        // nsp1 (value / 100) (Maybe nsp stands for "node speed"?)
        double nsp1 = ((data[13] << 8) | data[14]) / 100.0;
        double nsp2 = ((data[15] << 8) | data[16]) / 100.0;
        double nsp3 = ((data[17] << 8) | data[18]) / 100.0;
        // get mamu (value / 1E3) (basic snake angular speed)
        double mamu1 = ((data[19] << 8) | data[20]) / 1000.0;
        //manu2 (value / 1E3) (angle in rad per 8ms at which prey can turn)
        double mamu2 = ((data[21] << 8) | data[22]) / 1000.0;
        //sct is a snake body parts count (length) taking values between [2 .. mscps].
        //fpsls[mscps] contains snake volume (score) to snake length in body parts units. 1/fmlts[mscps] contains
        // body part volume (score) to certain snake length.
        double cst = ((data[23] << 8) | data[24]) / 1000.0;
        int protocolVersion = data[25];

        if (protocolVersion != 11) {
            view.log("wrong protocol version (" + protocolVersion + ")");
            return;
        }

        model = new MySlitherModel(spangdv, nsp1, nsp2, nsp3, mamu1, mamu2, gameRadius, sectorSize, cst, mscps, view);
        view.log("add game model " + spangdv + " nps1 " + nsp1 + " nps2 " + nsp2 + " nps3 " + nsp3 + " mamu1 " + mamu1 +
                " mamu2 " + mamu2 + " dame radius " + gameRadius + " sector size " + sectorSize + " cst " + cst + " mscps " + mscps);
        view.setModel(model);
        view.setKills(0);
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

    static URI[] getServerList() {
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
