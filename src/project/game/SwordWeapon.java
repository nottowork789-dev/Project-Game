package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class SwordWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long baseCooldown = 1000; // คูลดาวน์พื้นฐาน 1 วินาที
    private boolean isAttacking = false;
    private long attackStartTime = 0;
    private final long attackDuration = 150; // ระยะเวลาแสดงเอฟเฟกต์ 0.15 วินาที
    private double attackRadius = 90;

    public SwordWeapon(int level) {
        super("Sword", "Slashes in an arc in front of the player", level);
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        long effectiveCooldown = (long) (baseCooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            attackRadius = 80 + (level * 15);
            int damage = 25 + (level * 12);

            // ทิศทางที่ผู้เล่นกำลังหันไป (Normalized Vector)
            double dirX = player.lastDirX;
            double dirY = player.lastDirY;

            for (Enemy enemy : enemies) {
                // Vector จากผู้เล่นไปหาศัตรู
                double ex = enemy.x - player.x;
                double ey = enemy.y - player.y;
                double dist = Math.hypot(ex, ey);

                // เช็คระยะทางว่าอยู่ในระยะฟันหรือไม่
                if (dist <= attackRadius + (enemy.size / 2.0) && dist > 0) {
                    // คำนวณ Dot Product เพื่อดูว่าศัตรูอยู่ด้านหน้าหรือไม่
                    // dot = (ex/dist)*dirX + (ey/dist)*dirY
                    double dot = (ex / dist) * dirX + (ey / dist) * dirY;

                    // cos(60 องศา) = 0.5 -> dot >= 0.5 คือครอบคลุมมุม 120 องศาด้านหน้า
                    if (dot >= 0.5) {
                        enemy.hp -= damage;
                    }
                }
            }

            isAttacking = true;
            attackStartTime = currentTime;
            lastAttackTime = currentTime;
        }

        if (isAttacking && currentTime - attackStartTime > attackDuration) {
            isAttacking = false;
        }
    }

  @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        if (level <= 0 || !isAttacking) return;

        int sx = (int) (player.x - camX);
        int sy = (int) (player.y - camY);
        int r = (int) attackRadius;

        // คำนวณมุมหลักจากทิศทางที่ผู้เล่นหันไป (แปลงจาก Vector เป็น องศา)
        double facingAngle = Math.atan2(player.lastDirY, player.lastDirX);
        double facingDegrees = Math.toDegrees(facingAngle);

        // คำนวณมุมเริ่มต้นและมุมกว้าง (120 องศา)
        int startAngle = (int) (-facingDegrees - 60);
        int arcAngle = 120;

        // 1. วาดเนื้อเอฟเฟกต์ฟันแบบเต็มพื้นที่ (โปร่งแสง)
        g.setColor(new Color(255, 255, 200, 100)); // สีเหลืองนวลโปร่งแสง
        g.fillArc(sx - r, sy - r, r * 2, r * 2, startAngle, arcAngle);

        // 2. วาดเส้นขอบส่วนโค้งด้านนอกเพื่อให้เอฟเฟกต์ดูคมชัดขึ้น
        g.setColor(new Color(255, 255, 255, 220)); // สีขาวสว่าง
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawArc(sx - r, sy - r, r * 2, r * 2, startAngle, arcAngle);

        // คืนค่าความหนาเส้นมาตรฐาน
        g.setStroke(new java.awt.BasicStroke(1));
    }
}