package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Player {
    private static final int WALK_FRAME_COUNT = 8;
    private static final SpriteAnimation IDLE_SPRITE = SpriteAnimation.load("/project/game/Monster/Player/ass center.png", WALK_FRAME_COUNT);
    private static final SpriteAnimation WALK_SPRITE = SpriteAnimation.load("/project/game/Monster/Player/ass walk.png", WALK_FRAME_COUNT);

    public double x, y;
    public int size = 80;
    public double speed = 4.2;
    public int hp = 80;
    public int maxHp = 80;

    public double lastDirX = 1.0;
    public double lastDirY = 0.0;
    private boolean moving;
    
    public double attackCooldownMultiplier = 1.0;

    // เพิ่มตัวแปรเช็คการอมตะชั่วขณะเพื่อแก้ Bug โดนชนทีเดียวตาย
    public long lastDamageTime = 0;
    public final long invincibilityDuration = 450; // 0.45 วินาที

    public Player(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        moving = dx != 0 || dy != 0;
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
        boolean isInvincible = (System.currentTimeMillis() - lastDamageTime) < invincibilityDuration;
        long currentTime = System.currentTimeMillis();
        SpriteAnimation sprite = moving ? WALK_SPRITE : IDLE_SPRITE;
        BufferedImage frame = sprite.frameAt(currentTime, moving ? 100 : 180);
        boolean flash = isInvincible && (currentTime / 100) % 2 == 0;
        if (lastDirX < 0) {
            g.drawImage(frame, screenX + size / 2, screenY - size / 2,
                    screenX - size / 2, screenY + size / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - size / 2, screenY - size / 2,
                    size, size, null);
        }
        if (flash) {
            g.setColor(new Color(255, 0, 0, 110));
            g.fillOval(screenX - size / 2, screenY - size / 2, size, size);
        }
    }
}