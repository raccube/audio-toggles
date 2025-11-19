package net.raccube.mc.audio_toggles

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.option.SimpleOption
import net.minecraft.client.util.InputUtil
import net.minecraft.sound.SoundCategory
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.lwjgl.glfw.GLFW
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

object AudioToggles : ClientModInitializer {
    private lateinit var muteSoundMapping: KeyBinding
    private lateinit var muteMusicMapping: KeyBinding
    private lateinit var configFilePath: Path
    private var soundVolume: Double? = null
    private var musicVolume: Double? = null
    private val logger = LoggerFactory.getLogger("audio-toggles")

	override fun onInitializeClient() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
        logger.info("Registering keybinds...")

        val category = KeyBinding.Category.create(Identifier.of("raccube.audio_toggles", "name"))
        this.muteSoundMapping = KeyBinding(
            "raccube.audio_toggles.mute_sound",  // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_N,  // The keycode of the key
            category // The category of the key - you'll need to add a translation for this!
        )
        KeyBindingHelper.registerKeyBinding(this.muteSoundMapping)

        this.muteMusicMapping = KeyBinding(
            "raccube.audio_toggles.mute_music",  // The translation key of the keybinding's name
            InputUtil.Type.KEYSYM,  // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
            GLFW.GLFW_KEY_M,  // The keycode of the key
            category // The category of the key - you'll need to add a translation for this!
        )

        KeyBindingHelper.registerKeyBinding(this.muteMusicMapping)

        logger.info("Registering events...")

        ClientLifecycleEvents.CLIENT_STARTED.register { this.loadConfig(it) }

        ClientTickEvents.START_CLIENT_TICK.register { client ->
            while (this.muteSoundMapping.wasPressed()) {
                val newVolume  = this.handleKeyPress(client, this.soundVolume, SoundCategory.MASTER, "sound")
                if (newVolume != .0) {
                    this.soundVolume = newVolume
                    this.writeConfig()
                }
            }

            while (this.muteMusicMapping.wasPressed()) {
                val newVolume = this.handleKeyPress(client, this.musicVolume, SoundCategory.MUSIC, "music")
                if (newVolume != .0) {
                    this.musicVolume = newVolume
                    this.writeConfig()
                }
            }
        }

        logger.info("Audio Toggles initialized!")
	}

    fun handleKeyPress(client: MinecraftClient, unmutedVolume: Double?, category: SoundCategory, name: String): Double {
        val option = client.options.getSoundVolumeOption(category)

        if (option.value == .0) {
            option.value = unmutedVolume ?: 1.0
            client.player!!.sendMessage(Text.translatable("raccube.audio_toggles.${name}_unmuted"), true)
        } else {
            option.value = .0
            client.player!!.sendMessage(Text.translatable("raccube.audio_toggles.${name}_muted"), true)
        }

        return option.value
    }

    fun loadConfig(client: MinecraftClient) {
        var configDir = FabricLoader.getInstance().configDir
        this.configFilePath = configDir.resolve("raccube.audio_toggles.properties")
        var props = Properties()

        if (configFilePath.exists()) {
            props.load(configFilePath.inputStream())
            this.soundVolume = props.getProperty("sound-volume")?.toDoubleOrNull() ?: client.options.getSoundVolumeOption(SoundCategory.MASTER).value
            this.musicVolume = props.getProperty("music-volume")?.toDoubleOrNull() ?: client.options.getSoundVolumeOption(SoundCategory.MUSIC).value
        } else {
            logger.warn("Config file does not exist")
        }
    }

    fun writeConfig() {
        val props = Properties()
        props.setProperty("sound-volume", soundVolume.toString())
        props.setProperty("music-volume", musicVolume.toString())
        props.store(this.configFilePath.outputStream(), "")
    }
}