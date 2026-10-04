package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class Enemy {
    private static final int WALK_FRAME_COUNT = 8;
    private static final SpriteAnimation NORMAL_SPRITE = SpriteAnimation.load("/project/game/Monster/Normal/Slime_1.png", WALK_FRAME_COUNT);
    private static final SpriteAnimation FAST_SPRITE = SpriteAnimation.load("/project/game/Monster/Fast/kai.png", WALK_FRAME_COUNT);
    private static final SpriteAnimation TANK_SPRITE = SpriteAnimation.load("/project/game/Monster/Tank/South-East_4.png", WALK_FRAME_COUNT);
    private static final SpriteAnimation BOSS_SPRITE = SpriteAnimation.load("/project/game/Monster/Boss/South-East_10.png", WALK_FRAME_COUNT);

    public double x, y;
    public int size, hp, maxHp, damage, scoreValue;
    public double speed;
    public EnemyType type;
    private boolean facingLeft;

    public Enemy(double x, double y, EnemyType type, int playerLevel) {
        this.x = x;
        this.y = y;
        this.type = type;

        switch (type) {
            case FAST:
                this.size = 18;
                this.hp = 1 + (playerLevel / 3);
                this.speed = 2.4;
                this.damage = 1;
                this.scoreValue = 15;
                break;
            case TANK:
                this.size = 32;
                this.hp = 5 + playerLevel;
                this.speed = 0.8;
                this.damage = 2;
                this.scoreValue = 25;
                break;
            case BOSS:
                this.size = 52;
                this.hp = 35 + (playerLevel * 5);
                this.speed = 1.0;
                this.damage = 3;
                this.scoreValue = 200;
                break;
            case NORMAL:
            default:
                this.size = 22;
                this.hp = 2 + (playerLevel / 2);
                this.speed = 1.4;
                this.damage = 1;
                this.scoreValue = 10;
                break;
        }
        this.maxHp = this.hp;
    }

    public void update(double targetX, double targetY) {
        facingLeft = targetX < x;
        double angle = Math.atan2(targetY - y, targetX - x);
        x += Math.cos(angle) * speed;
        y += Math.sin(angle) * speed;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x - size / 2, (int) y - size / 2, size, size);
    }

    public void draw(Graphics2D g, double camX, double camY) {
        int screenX = (int) (x - camX);
        int screenY = (int) (y - camY);
        BufferedImage frame = getSprite().frameAt(System.currentTimeMillis(), 110);
        if (facingLeft) {
            g.drawImage(frame, screenX + size / 2, screenY - size / 2,
                    screenX - size / 2, screenY + size / 2,
                    0, 0, frame.getWidth(), frame.getHeight(), null);
        } else {
            g.drawImage(frame, screenX - size / 2, screenY - size / 2,
                    size, size, null);
        }

        if (type == EnemyType.BOSS) {
            g.setColor(Color.RED);
            g.fillRect(screenX - 30, screenY - size / 2 - 12, 60, 6);
            g.setColor(Color.GREEN);
            g.fillRect(screenX - 30, screenY - size / 2 - 12, (int) (60 * ((double) hp / maxHp)), 6);
            g.setColor(Color.WHITE);
            g.drawRect(screenX - 30, screenY - size / 2 - 12, 60, 6);
        }
    }

    private SpriteAnimation getSprite() {
        switch (type) {
            case FAST: return FAST_SPRITE;
            case TANK: return TANK_SPRITE;
            case BOSS: return BOSS_SPRITE;
            case NORMAL:
            default: return NORMAL_SPRITE;
        }
    }
}