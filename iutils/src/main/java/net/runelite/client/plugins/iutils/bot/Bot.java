package net.runelite.client.plugins.iutils.bot;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.iutils.CalculationUtils;
import net.runelite.client.plugins.iutils.KeyboardUtils;
import net.runelite.client.plugins.iutils.WalkUtils;
import net.runelite.client.plugins.iutils.actor.NpcStream;
import net.runelite.client.plugins.iutils.actor.PlayerStream;
import net.runelite.client.plugins.iutils.api.EquipmentSlot;
import net.runelite.client.plugins.iutils.iUtils;
import net.runelite.client.plugins.iutils.scene.GameObjectStream;
import net.runelite.client.plugins.iutils.scene.GroundItemStream;
import net.runelite.client.plugins.iutils.scene.ObjectCategory;
import net.runelite.client.plugins.iutils.scene.Position;
import net.runelite.client.plugins.iutils.ui.EquipmentItemStream;
import net.runelite.client.plugins.iutils.ui.InventoryItemStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.event.KeyEvent.VK_ENTER;
import static net.runelite.client.plugins.iutils.iUtils.sleep;

@Slf4j
@Singleton
public class Bot {

    @Inject
    public Client client;

    @Inject
    public ClientThread clientThread;

    @Inject
    public iUtils utils;

    @Inject
    public WalkUtils walkUtils;

    @Inject
    private CalculationUtils calc;

    @Inject
    private KeyboardUtils keyboard;

    @Inject
    private ExecutorService executorService;

    private boolean tickEvent;

    iTile[][][] tiles = new iTile[4][104][104];
    Position base;

    public Client client() {
        return client;
    }

    public ClientThread clientThread() {
        return clientThread;
    }

    public <T> T getFromClientThread(Supplier<T> supplier) {
        if (!client.isClientThread()) {
            CompletableFuture<T> future = new CompletableFuture<>();

            clientThread().invoke(() -> {
                future.complete(supplier.get());
            });
            return future.join();
        } else {
            return supplier.get();
        }
    }

//    public void onGameTick(GameTick event) {
//        log.info("Game tick {}", System.currentTimeMillis());
//    }

    public void tick(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick();
        }
    }

    public void tick() {
        long start = client().getTickCount();

        while (client.getTickCount() == start) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public iPlayer localPlayer() {
        return new iPlayer(this, client.getLocalPlayer(), client.getLocalPlayer().getPlayerComposition());
    }

    public Position base() {
        return new Position(client.getBaseX(), client.getBaseY(), client.getPlane());
    }

    public iTile tile(Position position) {
        int plane = position.z;
        int x = position.x - client.getBaseX();
        int y = position.y - client.getBaseY();
        if (plane < 0 || plane >= 4) {
            return null;
        }
        if (x < 0 || x >= 104) {
            return null;
        }
        if (y < 0 || y >= 104) {
            return null;
        }
        Tile tile = client.getScene().getTiles()[plane][x][y];
        return new iTile(this, tile);
    }

    public Stream<iTile> tiles() {
        return Arrays.stream(client().getScene().getTiles())
                .flatMap(Arrays::stream)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .map(to -> new iTile(this, to));
    }

    public GameObjectStream objects() {
        return getFromClientThread(() -> new GameObjectStream(iUtils.objects.stream()
                .map(o -> new iObject(
                        this,
                        o,
                        client().getObjectDefinition(o.getId())
                ))
                .collect(Collectors.toList())
                .stream())
        );
    }

//    public GameObjectStream objects() {
//        Collection<BaseObject> baseObjects = new ArrayList<>();
//        Tile[][][] tiles = client().getScene().getTiles();
//        int plane = client().getPlane();
//
//        for (int j = 0; j < tiles[plane].length; j++) {
//            for (int k = 0; k < tiles[plane][j].length; k++) {
//                GameObject[] go = tiles[plane][j][k].getGameObjects();
//                for (GameObject gameObject : go) {
//                    if (gameObject != null) {
//                        baseObjects.add(new BaseObject(gameObject, ObjectCategory.REGULAR));
//                    }
//                }
//                WallObject wallObject = tiles[plane][j][k].getWallObject();
//                if (wallObject != null) {
//                    baseObjects.add(new BaseObject(wallObject, ObjectCategory.WALL));
//                }
//
//                GroundObject groundObject = tiles[plane][j][k].getGroundObject();
//                if (groundObject != null) {
//                    baseObjects.add(new BaseObject(groundObject, ObjectCategory.FLOOR_DECORATION));
//                }
//
//                DecorativeObject decorativeObject = tiles[plane][j][k].getDecorativeObject();
//                if (decorativeObject != null) {
//                    baseObjects.add(new BaseObject(decorativeObject, ObjectCategory.WALL_DECORATION));
//                }
//            }
//        }
//        return getFromClientThread(() -> new GameObjectStream(baseObjects.stream()
//                .map(o -> new iObject(
//                        this,
//                        o.tileObject,
//                        o.objectCategory(),
//                        client().getObjectDefinition(o.tileObject.getId())
//                ))
//                .collect(Collectors.toList())
//                .stream())
//        );
//    }

    public GroundItemStream groundItems() {
        return getFromClientThread(() -> new GroundItemStream(iUtils.tileItems.stream()
                .map(o -> new iGroundItem(
                        this,
                        o,
                        client().getObjectDefinition(o.getId())
                ))
                .collect(Collectors.toList())
                .stream())
        );
    }

    public NpcStream npcs() {
        return getFromClientThread(() -> new NpcStream(client().getNpcs().stream()
                .map(npc -> new iNPC(this, npc, client().getNpcDefinition(npc.getId())))
                .collect(Collectors.toList())
                .stream())
        );
    }

    public PlayerStream players() {
        return getFromClientThread(() -> new PlayerStream(client().getPlayers().stream()
                .map(p -> new iPlayer(this, p, p.getPlayerComposition()))
                .collect(Collectors.toList())
                .stream())
        );
    }

    public iWidget widget(int group, int file) {
        return getFromClientThread(() -> new iWidget(this, client.getWidget(group, file)));
    }

    public iWidget widget(int group, int file, int child) {
//        Widget widget = Objects.requireNonNull(client.getWidget(group, file)).getDynamicChildren()[child];
        return getFromClientThread(() ->
            new iWidget(this, client.getWidget(group, file).getDynamicChildren()[child]));
    }

    public iWidget widget(WidgetInfo widgetInfo) {
        return getFromClientThread(() -> new iWidget(this, client.getWidget(widgetInfo)));
    }

    public InventoryItemStream inventory() {
        return getFromClientThread(() -> new InventoryItemStream(widget(WidgetInfo.INVENTORY).getWidgetItems().stream()
                .map(wi -> new InventoryItem(this, wi, client().getItemDefinition(wi.getId())))
                .collect(Collectors.toList())
                .stream())
        );
    }

    public EquipmentSlot equipmentSlot(int index) {
        for (var slot : EquipmentSlot.values()) {
            if (slot.index == index) {
                return slot;
            }
        }
        return null;
    }

    public EquipmentItemStream equipment() {
        Map<Item, EquipmentSlot> equipped = new HashMap();
        Item[] items = client.getItemContainer(InventoryID.EQUIPMENT).getItems();
        for (int i = 0; i <= items.length -1; i++)
        {
            if (items[i].getId() == -1 || items[i].getId() == 0)
            {
                continue;
            }
            equipped.put(items[i], equipmentSlot(i));
        }
        return getFromClientThread(() -> new EquipmentItemStream(equipped.entrySet().stream()
                .map(i -> new EquipmentItem(this, i.getKey(), client().getItemDefinition(i.getKey().getId()), i.getValue()))
                .collect(Collectors.toList())
                .stream())
                .filter(Objects::nonNull)
        );
    }

    public ItemContainer container(InventoryID inventoryID) {
        return client.getItemContainer(inventoryID);
    }

    public ItemContainer container(int containerId) {
        InventoryID inventoryID = InventoryID.getValue(containerId);
        return client.getItemContainer(inventoryID);
    }

    public void chooseNumber(int number) {
            keyboard.typeString(String.valueOf(number));
            sleep(calc.getRandomIntBetweenRange(80, 250));
            keyboard.pressKey(VK_ENTER);
    }

    /**
     * Sends an item choice to the server.
     */
    public void chooseItem(int item) {
        clientThread.invoke(() -> client.runScript(754, item, 84));
    }


    /**
     * The widget which contains all screens (bank, grand exchange, trade, etc.)
     */
    public iWidget screenContainer() {
        return client.isResized() ? widget(164, 15) : widget(548, 23); //Modern or fixed TODO support classic resizable
    }

    ///////////////////////////////////////////////////
    //                  Variables                    //
    ///////////////////////////////////////////////////

    public int varb(int id) {
        return getFromClientThread(() -> client.getVarbitValue(id));
    }

    public int varp(int id) {
        return getFromClientThread(() -> client.getVarpValue(id));
    }

    public int energy() { return client.getEnergy(); }

    public int experience(Skill skill) {
        return client.getSkillExperience(skill);
    }

    public int modifiedLevel(Skill skill) {
        return client.getBoostedSkillLevel(skill);
    }

    public int baseLevel(Skill skill) {
        return client.getRealSkillLevel(skill);
    }

    public GrandExchangeOffer grandExchangeOffer(int slot) {
        return client.getGrandExchangeOffers()[slot];
    }

    ///////////////////////////////////////////////////
    //                    Other                      //
    ///////////////////////////////////////////////////

    public void sleepApproximately(int averageTime) { //TODO
        sleepExact(calc.randomDelay(true, (int) (averageTime * 0.7), (int) (averageTime * 1.3), 50, averageTime));
    }

    public void sleepExact(long time) {
        log.info("Performing sleep for: {}ms", time);
        long endTime = System.currentTimeMillis() + time;

//        while (endTime > lastTickTime + 600) {
//            tick();
//        }

        time = endTime - System.currentTimeMillis();

        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //
    public void waitUntil(BooleanSupplier condition) {
        long start = System.currentTimeMillis();

        while (!condition.getAsBoolean()) {
            tick();

            if (System.currentTimeMillis() - start > 60000) {
                throw new IllegalStateException("timed out");
            }
        }

        int timeWaited = (int) (System.currentTimeMillis() - start);

        if (timeWaited > 200) {
            sleepExact((timeWaited / 10));
        }
    }

    public boolean waitUntil(BooleanSupplier condition, int timeout) {
        long start = System.currentTimeMillis();

        while (!condition.getAsBoolean()) {
            tick();

            if (System.currentTimeMillis() - start > timeout) {
                return false;
            }
        }

        int timeWaited = (int) (System.currentTimeMillis() - start);

        if (timeWaited > 200) {
            sleepExact((timeWaited / 10));
        }

        return true;
    }

    public static class BaseObject {
        private final TileObject tileObject;
        private final ObjectCategory objectCategory;

        public BaseObject(TileObject tileObject, ObjectCategory objectCategory) {
            this.tileObject = tileObject;
            this.objectCategory = objectCategory;
        }

        public TileObject tileObject() {
            return tileObject;
        }

        public ObjectCategory objectCategory() {
            return objectCategory;
        }
    }
}
