@echo off
echo ============================================
echo  Building the "Are" mod (first time = slow,
echo  it downloads Minecraft + NeoForge, ~1 GB)
echo ============================================
call gradlew.bat build
echo.
echo If it says BUILD SUCCESSFUL, your mod is here:
echo   build\libs\are-1.0.0.jar
echo Copy that file into your Minecraft "mods" folder.
pause
