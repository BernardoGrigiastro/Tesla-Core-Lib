package net.ndrei.teslacorelib.inventory

import net.minecraft.item.EnumDyeColor
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.INBTSerializable
import net.modcrafters.mclib.energy.implementations.GenericEnergyStorage
import net.ndrei.teslacorelib.MOD_ID
import net.ndrei.teslacorelib.capabilities.inventory.ISidedItemHandlerConfig
import net.ndrei.teslacorelib.energy.EnergySystemFactory

/**
 * Created by CF on 2017-06-28.
 */
open class EnergyStorage(maxStoredEnergy: Long, inputRate: Long, outputRate: Long)
    : GenericEnergyStorage(maxStoredEnergy, inputRate, outputRate), INBTSerializable<NBTTagCompound>, ICapabilityProvider, IEnergyStatistics {

    var color: EnumDyeColor? = null
        private set

    private var sidedConfig: ISidedItemHandlerConfig? = null

    private var statStored: Long = 0
    override final var averageEnergyPerTick: Long = 0
        private set
    override final var lastTickEnergy: Long = 0
        private set
    private val statTicks = mutableListOf<Long>()

    //region util method

    fun workPerformed(jobEnergy: Long): Long {
        return this.workPerformed(jobEnergy, 1.0f)
    }

    fun workPerformed(jobEnergy: Long, jobPercent: Float): Long {
        val energy = Math.round(jobEnergy.toDouble() * Math.max(0f, Math.min(1f, jobPercent)))
        return this.takePower(energy)
    }

    fun givePower(energy: Long): Long {
        return this.givePower(energy, false, true)
    }

    fun takePower(energy: Long): Long {
        return this.takePower(energy, false, true)
    }

    val isFull: Boolean
        get() = this.capacity == this.stored

    val isEmpty: Boolean
        get() = this.stored == 0L

    /**
     * Sets the capacity of the the container. If the existing stored power is more than the
     * new capacity, the stored power will be decreased to match the new capacity.

     * @param capacity The new capacity for the container.
     * *
     * @return The instance of the container being updated.
     */
    fun setCapacity(capacity: Long): EnergyStorage {
        val stored = this.stored
        this.capacity = capacity

        if (this.stored > capacity)
            this.stored = capacity

        if (stored != this.stored) {
            this.onChanged(stored, this.stored)
        }
        return this
    }

    /**
     * Gets the maximum amount of Tesla power that can be accepted by the container.

     * @return The amount of Tesla power that can be accepted at any time.
     */
    fun getEnergyInputRate(): Long {
        return this.inputRate
    }

    /**
     * Sets the maximum amount of Tesla power that can be accepted by the container.

     * @param rate The amount of Tesla power to accept at a time.
     * *
     * @return The instance of the container being updated.
     */
    fun setEnergyInputRate(rate: Long): EnergyStorage {
        this.inputRate = rate
        this.onChanged(this.stored, this.stored)
        return this
    }

    /**
     * Gets the maximum amount of Tesla power that can be pulled from the container.

     * @return The amount of Tesla power that can be extracted at any time.
     */
    fun getEnergyOutputRate(): Long {
        return this.outputRate
    }

    /**
     * Sets the maximum amount of Tesla power that can be pulled from the container.

     * @param rate The amount of Tesla power that can be extracted.
     * *
     * @return The instance of the container being updated.
     */
    fun setEnergyOutputRate(rate: Long): EnergyStorage {
        this.outputRate = rate
        this.onChanged(this.stored, this.stored)
        return this
    }

    /**
     * Sets both the input and output rates of the container at the same time. Both rates will
     * be the same.

     * @param rate The input/output rate for the Tesla container.
     * *
     * @return The instance of the container being updated.
     */
    fun setEnergyTransferRate(rate: Long): EnergyStorage {
        this.setEnergyInputRate(rate)
        this.setEnergyOutputRate(rate)
        return this
    }

    //endregion

    //#region nbt storage

    override fun serializeNBT(): NBTTagCompound {
        val dataTag = NBTTagCompound()
        dataTag.setLong("TeslaPower", this.stored)
        dataTag.setLong("TeslaCapacity", this.capacity)
        dataTag.setLong("TeslaInput", this.inputRate)
        dataTag.setLong("TeslaOutput", this.outputRate)

        return dataTag
    }

    override fun deserializeNBT(nbt: NBTTagCompound) {
        val originalStored = this.stored
        this.stored = nbt.getLong("TeslaPower")

        if (nbt.hasKey("TeslaCapacity"))
            this.capacity = nbt.getLong("TeslaCapacity")

        if (nbt.hasKey("TeslaInput"))
            this.inputRate = nbt.getLong("TeslaInput")

        if (nbt.hasKey("TeslaOutput"))
            this.outputRate = nbt.getLong("TeslaOutput")

        if (this.stored > this.capacity)
            this.stored = this.capacity

        if (this.stored != originalStored) {
            this.onChanged(originalStored, this.stored)
        }
    }

    //#endregion

    fun isSideAllowed(facing: EnumFacing?)
        = ((this.sidedConfig != null) && (this.color != null) && (facing != null) && this.sidedConfig!!.isSideSet(this.color!!, facing))

    override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean {
        if (this.isSideAllowed(facing)) {
            return EnergySystemFactory.isCapabilitySupported(capability)
        }

        return false
    }

    override fun <T> getCapability(capability: Capability<T>, facing: EnumFacing?): T? {
        if (this.isSideAllowed(facing)) {
            return EnergySystemFactory.wrapCapability(capability, this)
        }
        return null
    }

    fun setSidedConfig(color: EnumDyeColor, sidedConfig: ISidedItemHandlerConfig, highlight: BoundingRectangle) {
        if (this.sidedConfig === sidedConfig) {
            return
        }

        this.sidedConfig = sidedConfig
        this.color = color
        if ((this.sidedConfig != null) && (this.color != null)) {
            this.sidedConfig!!.addColoredInfo("$MOD_ID:Energy", this.color!!, highlight, -20)
        }
    }

    fun processStatistics() {
        this.lastTickEnergy = this.stored - this.statStored
        this.statStored = this.stored

        this.statTicks.add(this.lastTickEnergy)
        while (this.statTicks.size > 10) {
            this.statTicks.removeAt(0)
        }
        var sum: Long = 0
        for (l in this.statTicks) {
            sum += l
        }
        this.averageEnergyPerTick = sum / this.statTicks.size
    }
}
