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
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.MySlitherModel.PI2;

public class MySlitherWebSocketClient extends WebSocketClient {

    private static final byte[] DATA_PING = new byte[]{(byte) 251};
    private static final byte[] DATA_BOOST_START = new byte[]{(byte) 253};
    private static final byte[] DATA_BOOST_STOP = new byte[]{(byte) 254};
    private static final double ANGLE_CONSTANT = 16777215;
    private static final Map<String, String> HEADER = new LinkedHashMap<>();

    private final MySlitherJFrame view;
    private MySlitherModel model;

    private byte[] initRequest;
    private long lastAngleTime, lastPingTime;
    private byte lastAngleContent, angleToBeSent;
    private boolean lastBoostContent;
    private boolean waitingForPong;


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
            case '6' -> processPreInitResponse(data);
            case 'a' -> processInitResponse(data);
            case 'e', 'E', '3', '4', '5' -> processUpdateBodyparts(data, cmd);
            case 'h' -> processUpdateFam(data);
            case 'r' -> processRemoveSnakePart(data);
            case 'g', 'G', 'n', 'N' -> processUpdateSnakePosition(data, cmd);
            case 'l' -> processLeaderboard(data);
            case 'v' -> processDead(data);
            case 'w' -> processRemoveSector(data);
            case 'W' -> processAddSector(data);
            case 'm' -> processGlobalHighScore(data);
            case 'p' -> processPong(data);
            case 'u' -> processUpdateMinimap(data);
            case 's' -> processAddRemoveSnake(data);
            case 'F', 'b', 'f' -> processAddFood(data, cmd);
            case 'c' -> processRemoveFood(data);
            case 'j' -> processUpdatePrey(data);
            case 'y' -> processAddRemovePrey(data);

            // case 'o':
            case 'k' -> processKill(data);
        }
    }

    void sendData(Player.Wish wish){
        if (wish.angle != null){
            angleToBeSent = (byte) (wish.angle * 251 / PI2);
        }
        if (angleToBeSent != lastAngleContent && System.currentTimeMillis()-lastAngleTime>100){
            lastAngleTime = System.currentTimeMillis();
            lastAngleContent = angleToBeSent;
            send(new byte[]{angleToBeSent});
        }

        if (wish.boost != null && wish.boost != lastBoostContent){
            lastBoostContent = wish.boost;
            send(wish.boost ? DATA_BOOST_START : DATA_BOOST_STOP);
        }

        if (!waitingForPong && System.currentTimeMillis() - lastPingTime > 250){
            lastPingTime = System.currentTimeMillis();
            waitingForPong = true;
            send(DATA_PING);
        }
    }

    private void processKill(int[] data) {
        if (data.length != 8) {
            view.log("kill wrong length!");
            return;
        }

        int killerId = (data[3] << 8) | (data[4]);
        int kills = (data[5] << 16) |(data[6] << 8) | (data[7]);

        if (killerId == model.snake.id) {
            view.setKills(kills);
        } else {
            view.log("kill packet with invalid id: " + killerId);
        }
    }

    private void processAddRemovePrey(int[] data) {
        if (data.length == 5) {
            int preyId = (data[3] << 8) | (data[4]);
            model.removePrey(preyId);
        } else if (data.length == 7) {
            int preyId = (data[3] << 8) | (data[4]);
            int eatenId = (data[5] << 8) | (data[6]);
            model.removePrey(preyId);
        } else if (data.length==22){
            int preyId = (data[3] << 8) | (data[4]);
            int colorId = data[5];
            double x = ((data[6] << 16) |(data[7] << 8) | (data[8]))/5.0;
            double y = ((data[9] << 16) |(data[10] << 8) | (data[11]))/5.0;
            double size = data[12]/5.0;
            int direction = data[13]-48;
            double wang = ((data[14] << 16) |(data[15] << 8) | (data[16]))*PI2/ANGLE_CONSTANT;
            double ang = ((data[17] << 16) |(data[18] << 8) | (data[19]))*PI2/ANGLE_CONSTANT;
            double speed = ((data[20] << 8) | (data[21]))/1000.0;
            model.addPrey(preyId, new Prey(x, y, direction, wang, ang, speed, size));
        } else {
            view.log("add/remove prey wrong length!");
        }
    }
    @Override
    public void onClose(int code, String reason, boolean remote) {
        view.log("closed: " + code + ", " + remote + ", " + reason);
        view.onClose();
    }

    @Override
    public void onError(Exception ex) {
        view.log("ERROR: " + ex);
        ex.printStackTrace();
    }

    private void processUpdatePrey(int[] data) {
        if (data.length != 11 && data.length != 12 && data.length != 13 && data.length != 14 &&
                data.length != 15 && data.length != 16 && data.length != 18) {
            view.log("update prey wrong length!");
            return;
        }
        int preyId = (data[3] << 8) | data[4];
        int x = ((data[5] << 8) | data[6]) * 3 + 1;
        int y = ((data[7] << 8) | data[8]) * 3 + 1;

        synchronized (view.modelLock) {
            Prey prey = model.getPrey(preyId);
            prey.x = x;
            prey.y = y;

            switch (data.length) {
                case 11 -> prey.speed = ((data[9] << 8) | data[10]) / 1000.0;
                case 12 -> prey.ang = ((data[9] << 16) | (data[10] << 8) | data[11]) * PI2 / ANGLE_CONSTANT;
                case 13 -> {
                    prey.dir = data[9] - 48;
                    prey.wang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / ANGLE_CONSTANT;
                }
                case 14 -> {
                    prey.ang = ((data[9] << 16) | (data[10] << 8) | data[11]) * PI2 / ANGLE_CONSTANT;
                    prey.speed = ((data[12] << 8) | data[13]) / 1000.0;
                }
                case 15 -> {
                    prey.dir = data[9] - 48;
                    prey.wang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / ANGLE_CONSTANT;
                    prey.speed = ((data[13] << 8) | data[14]) / 1000.0;
                }
                case 16 -> {
                    prey.dir = data[9] - 48;
                    prey.ang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / ANGLE_CONSTANT;
                    prey.wang = ((data[13] << 16) | (data[14] << 8) | data[15]) * PI2 / ANGLE_CONSTANT;
                }
                case 18 -> {
                    prey.dir = data[9] - 48;
                    prey.ang = ((data[10] << 16) | (data[11] << 8) | data[12]) * PI2 / ANGLE_CONSTANT;
                    prey.wang = ((data[13] << 16) | (data[14] << 8) | data[15]) * PI2 / ANGLE_CONSTANT;
                    prey.speed = ((data[16] << 8) | data[17]) / 1000.0;
                }
            }
        }
    }

    private void processRemoveFood(int[] data) {
        if (data.length != 7 && data.length != 9) {
            view.log("remove food wrong length!");
            return;
        }

        int x = (data[3] << 8) | data[4];
        int y = (data[5] << 8) | data[6];

        model.removeFood(x, y);
    }

    private void processAddFood(int[] data, char cmd) {
        boolean isAllowMultipleEntries = cmd == 'F';
        boolean isFastSpawn = cmd != 'f';

        if ((!isAllowMultipleEntries && data.length != 9)
                || (isAllowMultipleEntries && data.length < 9)
                || ((data.length - 9) % 6 != 0)) {
            view.log("add food wrong length!");
            return;
        }

        for (int i = 8; i < data.length; i += 6) {
            int x = (data[i - 4] << 8) | data[i - 3];
            int y = (data[i - 2] << 8) | data[i - 1];
            double size = data[i] / 5.0;
            model.addFood(x, y, size, isFastSpawn);
        }
    }

    private void processUpdateMinimap(int[] data) {
        boolean[] map = new boolean[80 * 80];
        int mapPos = 0;
        for (int dataPos = 3; dataPos < data.length; dataPos++) {
            int value = data[dataPos];
            if (value >= 128) {
                value -= 128;
                mapPos += value;
                if (mapPos >= map.length) {
                    break;
                }
            } else {
                for (int i = 0; i < 7; i++) {
                    if ((value & (1 << (6 - i))) != 0) {
                        map[mapPos] = true;
                    }
                    mapPos++;
                    if (mapPos >= map.length) {
                        break;
                    }
                }
            }
        }
        view.setMap(map);    }

    private void processPong(int[] data) {
        if (data.length != 3) {
            view.log("pong wrong length!");
            return;
        }

        waitingForPong = false;
    }

    private void processGlobalHighScore(int[] data) {
        if (data.length < 10) {
            view.log("add sector wrong length!");
            return;
        }

        int bodyLength = (data[3] << 16) | (data[4] << 8) | data[5];
        double fam = ((data[6] << 16) | (data[7] << 8) | data[8]) / ANGLE_CONSTANT;

        int nameLength = data[9];
        StringBuilder name = new StringBuilder(nameLength);
        for (int i = 0; i < nameLength; i++) {
            name.append((char) data[10 + i]);
        }

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < data.length - 10 - nameLength; i++) {
            message.append((char) data[10 + nameLength + i]);
        }

        view.log("Received Highscore of the day: " + name + " (" +
                model.getSnakeLength(bodyLength, fam) + "): " + message);
    }

    private void processRemoveSector(int[] data) {
        if (data.length != 5) {
            view.log("add sector wrong length!");
            return;
        }
        int sectorX = data[3];
        int sectorY = data[4];
        model.removeSector(sectorX, sectorY);
    }

    private void processAddSector(int[] data) {
        if (data.length != 5) {
            view.log("remove sector wrong length!");
            return;
        }
        int sectorX = data[3];
        int sectorY = data[4];
        model.addSector(sectorX, sectorY);
    }

    private void processDead(int[] data) {
        if (data.length != 4) {
            view.log("dead wrong length!");
            return;
        }
        int deathReason = data[3];
        switch (deathReason) {
            case 0 -> view.log("You died.");
            case 1 -> view.log("You've achieved a new record!");
            case 2 -> view.log("Death reason 2, unknown");
            default -> view.log("invalid death reason: " + deathReason + "!");
        }
    }

    private void processLeaderboard(int[] data) {
        if (data.length < 8 + 10 * 7) {
            view.log("leaderboard wrong length!");
            return;
        }

        int ownRank = (data[4] << 8) | (data[5]);
        int playersCount = (data[6] << 8) | (data[7]);

        view.setRank(ownRank, playersCount);

        int rank = 0;
        int cursorPosition = 8;
        while (cursorPosition + 6 < data.length) {
            int bodyLength = (data[cursorPosition] << 8) | (data[cursorPosition + 1]);
            double bodyPartFam = ((data[cursorPosition + 2] << 16) | (data[cursorPosition + 3] << 8)
                    | (data[cursorPosition + 4])) / ANGLE_CONSTANT;
            int nameLength = data[cursorPosition + 6];
            StringBuilder name = new StringBuilder(nameLength);
            for (int i = 0; i < nameLength && cursorPosition + 7 + i < data.length; i++) {
                name.append((char) data[cursorPosition + 7 + i]);
            }
            rank++;
            cursorPosition += nameLength + 7;
            view.setHighScoreData(rank-1, name.toString(), model.getSnakeLength(bodyLength, bodyPartFam),
                    ownRank == rank);
        }
        view.log("leaderboard is set");
    }

    private void processRemoveSnakePart(int[] data) {
        if (data.length != 8 && data.length != 5) {
            view.log("remove snake part wrong length!");
            return;
        }
        int snakeId = (data[3] << 8) | (data[4]);
        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeId);
            if (data.length == 8) {
                snake.setFam(((data[5] << 16) | (data[6] << 8) | (data[7])) / ANGLE_CONSTANT);
            }
            snake.body.pollLast();
        }
    }

    private void processUpdateFam(int[] data) {
        if (data.length != 8) {
            view.log("update fam wrong length!");
            return;
        }
        int snakeId = (data[3] << 8) | (data[4]);
        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeId);
            snake.setFam(((data[5] << 16) | (data[6] << 8) | (data[7])) / ANGLE_CONSTANT);
            view.log("newFam " + snake.getFam());
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

        if (data.length == 8) {
            newDir = cmd == 'e' ? 1 : 2;

            newAng = getNewAngle(data[5]);
            newWang = getNewAngle(data[6]);
            newSpeed = getNewSpeed(data[7]);

        } else if (data.length == 7) {
            switch (cmd) {
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
        } else if (data.length == 6) {
            switch (cmd) {
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

        synchronized (view.modelLock) {
            Snake snake = model.getSnake(snakeId);

            if (newDir != -1) {
                snake.dir = newDir;
            }
            if (newAng != -1) {
                snake.ang = newAng;
            }
            if (newWang != -1) {
                snake.wang = newWang;
            }
            if (newSpeed != -1) {
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

            int skin = data[17];

            double x = ((data[18] << 16) | (data[19] << 8) | (data[20])) / 5.0;
            double y = ((data[21] << 16) | (data[22] << 8) | (data[23])) / 5.0;

            int nameLength = data[24];
            StringBuilder name = new StringBuilder(nameLength);
            for (int i = 0; i < nameLength; i++) {
                name.append((char) data[25 + i]);
            }

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

            model.addSnake(snakeId, name.toString(), view.SNAKES.get(skin), x, y, ang, wang, speed, fam, body);
        } else {
            view.log("add/remove snake wrong length!");
        }
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
