package net.runelite.client.plugins.iutils.bot;

import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.iutils.api.Interactable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InventoryItem implements Interactable, Useable {
    private final Bot bot;
    private final WidgetItem widgetItem;
    private final ItemComposition definition;

    public InventoryItem(Bot bot, WidgetItem widgetItem, ItemComposition definition) {
        this.bot = bot;
        this.widgetItem = widgetItem;
        this.definition = definition;
    }

    public Bot bot() {
        return bot;
    }

    public int id() {
        return widgetItem.getId();
    }

    public String name() {
        return definition.getName();
    }

    public int quantity() {
        return widgetItem.getQuantity();
    }

    public int slot() {
        return widgetItem.getIndex();
    }

    public ItemComposition definition() {
        return definition;
    }

    @Override
    public List<String> actions() {
        return Arrays.stream(definition.getInventoryActions()).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public void interact(String action) {
        String[] actions = definition.getInventoryActions();

        for (int i = 0; i < actions.length; i++) {
            if (action.equalsIgnoreCase(actions[i])) {
                interact(i);
                return;
            }
        }

        throw new IllegalArgumentException("no action \"" + action + "\" on item " + id());
    }

    private int getActionId(int action) {
        switch (action) {
            case 0:
                return MenuAction.ITEM_FIRST_OPTION.getId();
            case 1:
                return MenuAction.ITEM_SECOND_OPTION.getId();
            case 2:
                return MenuAction.ITEM_THIRD_OPTION.getId();
            case 3:
                return MenuAction.ITEM_FOURTH_OPTION.getId();
            case 4:
                return MenuAction.ITEM_FIFTH_OPTION.getId();
            default:
                throw new IllegalArgumentException("action = " + action);
        }
    }

    public void interact(int action) {
        bot().clientThread.invoke(() -> {
            bot.client().invokeMenuAction("",
                    "",
                    id(),
                    getActionId(action),
                    slot(),
                    WidgetInfo.INVENTORY.getId()
            );
        });
    }

    @Override
    public void useOn(InventoryItem item) {
        bot.clientThread.invoke(() -> {
            bot.client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
            bot.client.setSelectedItemSlot(item.slot());
            bot.client.setSelectedItemID(item.id());
            bot.client.invokeMenuAction("", "", id(),
                    MenuAction.ITEM_USE_ON_WIDGET_ITEM.getId(), slot(), WidgetInfo.INVENTORY.getId());
        });
    }

    @Override
    public void useOn(iObject object) {
        bot.clientThread.invoke(() -> {
            bot.client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
            bot.client.setSelectedItemSlot(slot());
            bot.client.setSelectedItemID(id());
            bot.client.invokeMenuAction("", "", object.id(),
                    MenuAction.ITEM_USE_ON_GAME_OBJECT.getId(), object.menuPoint().getX(), object.menuPoint().getY());
        });
    }

    @Override
    public void useOn(iNPC npc) {
        bot.clientThread.invoke(() -> {
            bot.client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
            bot.client.setSelectedItemSlot(slot());
            bot.client.setSelectedItemID(id());
            bot.client.invokeMenuAction("", "", npc.index(),
                    MenuAction.ITEM_USE_ON_NPC.getId(), 0, 0);
        });
    }
//
//    @Override
//    public void useOn(iPlayer player) {
//        game.mouseClicked();
//        game.connection().playerUseItem(player.index(), id, slot, (containingWidget.group << 16) + containingWidget.file, game.ctrlRun);
//    }
//
//    @Override
//    public void useOn(iNPC npc) {
//        game.mouseClicked();
//        game.connection().npcUseItem(npc.index(), id, slot, (containingWidget.group << 16) + containingWidget.file, game.ctrlRun);
//    }

    public String toString() {
        return name() + " (" + id() + ")" + (quantity() == 1 ? "" : " x" + quantity());
    }
}
