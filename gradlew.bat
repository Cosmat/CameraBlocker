@echo off
rem Gradle wrapper for Windows
rem Requires Gradle installed and in PATH, or use Android Studio's embedded Gradle

set GRADLE_CMD=gradle
if exist "%USERPROFILE%\.gradle\wrapper\dists\gradle-8.2.2-bin\*" (
    for /d %%i in ("%USERPROFILE%\.gradle\wrapper\dists\gradle-8.2.2-bin\*") do (
        if exist "%%i\bin\gradle.bat" set GRADLE_CMD="%%i\bin\gradle.bat"
    )
)

%GRADLE_CMD% %*