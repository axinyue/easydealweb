@echo off
setlocal

set "ROOT_DIR=%~dp0"
set "WEB_DIR=%ROOT_DIR%web"
set "TARGET_DIR=%ROOT_DIR%src\main\resources\web"

echo Building web app...
echo Source: %WEB_DIR%
echo Target: %TARGET_DIR%

if not exist "%WEB_DIR%\package.json" (
  echo web/package.json not found.
  exit /b 1
)

pushd "%WEB_DIR%" || exit /b 1
call npm.cmd run build
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
popd

if not "%BUILD_EXIT_CODE%"=="0" (
  echo Web build failed.
  exit /b %BUILD_EXIT_CODE%
)

if not exist "%TARGET_DIR%\index.html" (
  echo Web build completed, but %TARGET_DIR%\index.html was not found.
  exit /b 1
)

echo Web build completed and copied to Java resources.
exit /b 0
