package dev.ithundxr.createnumismatics.content.depositor;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.backend.Trusted;
import dev.ithundxr.createnumismatics.content.bank.CardItem;
import dev.ithundxr.createnumismatics.content.coins.CoinBag;
import dev.ithundxr.createnumismatics.content.coins.DiscreteCoinBag;
import dev.ithundxr.createnumismatics.registry.NumismaticsTags;
import dev.ithundxr.createnumismatics.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractDepositorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Trusted {

    public final Container cardContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            AbstractDepositorBlockEntity.this.setChanged();
        }
    };
    @Nullable
    protected UUID owner;

    protected final List<UUID> trustList = new ArrayList<>();

    protected final DiscreteCoinBag inventory = new DiscreteCoinBag();

    public AbstractDepositorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void activate() {
        if (level instanceof ServerLevel serverLevel) {
            if (getBlockState().getBlock() instanceof AbstractDepositorBlock<?> depositorBlock) {
                depositorBlock.activate(getBlockState(), serverLevel, worldPosition);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        if (owner != null)
            tag.putUUID("Owner", owner);
        if (!trustList.isEmpty()) {
            ListTag list = new ListTag();
            for (UUID id : trustList) {
                list.add(NbtUtils.createUUID(id));
            }
            tag.put("TrustList", list);
        }
        if (!inventory.isEmpty()) {
            tag.put("Inventory", inventory.save(new CompoundTag()));
        }

        if (!cardContainer.getItem(0).isEmpty()) {
            tag.put("Card", cardContainer.getItem(0).save(new CompoundTag()));
        }
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        trustList.clear();

        if (tag.contains("TrustList", Tag.TAG_LIST)) {
            ListTag list = tag.getList("TrustList", Tag.TAG_INT_ARRAY);
            for (Tag entry : list) {
                if (entry.getType() == IntArrayTag.TYPE)
                    trustList.add(NbtUtils.loadUUID(entry));
            }
        }

        inventory.clear();
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            inventory.load(tag.getCompound("Inventory"));
        }

        if (tag.contains("Card", Tag.TAG_COMPOUND)) {
            ItemStack cardStack = ItemStack.of(tag.getCompound("Card"));
            cardContainer.setItem(0, cardStack);
        } else {
            cardContainer.setItem(0, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean isTrustedInternal(Player player) {
        if (Utils.isDevEnv()) { // easier to test this way in dev
            return player.getItemBySlot(EquipmentSlot.FEET).is(Items.GOLDEN_BOOTS);
        } else {
            return owner == null || owner.equals(player.getUUID()) || trustList.contains(player.getUUID());
        }
    }

    @Nullable
    public UUID getDepositAccount() {
        ItemStack cardStack = cardContainer.getItem(0);
        if (cardStack.isEmpty())
            return null;
        if (!NumismaticsTags.AllItemTags.CARDS.matches(cardStack))
            return null;

        return CardItem.get(cardStack);
    }

    public void addCoin(Coin coin, int count) {
        UUID depositAccount = getDepositAccount();
        if (depositAccount != null) {
            BankAccount account = Numismatics.BANK.getAccount(depositAccount);
            if (account != null) {
                account.deposit(coin, count);
                return;
            }
        }
        inventory.add(coin, count);
        setChanged();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        UUID depositAccount = getDepositAccount();
        if (depositAccount != null && !inventory.isEmpty()) {
            BankAccount account = Numismatics.BANK.getAccount(depositAccount);
            if (account != null) {
                for (Coin coin : Coin.values()) {
                    int count = inventory.getDiscrete(coin);
                    inventory.subtract(coin, count);
                    account.deposit(coin, count);
                }
            }
        }
    }

    protected static class DepositorValueBoxTransform extends CenteredSideValueBoxTransform {
        public DepositorValueBoxTransform() {
            super((state, d) -> d.getAxis().isVertical());
        }

        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8, 8, 15.5);
        }
    }
}
