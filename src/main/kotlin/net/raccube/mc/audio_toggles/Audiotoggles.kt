package net.raccube.mc.audio_toggles

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Audiotoggles : ModInitializer {
    private val logger = LoggerFactory.getLogger("audio-toggles")

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("Hello Fabric world!")
	}
}