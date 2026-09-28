package project.game;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VampireSurvivorsGame extends JPanel implements Runnable, KeyListener {

    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final int TILE_SIZE = 100;

    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;
    private final long TARGET_TIME = 1000 / FPS;

    private BufferedImage dbImage;
    private Graphics2D dbg;

    private final Set<Integer> activeKeys = new HashSet<>();

    private double cameraX = 0;
    private double cameraY = 0;

    private Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Gem> gems = new ArrayList<>();
    private final List<VisualEffect> visualEffects = new ArrayList<>();
    private final List<Weapon> weapons = new ArrayList<>();
    private final List<PassiveItem> passiveItems = new ArrayList<>();

    private final LevelManager levelManager = new LevelManager();

    private long gameStartTime;
    private long elapsedSeconds = 0;
    private long lastBossSpawnMinute = 0;
    private long lastSpawnTime = 0;
    private long spawnInterval = 700;

    private boolean gameOver = false;
    private int score = 0;

    public VampireSurvivorsGame() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setFocusable(true);
        requestFocus();
        addKeyListener(this);
        initGame();
    }

    private void initGame() {
        player = new Player(0, 0);
        enemies.clear();
        gems.clear();
        visualEffects.clear();
        weapons.clear();
        passiveItems.clear();
        activeKeys.clear();

        levelManager.reset();

        weapons.add(new SwordWeapon(1));
        weapons.add(new GarlicWeapon(0));
        weapons.add(new LaserWeapon(0));
        weapons.add(new GunWeapon(0));
        weapons.add(new FireStaffWeapon(0));
        weapons.add(new SpearWeapon(0));
        weapons.add(new HolyWaterWeapon(0));
        weapons.add(new ShieldWeapon(0));

        passiveItems.add(new PassiveItem(PassiveItem.ItemType.ARMOR));
        passiveItems.add(new PassiveItem(PassiveItem.ItemType.BOOTS));
        passiveItems.add(new PassiveItem(PassiveItem.ItemType.GLOVES));

        gameOver = false;
        score = 0;
        spawnInterval = 700;

        gameStartTime = System.currentTimeMillis();
        elapsedSeconds = 0;
        lastBossSpawnMinute = 0;
    }

    public void startGameThread() {
        if (gameThread == null || !running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        long start, elapsed, wait;

        while (running) {
            start = System.currentTimeMillis();

            updateGame();
            renderToBuffer();
            drawToScreen();

            elapsed = System.currentTimeMillis() - start;
            wait = TARGET_TIME - elapsed;

            if (wait > 0) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateGame() {
        if (gameOver || levelManager.isLevelUping()) return;

        elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;

        int dx = 0, dy = 0;
        if (activeKeys.contains(KeyEvent.VK_W) || activeKeys.contains(KeyEvent.VK_UP)) dy -= 1;
        if (activeKeys.contains(KeyEvent.VK_S) || activeKeys.contains(KeyEvent.VK_DOWN)) dy += 1;
        if (activeKeys.contains(KeyEvent.VK_A) || activeKeys.contains(KeyEvent.VK_LEFT)) dx -= 1;
        if (activeKeys.contains(KeyEvent.VK_D) || activeKeys.contains(KeyEvent.VK_RIGHT)) dx += 1;

        player.move(dx, dy);

        cameraX = player.x - (SCREEN_WIDTH / 2.0);
        cameraY = player.y - (SCREEN_HEIGHT / 2.0);

        long currentTime = System.currentTimeMillis();
        for (Weapon weapon : weapons) {
            weapon.update(player, enemies, visualEffects, currentTime);
        }

        if (currentTime - lastSpawnTime >= spawnInterval) {
            spawnEnemyAroundPlayer();
            lastSpawnTime = currentTime;
        }

        long currentMinute = elapsedSeconds / 60;
        if (currentMinute > lastBossSpawnMinute) {
            spawnBossAroundPlayer(currentMinute);
            lastBossSpawnMinute = currentMinute;
        }

        Iterator<VisualEffect> vIt = visualEffects.iterator();
        while (vIt.hasNext()) {
            VisualEffect ve = vIt.next();
            ve.update();
            if (ve.isFinished()) vIt.remove();
        }

        Iterator<Enemy> eIt = enemies.iterator();
        while (eIt.hasNext()) {
            Enemy enemy = eIt.next();
            enemy.update(player.x, player.y);

            if (enemy.hp <= 0) {
                int gemXp = (enemy.type == EnemyType.BOSS) ? 50 : 5;
                gems.add(new Gem(enemy.x, enemy.y, gemXp, enemy.type == EnemyType.BOSS));
                score += enemy.scoreValue;
                eIt.remove();
                continue;
            }

            if (enemy.type != EnemyType.BOSS && Math.hypot(enemy.x - player.x, enemy.y - player.y) > 1200) {
                eIt.remove();
                continue;
            }

            // แก้ไขการรับดาเมจ ให้เช็ค Invincibility Frames
            if (enemy.getBounds().intersects(player.getBounds())) {
                long now = System.currentTimeMillis();
                if (now - player.lastDamageTime >= player.invincibilityDuration) {
                    player.hp -= enemy.damage;
                    player.lastDamageTime = now;
                    if (player.hp <= 0) {
                        player.hp = 0;
                        gameOver = true;
                    }
                }
            }
        }

        Iterator<Gem> gIt = gems.iterator();
        while (gIt.hasNext()) {
            Gem gem = gIt.next();
            double dist = Math.hypot(player.x - gem.x, player.y - gem.y);

            if (dist < 150) {
                gem.x += (player.x - gem.x) * 0.12;
                gem.y += (player.y - gem.y) * 0.12;
            }

            if (gem.getBounds().intersects(player.getBounds())) {
                levelManager.gainXP(gem.xpValue, player, weapons, passiveItems);
                if (levelManager.isLevelUping()) {
                    activeKeys.clear();
                }
                gIt.remove();
            }
        }
    }

    private void spawnEnemyAroundPlayer() {
        double spawnDistance = 500 + Math.random() * 100;
        double angle = Math.random() * Math.PI * 2;

        double ex = player.x + Math.cos(angle) * spawnDistance;
        double ey = player.y + Math.sin(angle) * spawnDistance;

        double rand = Math.random();
        EnemyType type;
        if (rand < 0.60) type = EnemyType.NORMAL;
        else if (rand < 0.85) type = EnemyType.FAST;
        else type = EnemyType.TANK;

        enemies.add(new Enemy(ex, ey, type, levelManager.getLevel()));
    }

    private void spawnBossAroundPlayer(long bossWave) {
        double spawnDistance = 450;
        double angle = Math.random() * Math.PI * 2;

        double ex = player.x + Math.cos(angle) * spawnDistance;
        double ey = player.y + Math.sin(angle) * spawnDistance;

        Enemy boss = new Enemy(ex, ey, EnemyType.BOSS, levelManager.getLevel());
        boss.hp += (int) (bossWave * 30);
        boss.maxHp = boss.hp;
        enemies.add(boss);
    }

    private void renderToBuffer() {
        if (dbImage == null) {
            dbImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            dbg = dbImage.createGraphics();
            dbg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        drawInfiniteMap(dbg);

        for (Gem gem : gems) gem.draw(dbg, cameraX, cameraY);
        for (Enemy enemy : enemies) enemy.draw(dbg, cameraX, cameraY);
        for (Weapon weapon : weapons) weapon.draw(dbg, player, cameraX, cameraY);
        for (VisualEffect ve : visualEffects) ve.draw(dbg, cameraX, cameraY);

        player.draw(dbg, cameraX, cameraY);
        drawUI(dbg);
    }

    private void drawInfiniteMap(Graphics2D g) {
        g.setColor(new Color(25, 28, 32));
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int startX = (int) Math.floor(cameraX / TILE_SIZE) * TILE_SIZE;
        int startY = (int) Math.floor(cameraY / TILE_SIZE) * TILE_SIZE;

        g.setColor(new Color(40, 45, 52));
        for (int x = startX; x < cameraX + SCREEN_WIDTH + TILE_SIZE; x += TILE_SIZE) {
            for (int y = startY; y < cameraY + SCREEN_HEIGHT + TILE_SIZE; y += TILE_SIZE) {
                int screenX = (int) (x - cameraX);
                int screenY = (int) (y - cameraY);

                g.drawRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
            }
        }
    }

    private void drawUI(Graphics2D g) {
        g.setColor(Color.RED);
        g.fillRect(20, 20, 200, 15);
        g.setColor(Color.GREEN);
        g.fillRect(20, 20, (int) (200 * ((double) player.hp / player.maxHp)), 15);
        g.setColor(Color.WHITE);
        g.drawRect(20, 20, 200, 15);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("HP: " + player.hp + "/" + player.maxHp, 25, 32);

        levelManager.drawXPBar(g, 20, 42, 200, 10);

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Level: " + levelManager.getLevel(), 230, 32);
        g.drawString("Score: " + score, 230, 50);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(Color.LIGHT_GRAY);
        StringBuilder itemSummary = new StringBuilder();
        for (Weapon w : weapons) {
            if (w.getLevel() > 0) itemSummary.append(w.getName()).append(": Lv.").append(w.getLevel()).append(" | ");
        }
        for (PassiveItem p : passiveItems) {
            if (p.getLevel() > 0) itemSummary.append(p.getName()).append(": Lv.").append(p.getLevel()).append(" | ");
        }
        g.drawString(itemSummary.toString(), 20, 72);

        long minutes = elapsedSeconds / 60;
        long seconds = elapsedSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(Color.YELLOW);
        g.drawString(timeStr, SCREEN_WIDTH / 2 - 30, 35);

        levelManager.drawLevelUpUI(g, SCREEN_WIDTH, SCREEN_HEIGHT);

        if (gameOver) {
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            g.setColor(Color.RED);
            g.setFont(new Font("SansSerif", Font.BOLD, 48));
            g.drawString("GAME OVER", SCREEN_WIDTH / 2 - 140, SCREEN_HEIGHT / 2 - 20);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g.drawString("Survived Time: " + timeStr, SCREEN_WIDTH / 2 - 95, SCREEN_HEIGHT / 2 + 20);
            g.drawString("Press 'R' to Restart", SCREEN_WIDTH / 2 - 85, SCREEN_HEIGHT / 2 + 55);
        }
    }

    private void drawToScreen() {
        Graphics g = getGraphics();
        if (g != null && dbImage != null) {
            g.drawImage(dbImage, 0, 0, null);
            g.dispose();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (levelManager.handleKeyPress(e, player)) {
            return;
        }

        activeKeys.add(e.getKeyCode());

        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
            initGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        activeKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}