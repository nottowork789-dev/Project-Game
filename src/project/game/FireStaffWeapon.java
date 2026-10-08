package project.game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FireStaffWeapon extends Weapon {
    private long lastAttackTime = 0;
    private final long cooldown = 1800;
    private final List<Fireball> fireballs = new ArrayList<>();

    public FireStaffWeapon(int level) {
        super("Fire Staff", "Launches a fireball at nearest enemy that explodes on hit", level);
    }

    private static class Fireball {
        double x, y, dirX, dirY;
        double speed = 7.0;
        int damage = 1;
        int explosionRadius = 70;

        Fireball(double x, double y, double dirX, double dirY, int damage, int radius) {
            this.x = x;
            this.y = y;
            this.dirX = dirX;
            this.dirY = dirY;
            this.damage = damage;
            this.explosionRadius = radius;
        }
    }

    @Override
    public void update(Player player, List<Enemy> enemies, List<VisualEffect> visualEffects, long currentTime) {
        if (level <= 0) return;

        long effectiveCooldown = (long) (cooldown * player.attackCooldownMultiplier);

        if (currentTime - lastAttackTime >= effectiveCooldown) {
            Enemy target = findClosestEnemy(player, enemies);
            if (target != null) {
                double dx = target.x - player.x;
                double dy = target.y - player.y;
                double len = Math.hypot(dx, dy);

                if (len > 0 && len <= 500) {
                    int dmg = 15 + (level * 7);
                    int radius = 44 + (level * 7);
                    fireballs.add(new Fireball(player.x, player.y, dx / len, dy / len, dmg, radius));
                }
            }
            lastAttackTime = currentTime;
        }

        Iterator<Fireball> fIt = fireballs.iterator();
        while (fIt.hasNext()) {
            Fireball f = fIt.next();
            f.x += f.dirX * f.speed;
            f.y += f.dirY * f.speed;

            boolean exploded = false;
            for (Enemy enemy : enemies) {
                if (Math.hypot(enemy.x - f.x, enemy.y - f.y) < enemy.size / 2.0 + 8) {
                    exploded = true;
                    break;
                }
            }

            if (exploded) {
                // ทำดาเมจเป็นวงระเบิด
                for (Enemy enemy : enemies) {
                    if (Math.hypot(enemy.x - f.x, enemy.y - f.y) <= f.explosionRadius) {
                        enemy.hp -= f.damage;
                    }
                }
                // เอฟเฟกต์วงกลมระเบิด
                visualEffects.add(new VisualEffect(f.x, f.y, f.explosionRadius, Color.ORANGE, 15));
                fIt.remove();
            } else if (Math.hypot(f.x - player.x, f.y - player.y) > 900) {
                fIt.remove();
            }
        }
    }

    @Override
    public void draw(Graphics2D g, Player player, double camX, double camY) {
        g.setColor(Color.RED);
        for (Fireball f : fireballs) {
            int sx = (int) (f.x - camX);
            int sy = (int) (f.y - camY);
            g.fillOval(sx - 8, sy - 8, 16, 16);
        }
    }
}