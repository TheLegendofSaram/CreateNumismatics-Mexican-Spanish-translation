package dev.ithundxr.createnumismatics.base.data;

import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ithundxr.createnumismatics.content.bank.BankTerminalBlock;
import dev.ithundxr.createnumismatics.content.depositor.AbstractDepositorBlock;

public class BuilderTransformers {
    @ExpectPlatform
    public static <B extends AbstractDepositorBlock<?>, P> NonNullUnaryOperator<BlockBuilder<B, P>> depositor(String material) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <B extends BankTerminalBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> bankTerminal() {
        throw new AssertionError();
    }
}
