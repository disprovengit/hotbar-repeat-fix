# Hotbar Repeat Fix

A lightweight client-side Fabric mod by Disproven. Hold one hotbar key and tap another to switch briefly, then return to the held slot. Jumping preserves repeat behavior. Releasing the held key clears pending repeats.

## Reviewing the code

The two mixins in src/main/java/dev/hotbarrepeatfix/mixin handle keyboard events and hotbar press counts for 1.21.11. The equivalent code for each newer version lives in ports under its Minecraft version number. RepeatConfig handles the local enabled setting in config/hotbar-repeat-fix.properties. Mod Menu provides an optional on/off button.

## Building

For Minecraft 1.21.11, run ./gradlew build (gradlew.bat build on Windows) with JDK 21. The mod JAR is written to build/libs.

For the 26.x builds, the original Windows build script is in scripts/build-port.ps1. Use JDK 25 with javac and jar on PATH and run ./scripts/build-port.ps1 -Version 26.3 (or 26.1 or 26.2) in PowerShell. This offline script expects the matching Minecraft client and its dependencies installed in the standard launcher and Gradle caches. The script lists the exact Fabric Loader, Mixin, Mod Menu and LWJGL dependencies. The 26.1 build uses Minecraft 26.1.2.

## Checking downloads

SHA256SUMS contains the digests of the four distributed 1.0.4 JARs. Run Get-FileHash on your downloaded JAR in PowerShell and compare its SHA-256 with the entry for that filename. These hashes identify the original downloads. Independently rebuilt JARs can differ in archive metadata.

The mod source is available under the MIT License. The Gradle wrapper retains its upstream license.
