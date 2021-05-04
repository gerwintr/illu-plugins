package net.runelite.client.plugins.iutils.bot;

import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.client.plugins.iutils.api.EquipmentSlot;
import net.runelite.client.plugins.iutils.api.Interactable;

import java.util.Arrays;
import java.util.List;

public class EquipmentItem implements Interactable {
    private final Bot bot;
    private final Item item;
    private final ItemComposition definition;
    private final EquipmentSlot equipmentSlot;

    public EquipmentItem(Bot bot, Item item, ItemComposition definition, EquipmentSlot equipmentSlot) {
        this.bot = bot;
        this.item = item;
        this.definition = definition;
        this.equipmentSlot = equipmentSlot;
    }

    public Bot bot() {
        return bot;
    }

    public int id() {
        return item.getId();
    }

    public String name() {
        return definition.getName();
    }

    public int quantity() {
        return item.getQuantity();
    }

    public int slot() {
        return equipmentSlot.index;
    }

    public ItemComposition definition() {
        return definition;
    }

    @Override
    public List<String> actions() {
        var itemAttributes = definition().getParams();
        if (itemAttributes == null) return List.of("Remove", "Examine");
        var actions = new String[10];
        actions[0] = "Remove";
        actions[1] = "Examine";
        for (int i = 0; i < 8; i++) {
            if (itemAttributes.get(451 + i) != null) {
                actions[i + 1] = definition.getStringValue(451 + i);
            }
        }
        System.out.println(Arrays.asList(actions).toString());
        return Arrays.asList(actions);
    }

    @Override
    public void interact(String action) {
        List<String> actions = actions();

        for (int i = 0; i < actions.size(); i++) {
            if (action.equalsIgnoreCase(actions.get(i))) {
                interact(i + 1);
                return;
            }
        }

        throw new IllegalArgumentException("no action \"" + action + "\" on item " + id());
    }

    public void interact(int action) {
        bot().clientThread.invoke(() -> {
            bot.client().invokeMenuAction("",
                    "",
                    action,
                    MenuAction.CC_OP.getId(),
                    -1,
                    equipmentSlot.widgetInfo.getId()
            );
        });
    }

    public String toString() {
        return name() + " (" + id() + ")" + (quantity() == 1 ? "" : " x" + quantity());
    }
}
