package project.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LevelManager {
    private int level = 1;
    private int xp = 0;
    private int xpToNextLevel = 10;
    private boolean isLevelUping = false;

    // เก็บรายการตัวเลือก 3 ชิ้นที่สุ่มได้ในรอบนั้นๆ
    private final List<Object> offeredUpgrades = new ArrayList<>();

    public LevelManager() {
        reset();
    }

    public void reset() {
        this.level = 1;
        this.xp = 0;
        this.xpToNextLevel = 10;
        this.isLevelUping = false;
        this.offeredUpgrades.clear();
    }

    public void gainXP(int amount, Player player, List<Weapon> weapons, List<PassiveItem> passiveItems) {
        xp += amount;
        if (xp >= xpToNextLevel) {
            level++;
            xp -= xpToNextLevel;
            xpToNextLevel = (int) (xpToNextLevel * 1.5);

            if (player != null) {
                player.hp = Math.min(player.hp + 20, player.maxHp);
            }

            // สุ่มของ 3 ชิ้นขึ้นมาเสนอให้ผู้เล่น
            generateUpgradeOptions(weapons, passiveItems);
            isLevelUping = true;
        }
    }

    // ฟังก์ชันสุ่มตัวเลือก 3 ชิ้น
    private void generateUpgradeOptions(List<Weapon> weapons, List<PassiveItem> passiveItems) {
        offeredUpgrades.clear();
        List<Object> pool = new ArrayList<>();

        // กรองเอาเฉพาะอาวุธที่ยังอัปเกรดไม่เต็ม
        for (Weapon w : weapons) {
            if (w.getLevel() < 5) { // สมมติ max level อาวุธคือ 5
                pool.add(w);
            }
        }

        // กรองเอาเฉพาะ PassiveItem ที่ยังอัปเกรดไม่เต็ม
        for (PassiveItem p : passiveItems) {
            if (p.getLevel() < p.getMaxLevel()) {
                pool.add(p);
            }
        }

        // สุ่มสลับรายการใน pool
        Collections.shuffle(pool);

        // ดึงมา 3 ชิ้น (หรือเท่าที่มีหากเหลือไม่ถึง 3)
        int count = Math.min(3, pool.size());
        for (int i = 0; i < count; i++) {
            offeredUpgrades.add(pool.get(i));
        }
    }

    public boolean handleKeyPress(KeyEvent e, Player player) {
        if (!isLevelUping) return false;

        if (e.getKeyCode() == KeyEvent.VK_1) selectUpgrade(0, player);
        else if (e.getKeyCode() == KeyEvent.VK_2) selectUpgrade(1, player);
        else if (e.getKeyCode() == KeyEvent.VK_3) selectUpgrade(2, player);

        return true;
    }

    private void selectUpgrade(int index, Player player) {
        if (index >= 0 && index < offeredUpgrades.size()) {
            Object selected = offeredUpgrades.get(index);

            if (selected instanceof Weapon) {
                ((Weapon) selected).levelUp();
            } else if (selected instanceof PassiveItem) {
                ((PassiveItem) selected).levelUp(player);
            }

            isLevelUping = false;
            offeredUpgrades.clear();
        }
    }

    public void drawLevelUpUI(Graphics2D g, int screenWidth, int screenHeight) {
        if (!isLevelUping) return;

        // ฉากหลังดำโปร่งแสง
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, screenWidth, screenHeight);

        // หัวข้อ Level Up
        g.setColor(Color.YELLOW);
        g.setFont(new Font("SansSerif", Font.BOLD, 36));
        g.drawString("LEVEL UP!", screenWidth / 2 - 100, 120);

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(Color.WHITE);
        g.drawString("Choose 1 Item to Upgrade:", screenWidth / 2 - 110, 160);

        // แสดงผลตัวเลือก 3 ชิ้น
        for (int i = 0; i < offeredUpgrades.size(); i++) {
            Object upgrade = offeredUpgrades.get(i);
            int boxY = 200 + (i * 90);

            String name = "";
            String desc = "";
            int currentLv = 0;
            boolean isWeapon = (upgrade instanceof Weapon);

            if (isWeapon) {
                Weapon w = (Weapon) upgrade;
                name = w.getName();
                desc = w.getDescription();
                currentLv = w.getLevel();
            } else {
                PassiveItem p = (PassiveItem) upgrade;
                name = p.getName();
                desc = p.getDescription();
                currentLv = p.getLevel();
            }

            // กรอบตัวเลือก
            g.setColor(new Color(35, 39, 46));
            g.fillRect(screenWidth / 2 - 210, boxY, 420, 75);
            g.setColor(isWeapon ? Color.CYAN : Color.GREEN); // อาวุธกรอบฟ้า / Item กรอบเขียว
            g.drawRect(screenWidth / 2 - 210, boxY, 420, 75);

            // ชื่อและเลเวล
            g.setFont(new Font("SansSerif", Font.BOLD, 18));
            g.drawString("[" + (i + 1) + "] " + name + " (Lv." + (currentLv + 1) + ")", screenWidth / 2 - 190, boxY + 30);

            // คำอธิบาย
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(desc, screenWidth / 2 - 190, boxY + 55);
        }
    }

    public void drawXPBar(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x, y, width, height);
        g.setColor(Color.CYAN);
        g.fillRect(x, y, (int) (width * ((double) xp / xpToNextLevel)), height);
        g.setColor(Color.WHITE);
        g.drawRect(x, y, width, height);
    }

    public int getLevel() { return level; }
    public boolean isLevelUping() { return isLevelUping; }
}