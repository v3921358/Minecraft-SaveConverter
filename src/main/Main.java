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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private List<String> nonExistUUIDList = new ArrayList<>();

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
        System.out.println("轉換完成");
        if (nonExistUUIDList.isEmpty()) {
            return;
        }
        System.out.println("共有" + nonExistUUIDList.size() + "個UUID無法轉換:");
        nonExistUUIDList.forEach(uuid -> {
            System.out.println(uuid);
        });
    }

    private void loadUserInfo() {
        String content = FileUtil.read("usercache.json");
        JSONArray userList = JSONObject.parseArray(content);
        loadUserInfo(userList);
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
        String userName = offlineUserInfoList.get(fileNameWithoutExtension);

        if (!isOfflineUUID(fileNameWithoutExtension)) {
            System.out.println("[" + file.getParentFile().getName() + "] " + fileNameWithoutExtension + "已經是線上的UUID，自動跳過");
            return;
        }

        if (userName == null) {
            if (!nonExistUUIDList.contains(fileNameWithoutExtension)) {
                nonExistUUIDList.add(fileNameWithoutExtension);
                System.err.println("[" + file.getParentFile().getName() + "] 線下玩家列表中沒有找到UUID為[" + fileNameWithoutExtension + "]的人物，自動跳過");
            }
            return;
        }

        String newUUID = onlineUserInfoList.get(userName);
        if (newUUID == null) {
            newUUID = getUUIDFromUserName(userName);
            onlineUserInfoList.put(userName, newUUID);
            System.out.println("[" + file.getParentFile().getName() + "] 線上玩家列表中沒有找到名稱為[" + userName + "]的人物，自動轉換: " + newUUID);
        }
        String fileExtension = FileUtil.getExtension(fileName);
        Path source = Paths.get(file.getCanonicalPath());
        Files.move(source, source.resolveSibling(newUUID + "." + fileExtension), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[" + file.getParentFile().getName() + "] 角色[" + userName + "] UUID已成功轉換為" + newUUID);
    }

    private void loadUserInfo(JSONArray userList) {
        userList.stream().map(obj -> (JSONObject) obj).forEachOrdered(json -> {
            String userNameFromJson = json.getString("name"), uuidFromJson = json.getString("uuid");
            if (isOfflineUUID(uuidFromJson)) {
                offlineUserInfoList.put(uuidFromJson, userNameFromJson);
                System.out.println("[offline] name: " + userNameFromJson + " uuid: " + uuidFromJson);
            } else {
                onlineUserInfoList.put(userNameFromJson, uuidFromJson);
                System.out.println("[online] name: " + userNameFromJson + " uuid: " + uuidFromJson);
            }
        });
    }

    private boolean isOfflineUUID(String uuid) {
        if (uuid == null || uuid.isBlank() || uuid.length() < 36) {
            return false;
        }
        return uuid.substring(14, 15).equals("3");
    }

    private String getUUIDFromUserName(String userName) {
        String uuidWithoutSymbol = JSONObject.parseObject(postUrl("https://api.mojang.com/users/profiles/minecraft/" + userName, "")).getString("id");
        return uuidWithoutSymbol.substring(0, 8)
                + "-" + uuidWithoutSymbol.substring(8, 12)
                + "-" + uuidWithoutSymbol.substring(12, 16)
                + "-" + uuidWithoutSymbol.substring(16, 20)
                + "-" + uuidWithoutSymbol.substring(20, uuidWithoutSymbol.length());
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
