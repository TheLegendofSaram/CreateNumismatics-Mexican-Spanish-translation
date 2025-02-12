package dev.ithundxr.createnumismatics.content.vendor;

import com.simibubi.create.foundation.gui.menu.MenuBase;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.bank.CardSlot;
import dev.ithundxr.createnumismatics.content.coins.CoinDisplaySlot;
import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import dev.ithundxr.createnumismatics.content.coins.SlotDiscreteCoinBag;
import dev.ithundxr.createnumismatics.registry.NumismaticsTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class VendorMenu extends MenuBase<VendorBlockEntity> {
    public static final int COIN_SLOTS = Coin.values().length;
    public static final int CARD_SLOT_INDEX = COIN_SLOTS;
    public static final int SELLING_SLOT_INDEX = CARD_SLOT_INDEX + 1;
    public static final int INV_START_INDEX = SELLING_SLOT_INDEX + 1;
    public static final int INV_END_INDEX = INV_START_INDEX + 9; // exclusive
    public static final int PLAYER_INV_START_INDEX = INV_END_INDEX;
    public static final int PLAYER_HOTBAR_END_INDEX = PLAYER_INV_START_INDEX + 9; // exclusive
    public static final int PLAYER_INV_END_INDEX = PLAYER_INV_START_INDEX + 36; // exclusive
    public VendorMenu(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public VendorMenu(MenuType<?> type, int id, Inventory inv, VendorBlockEntity contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected VendorBlockEntity createOnClient(FriendlyByteBuf extraData) {
        ClientLevel world = Minecraft.getInstance().level;
        BlockEntity blockEntity = world.getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof VendorBlockEntity vendorBE) {
            vendorBE.readClient(extraData.readNbt());
            return vendorBE;
        }
        return null;
    }

    @Override
    protected void initAndReadInventory(VendorBlockEntity contentHolder) {}

    @Override
    protected void addSlots() {
        int x = 14+16;
        int y = 122;

        for (Coin coin : Coin.values()) {
            addSlot(new SlotDiscreteCoinBag(contentHolder.inventory, coin, x, y, true, true));
            x += 18;
        }
        addSlot(new CardSlot.BoundCardSlot(contentHolder.cardContainer, 0, 170+4, y)); // make here to preserve slot order
        addSlot(new Slot(contentHolder.sellingContainer, 0, 142+5, y));

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new FilteringSlot(contentHolder, j + i * 3, 87 + j * 18, 49 + i * 18 + 11, contentHolder::matchesSellingItem));
            }
        }

        addPlayerSlots(58, 165);

        // label coins

        int labelX1 = 12;
        int labelX2 = labelX1 + 86 + 54;
        int labelY = 46;
        int labelYIncrement = 22;

        for (int i = 0; i < 6; i++) {
            Coin coin = Coin.values()[i];
            int slotX = i < 3 ? labelX1 : labelX2;
            int slotY = labelY + ((i%3) * labelYIncrement);

            addSlot(new CoinDisplaySlot(coin, slotX, slotY));
        }
    }

    @Override
    protected void saveData(VendorBlockEntity contentHolder) {}

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        //if (clickType == ClickType.THROW)
        //    return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot clickedSlot = getSlot(index);
        if (!clickedSlot.hasItem())
            return ItemStack.EMPTY;
        ItemStack stack = clickedSlot.getItem();

        if (index < COIN_SLOTS) { // extracting coins
            if (!(stack.getItem() instanceof CoinItem coinItem))
                return ItemStack.EMPTY;

            Coin coin = coinItem.coin;
            int startCount = stack.getCount();

            moveItemStackTo(stack, PLAYER_INV_START_INDEX, PLAYER_INV_END_INDEX, false);

            int diff = startCount - stack.getCount();

            if (diff > 0) {
                contentHolder.inventory.subtract(coin, diff);
                clickedSlot.setChanged();
            } else if (diff < 0) {
                contentHolder.inventory.add(coin, -diff);
                clickedSlot.setChanged();
            }

            return ItemStack.EMPTY;
        } else if (index == CARD_SLOT_INDEX) { // removing card
            if (!moveItemStackTo(stack, PLAYER_INV_START_INDEX, PLAYER_INV_END_INDEX, false))
                return ItemStack.EMPTY;
        } else if (index == SELLING_SLOT_INDEX) { // removing selling item
            if (!moveItemStackTo(stack, PLAYER_INV_START_INDEX, PLAYER_INV_END_INDEX, false))
                return ItemStack.EMPTY;
        } else if (INV_START_INDEX <= index && index < INV_END_INDEX) { // removing stock
            if (!moveItemStackTo(stack, PLAYER_INV_START_INDEX, PLAYER_INV_END_INDEX, false))
                return ItemStack.EMPTY;
        } else { // player inventory
            /*
            priority:
            1. Coin slots
            2. Card slot
            3. Selling slot (if empty)
            4. Inventory (if accepted)
            5. Player inventory
             */
            if (stack.getItem() instanceof CoinItem && !moveItemStackTo(stack, 0, COIN_SLOTS, false)) {
                return ItemStack.EMPTY;
            } else if (NumismaticsTags.AllItemTags.CARDS.matches(stack) && !moveItemStackTo(stack, CARD_SLOT_INDEX, CARD_SLOT_INDEX+1, false)) {
                return ItemStack.EMPTY;
            } else if (contentHolder.sellingContainer.isEmpty() && !moveItemStackTo(stack, SELLING_SLOT_INDEX, SELLING_SLOT_INDEX+1, false)) {
                return ItemStack.EMPTY;
            } else if (contentHolder.matchesSellingItem(stack) && !moveItemStackTo(stack, INV_START_INDEX, INV_END_INDEX, false)) {
                return ItemStack.EMPTY;
            } else if (index >= PLAYER_INV_START_INDEX && index < PLAYER_HOTBAR_END_INDEX && !moveItemStackTo(stack, PLAYER_HOTBAR_END_INDEX, PLAYER_INV_END_INDEX, false)) {
                return ItemStack.EMPTY;
            } else if (index >= PLAYER_HOTBAR_END_INDEX && index < PLAYER_INV_END_INDEX && !moveItemStackTo(stack, PLAYER_INV_START_INDEX, PLAYER_HOTBAR_END_INDEX, false)) {
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }
}
