package main;

import java.util.Scanner;

/**
 *
 * @author Windy
 */
public class Entry {

    public static void main(String[] args) {
        System.out.println("歡迎使用創世神UUID轉換系統");
        System.out.println("本系統將需要usercache.json讀取目前玩家資料");
        System.out.println("讀取完成後將會線下UUID轉換為線上");
        Scanner scanner = new Scanner(System.in);
        System.out.println("請輸入轉換的存檔名稱");
        String worldName = scanner.nextLine();
        System.out.println("存檔名稱: " + worldName);
        System.out.println("即將開始轉換");
        Main.getInstance().startConvert(worldName);
    }
}
