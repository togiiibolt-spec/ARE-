ARE  -  NeoForge 1.21.1 horror mod
==================================

HOW TO BUILD (needs internet the first time)
  1. Install Java 21 (https://adoptium.net  ->  Temurin 21 JDK).
  2. Unzip this project anywhere (path with NO spaces / special characters is safest).
  3. Windows: double-click BUILD.bat       Mac/Linux: ./BUILD.sh
  4. When it says BUILD SUCCESSFUL, take  build/libs/are-1.0.0.jar
  5. Put it in your Minecraft "mods" folder (NeoForge 1.21.1) and start the game.

TEST COMMANDS (need cheats / op)
  /are watcher        spawn a Watcher near you
  /are hollow         spawn a Hollow
  /are scare          jumpscare face
  /are glitch         glitch / static
  /are blackout       blackout
  /are book           get the welcome book again
  /are stage 0..3     force the fear stage (-1 = automatic: gets worse each in-game day)

CONFIG: config/are-common.toml  (intensity, turn off system info, torches, doors, fake chat)
The book shows your computer user name / OS / time. It is only shown on YOUR screen,
nothing is sent anywhere. Set systemInfoInBook = false to disable it.
