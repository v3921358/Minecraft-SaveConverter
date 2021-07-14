package main;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import util.FileUtil;

/**
 *
 * @author Windy
 */
public class Main {

    public static Main getInstance() {
        return SingletonHolder.INSTANCE;
    }
    private final String[] directorys = {"advancements", "playerdata", "stats"};
    private final Map<String, String> onlineUserInfoList = new HashMap();
    private final Map<String, String> offlineUserInfoList = new HashMap();

    public void startConvert(String worldName) {
        if (!canConvert(worldName)) {
            return;
        }
        loadUserInfo();
        try {
            doConvert(worldName);
        } catch (IOException ex) {
            FileUtil.printError("Main.txt", "startConvert", ex, "轉換過程中發生錯誤 worldName: " + worldName);
        }
    }

    private void loadUserInfo() {
        String content = FileUtil.read("usercache.json");
        JSONArray userList = JSONObject.parseArray(content);
        loadOnlineUserInfo(userList);
        loadOfflineUserInfo(userList);
    }

    private boolean canConvert(String worldName) {
        File worldFile = new File(worldName);
        if (!worldFile.exists() || !worldFile.canRead() || !worldFile.isDirectory()) {
            System.err.println(worldName + " 不存在/無法讀取/不是資料夾");
            return false;
        }
        StringBuilder directoryName;
        for (String directory : directorys) {
            directoryName = new StringBuilder();
            directoryName.append(worldName).append(FileUtil.getFileSeparator()).append(directory);
            File file = new File(directoryName.toString());
            if (!file.exists() || !file.canRead() || !file.isDirectory()) {
                System.err.println(directoryName.toString() + " 不存在/無法讀取/不是資料夾");
                return false;
            }
        }
        File userCacheFile = new File("usercache.json");
        if (!userCacheFile.exists() || !userCacheFile.canRead() || !userCacheFile.isFile()) {
            System.err.println("usercache.json 不存在/無法讀取/不是檔案");
            return false;
        }
        return true;
    }

    private void doConvert(String worldName) throws IOException {
        StringBuilder directoryName;
        for (String directory : directorys) {
            directoryName = new StringBuilder();
            directoryName.append(worldName).append(FileUtil.getFileSeparator()).append(directory);
            File directoryFile = new File(directoryName.toString());
            for (File file : directoryFile.listFiles()) {
                String fileExtension = FileUtil.getExtension(file.getName());
                if (fileExtension.equals("json") || fileExtension.equals("dat")) {
                    doConvertFile(file);
                }
            }
        }
    }

    private void doConvertFile(File file) throws IOException {
        String fileName = file.getName();
        String fileNameWithoutExtension = fileName.substring(0, fileName.indexOf("."));
        String userName = onlineUserInfoList.get(fileNameWithoutExtension);
        if (userName == null) {
            if (isOfflineUUID(fileNameWithoutExtension)) {
                System.out.println("[" + file.getParentFile().getName() + "] " + fileNameWithoutExtension + "已經是離線的UUID，自動跳過");
                return;
            } else {
                userName = getUserNameFromUUID(fileNameWithoutExtension);
                onlineUserInfoList.put(fileNameWithoutExtension, userName);
                System.out.println("[" + file.getParentFile().getName() + "] 線上玩家列表中沒有找到名稱為[" + fileNameWithoutExtension + "]的UUID，自動轉換: " + userName);
            }
        }
        String newUUID = offlineUserInfoList.get(userName);
        if (newUUID == null) {
            newUUID = getOfflineUUID(userName).toString();
            offlineUserInfoList.put(userName, newUUID);
            System.out.println("[" + file.getParentFile().getName() + "] 線下玩家列表中沒有找到名稱為[" + fileNameWithoutExtension + "]的UUID，自動轉換為:" + newUUID);
        }
        String fileExtension = FileUtil.getExtension(fileName);
        Path source = Paths.get(file.getCanonicalPath());
        Files.move(source, source.resolveSibling(newUUID + "." + fileExtension), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[" + file.getParentFile().getName() + "] 角色[" + userName + "] UUID已成功轉換為" + newUUID);
    }

    private void loadOnlineUserInfo(JSONArray userList) {
        userList.stream().map(obj -> (JSONObject) obj).forEachOrdered(json -> {
            String userName = json.getString("name"), uuid = json.getString("uuid");
            if (!isOfflineUUID(uuid)) {
                onlineUserInfoList.put(uuid, userName);
                System.out.println("[online] name: " + userName + " uuid: " + uuid);
            }
        });
    }

    private void loadOfflineUserInfo(JSONArray userList) {
        userList.stream().map(obj -> (JSONObject) obj).map(json -> json.getString("name")).forEachOrdered(userName -> {
            String uuid = getOfflineUUID(userName).toString();
            offlineUserInfoList.put(userName, uuid);
            System.out.println("[offline] name: " + userName + " uuid: " + uuid);
        });
    }

    private boolean isOfflineUUID(String uuid) {
        if (uuid == null || uuid.isBlank() || uuid.length() < 36) {
            return false;
        }
        return uuid.substring(14, 15).equals("3");
    }

    private String getUserNameFromUUID(String uuid) {
        return JSONObject.parseObject(postUrl("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.replace("-", ""), "")).getString("name");
    }

    private UUID getOfflineUUID(String userName) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + userName).getBytes(StandardCharsets.UTF_8));
    }

    private String postUrl(String urlData, String data) {
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlData);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (java.io.BufferedReader rd = new java.io.BufferedReader(new java.io.InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
            }
        } catch (Exception ex) {
            FileUtil.printError("Main.txt", "postUrl", ex, "伺服器連線失敗，urlData: " + urlData);
        }
        return sb.toString();
    }

    private static class SingletonHolder {

        protected static final Main INSTANCE = new Main();
    }
}
