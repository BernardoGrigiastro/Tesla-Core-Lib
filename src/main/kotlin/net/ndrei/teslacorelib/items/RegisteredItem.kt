package net.ndrei.teslacorelib.items

import net.minecraft.client.renderer.block.model.ModelResourceLocation
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import net.minecraftforge.registries.IForgeRegistry
import net.ndrei.teslacorelib.render.ISelfRegisteringRenderer

/**
 * Created by CF on 2017-06-22.
 */
abstract class RegisteredItem(modId: String, tab: CreativeTabs?, registryName: String)
    : Item(), ISelfRegisteringItem, ISelfRegisteringRenderer {
    init {
        this.setRegistryName(modId, registryName)
        this.translationKey = "$modId.$registryName"
        if (tab != null) {
            this.creativeTab = tab
        }
    }

    override fun registerItem(registry: IForgeRegistry<Item>) {
        registry.register(this)
    }

    @Deprecated("One should really use JSON resources for recipes.", ReplaceWith("A JSON File!"), DeprecationLevel.WARNING)
    fun registerRecipe(registry: (recipe: IRecipe) -> ResourceLocation)
            = this.recipes.forEach { registry(it) }

    @Deprecated("One should really use JSON resources for recipes.", ReplaceWith("A JSON File!"), DeprecationLevel.WARNING)
    protected open val recipe: IRecipe?
        get() = null

    @Deprecated("One should really use JSON resources for recipes.", ReplaceWith("A JSON File!"), DeprecationLevel.WARNING)
    protected open val recipes: List<IRecipe>
        get() {
            val recipe = this.recipe
            return if (recipe != null) listOf(recipe) else listOf()
        }

    @SideOnly(Side.CLIENT)
    override fun registerRenderer() = ModelLoader.setCustomModelResourceLocation(this, 0, ModelResourceLocation(this.registryName!!, "inventory"))
}
