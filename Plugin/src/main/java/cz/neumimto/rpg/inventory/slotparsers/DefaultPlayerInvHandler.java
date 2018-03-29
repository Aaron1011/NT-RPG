package cz.neumimto.rpg.inventory.slotparsers;

import cz.neumimto.core.ioc.Singleton;
import cz.neumimto.rpg.NtRpgPlugin;
import cz.neumimto.rpg.TextHelper;
import cz.neumimto.rpg.configuration.Localization;
import cz.neumimto.rpg.inventory.CannotUseItemReson;
import cz.neumimto.rpg.inventory.HotbarObject;
import cz.neumimto.rpg.inventory.HotbarObjectTypes;
import cz.neumimto.rpg.inventory.Weapon;
import cz.neumimto.rpg.players.IActiveCharacter;
import cz.neumimto.rpg.utils.ItemStackUtils;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.Hotbar;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;

import java.util.Optional;

/**
 * Created by NeumimTo on 26.3.2018.
 */
@Singleton
public class DefaultPlayerInvHandler extends PlayerInvHandler {

    public DefaultPlayerInvHandler() {
        super("slot_order");
    }

    @Override
    public void initializeHotbar(IActiveCharacter character) {
        Hotbar hotbar = character.getPlayer().getInventory().query(Hotbar.class);
        int slot = 0;
        for (Inventory inventory : hotbar) {
            initializeHotbar(character, slot, (Slot) inventory, hotbar);
            slot++;
        }
    }

    //TODO second hand
    protected void initializeHotbar(IActiveCharacter character, int slot, Slot s, Hotbar hotbar) {
        int selectedSlotIndex = hotbar.getSelectedSlotIndex();
        //very stupid but fast
        Optional<ItemStack> peek = s.peek();

        //unequip
        HotbarObject hotbarObject = character.getHotbar()[slot];
        if (hotbarObject != null) {
            hotbarObject.onUnEquip(character);
        }

        //parse slot
        if (peek.isPresent()) {
            ItemStack itemStack1 = peek.get();

            HotbarObject hotbarObject0 = NtRpgPlugin.GlobalScope.inventorySerivce.getHotbarObject(character, itemStack1);

            if (hotbarObject0 != HotbarObject.EMPTYHAND_OR_CONSUMABLE) {
                hotbarObject0.setSlot(slot);
                character.getHotbar()[slot] = hotbarObject0;
                CannotUseItemReson reason = NtRpgPlugin.GlobalScope.inventorySerivce.canUse(itemStack1, character);
                //cannot use
                if (reason != CannotUseItemReson.OK) {
                    character.getHotbar()[slot] = hotbarObject0;
                    character.getPlayer().sendMessage(ChatTypes.ACTION_BAR, TextHelper.parse(Localization.PLAYER_CANT_USE_HOTBAR_ITEMS));
                    character.getDenyHotbarSlotInteractions()[slot] = true;
                    return;
                } else {
                    character.getDenyHotbarSlotInteractions()[slot] = false;
                }

                //charm is active anywhere in the hotbar
                if (hotbarObject0.getHotbarObjectType() == HotbarObjectTypes.CHARM) {
                    hotbarObject0.onEquip(character);
                    character.getDenyHotbarSlotInteractions()[slot] = false;
                } else if (hotbarObject0.getHotbarObjectType() == HotbarObjectTypes.WEAPON && slot == selectedSlotIndex) {
                    //weapon active only if item is in hand
                    hotbarObject0.onRightClick(character); //simulate player interaction to equip the weapon
                    ((Weapon) hotbarObject0).setCurrent(true);
                }

            } else {
                //consumable/non weapon item
                character.getHotbar()[slot] = HotbarObject.EMPTYHAND_OR_CONSUMABLE;
                character.getDenyHotbarSlotInteractions()[slot] = false;
            }

        } else {
            //empty slot
            character.getHotbar()[slot] = HotbarObject.EMPTYHAND_OR_CONSUMABLE;
            character.getDenyHotbarSlotInteractions()[slot] = false;
        }
        // ??
    }

    @Override
    public void initializeArmor(IActiveCharacter character) {

    }

    @Override
    public void changeActiveHotbarSlot(IActiveCharacter character, int slot) {
        if (character.getDenyHotbarSlotInteractions()[slot]) {
            HotbarObject hotbarObject = character.getHotbar()[slot];
            if (hotbarObject != HotbarObject.EMPTYHAND_OR_CONSUMABLE) {
                if (hotbarObject.getHotbarObjectType() == HotbarObjectTypes.WEAPON) {
                    Optional<ItemStack> itemInHand = character.getPlayer().getItemInHand(HandTypes.MAIN_HAND);
                    ItemStack itemStack = itemInHand.get().copy();
                    character.getPlayer().setItemInHand(HandTypes.MAIN_HAND, null);
                    ItemStackUtils.dropItem(character.getPlayer(), itemStack);
                }
                character.getPlayer().sendMessage(ChatTypes.ACTION_BAR, TextHelper.parse(Localization.CANNOT_USE_ITEM_GENERIC));
            }
        } else {
            character.getPlayer().sendMessage(ChatTypes.ACTION_BAR, Text.of(""));
        }
    }
}
