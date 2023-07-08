package dev.ithundxr.createnumismatics.content.depositor;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import dev.ithundxr.createnumismatics.content.backend.Coin;
import dev.ithundxr.createnumismatics.content.bank.CardItem;
import dev.ithundxr.createnumismatics.content.coins.CoinItem;
import dev.ithundxr.createnumismatics.registry.NumismaticsBlockEntities;
import dev.ithundxr.createnumismatics.registry.NumismaticsTags;
import dev.ithundxr.createnumismatics.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class BrassDepositorBlock extends AbstractDepositorBlock<BrassDepositorBlockEntity> {
    public BrassDepositorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<BrassDepositorBlockEntity> getBlockEntityClass() {
        return BrassDepositorBlockEntity.class;
    }

    @Override
    public BlockEntityType<BrassDepositorBlockEntity> getBlockEntityType() {
        return NumismaticsBlockEntities.BRASS_DEPOSITOR.get();
    }

    @Override
    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos,
                                          @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {

        if (hit.getDirection().getAxis().isVertical()) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;
            if (isTrusted(player, level, pos)) {
                withBlockEntityDo(level, pos,
                    be -> Utils.openScreen((ServerPlayer) player, be, be::sendToMenu));
            }
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(HORIZONTAL_FACING) != hit.getDirection())
            return InteractionResult.PASS;

        if (state.getValue(POWERED) || state.getValue(LOCKED))
            return InteractionResult.FAIL;

        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof BrassDepositorBlockEntity brassDepositor) {
            int totalPrice = brassDepositor.getTotalPrice();

            ItemStack handStack = player.getItemInHand(hand);
            if (NumismaticsTags.AllItemTags.CARDS.matches(handStack)) {
                if (CardItem.isBound(handStack)) {
                    UUID id = CardItem.get(handStack);
                    BankAccount account = Numismatics.BANK.getAccount(id);
                    if (account != null && account.isAuthorized(player)) {
                        if (account.deduct(totalPrice)) {
                            activate(state, level, pos);
                            for (Map.Entry<Coin, Integer> entry : brassDepositor.prices.entrySet()) {
                                brassDepositor.addCoin(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                }
            } else if (CoinItem.extract(player, hand, brassDepositor.prices, false)) {
                activate(state, level, pos);
                for (Map.Entry<Coin, Integer> entry : brassDepositor.prices.entrySet()) {
                    brassDepositor.addCoin(entry.getKey(), entry.getValue());
                }
            }

        }
        return InteractionResult.CONSUME;
    }
}
