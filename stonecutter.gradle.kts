plugins {
    id("dev.kikugie.stonecutter")
}

// Версия, которая «активна» в src/ по умолчанию (под неё работает IDE).
// Переключение: ./gradlew "Set active project to <версия>"
// Сборка всех версий сразу: ./gradlew buildAndCollect (jar'ы -> build/libs/<mod.version>/)
// Публикация всех версий на Modrinth: ./gradlew publishMods (нужен env MODRINTH_TOKEN).
stonecutter active "1.21.11"

// Точечные правки исходников под версии, где Mojang переименовал API.
// Базовый src/ написан под 1.20.6–1.21.11; здесь — отличия для новых версий.
// Док: https://stonecutter.kikugie.dev/wiki/config/params
stonecutter parameters {
    replacements {
        // 26.1: переименования в client/inventory API.
        string(current.parsed >= "26.1") {
            // ClickType -> ContainerInput (enum кликов по слотам, константы те же)
            replace("net.minecraft.world.inventory.ClickType", "net.minecraft.world.inventory.ContainerInput")
            replace("ClickType.PICKUP", "ContainerInput.PICKUP")
            replace("ClickType clickType", "ContainerInput clickType")
            // handleInventoryMouseClick -> handleContainerInput (вызов + цель миксина)
            replace("handleInventoryMouseClick", "handleContainerInput")
            // player.displayClientMessage(msg, false) -> player.sendSystemMessage(msg)
            replace("player.displayClientMessage(message, false)", "player.sendSystemMessage(message)")
        }
    }
}
