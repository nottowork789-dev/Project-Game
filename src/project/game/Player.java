package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Player {
    public double x, y;
    public int size = 26;
    public double speed = 4.2;
    public int hp = 100;
    public int maxHp = 100;

    public double lastDirX = 1.0;
    public double lastDirY = 0.0;
    
    public double attackCooldownMultiplier = 1.0;

    // เพิ่มตัวแปรเช็คการอมตะชั่วขณะเพื่อแก้ Bug โดนชนทีเดียวตาย
    public long lastDamageTime = 0;
    public final long invincibilityDuration = 500; // 0.5 วินาที

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        if (dx != 0 || dy != 0) {
            double len = Math.hypot(dx, dy);
            lastDirX = dx / len;
            lastDirY = dy / len;

            if (dx != 0 && dy != 0) {
                double diagSpeed = speed / Math.sqrt(2);
                x += dx * diagSpeed;
                y += dy * diagSpeed;
            } else {
                x += dx * speed;
                y += dy * speed;
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);

        // ให้ตัวละครกระพริบสีแดงเมื่ออยู่ในช่วงอมตะ
        boolean isInvincible = (System.currentTimeMillis() - lastDamageTime) < invincibilityDuration;
        if (isInvincible && (System.currentTimeMillis() / 100) % 2 == 0) {
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.GREEN);
        }

        g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        g.setColor(Color.WHITE);
        g.drawOval(screenX - size / 2, screenY - size / 2, size, size);
    }
}