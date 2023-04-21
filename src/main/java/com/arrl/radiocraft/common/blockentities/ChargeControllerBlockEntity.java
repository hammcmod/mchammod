package com.arrl.radiocraft.common.blockentities;

import com.arrl.radiocraft.RadiocraftConfig;
import com.arrl.radiocraft.common.init.RadiocraftBlockEntities;
import com.arrl.radiocraft.common.menus.ChargeControllerMenu;
import com.arrl.radiocraft.common.power.ConnectionType;
import com.arrl.radiocraft.common.power.PowerNetwork;
import com.arrl.radiocraft.common.power.PowerNetwork.PowerNetworkEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ChargeControllerBlockEntity extends AbstractPowerBlockEntity {

	private int lastPowerTick = 0;

	// Using a ContainerData for one value is awkward, but it changes constantly and needs to be synchronised.
	private final ContainerData fields = new ContainerData() {

		@Override
		public int get(int index) {
			if(index == 0)
				return lastPowerTick;
			return 0;
		}

		@Override
		public void set(int index, int value) {
			if(index == 0)
				lastPowerTick = 0;
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	public ChargeControllerBlockEntity(BlockPos pos, BlockState state) {
		super(RadiocraftBlockEntities.CHARGE_CONTROLLER.get(), pos, state, RadiocraftConfig.CHARGE_CONTROLLER_TICK.get(), RadiocraftConfig.CHARGE_CONTROLLER_TICK.get());
	}

	public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
		if(t instanceof ChargeControllerBlockEntity be) {
			if(!level.isClientSide) { // Serverside only
				int energyToPush = be.energyStorage.extractEnergy(be.energyStorage.getEnergyStored(), true); // Do not actually pull out power yet.
				be.lastPowerTick = energyToPush;

				List<LargeBatteryBlockEntity> batteries = new ArrayList<>(); // Specifically grab batteries to avoid having to use another sorted list.
				for(PowerNetwork network : be.getNetworks().values()) {
					for(PowerNetworkEntry item : network.getConnections()) {
						if(item.getNetworkItem().getConnectionType() == ConnectionType.PUSH) // Double check here is faster as instanceof can be quite slow.
							if(item.getNetworkItem() instanceof LargeBatteryBlockEntity battery)
								batteries.add(battery);
					}
				}

				for(LargeBatteryBlockEntity battery : batteries) {
					LazyOptional<IEnergyStorage> energyCap = battery.getCapability(ForgeCapabilities.ENERGY);
					if(energyCap.isPresent()) { // This is horrendous code but java doesn't like lambdas and vars.
						IEnergyStorage storage = energyCap.orElse(null);
						energyToPush -= storage.receiveEnergy(energyToPush, false);

						if(energyToPush <= 0)
							break;
					}
				}
				be.energyStorage.setEnergy(energyToPush); // Set energy to the remainder after pushing.
			}
		}
	}

	@Override
	public Component getDisplayName() {
		return Component.translatable("container.charge_controller");
	}

	@Nullable
	@Override
	public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
		return new ChargeControllerMenu(id, this, fields);
	}
}
