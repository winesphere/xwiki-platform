@echo off
REM -------------------------------------------------------------------------
REM See the NOTICE file distributed with this work for additional
REM information regarding copyright ownership.
REM
REM This is free software; you can redistribute it and/or modify it
REM under the terms of the GNU Lesser General Public License as
REM published by the Free Software Foundation; either version 2.1 of
REM the License, or (at your option) any later version.
REM
REM This software is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM Lesser General Public License for more details.
REM
REM You should have received a copy of the GNU Lesser General Public
REM License along with this software; if not, write to the Free
REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.
REM -------------------------------------------------------------------------

set JETTY_HOME=jetty
set JETTY_PORT=8080
set XWIKI_OPTS=-Xmx512m -XX:MaxPermSize=128m
set XWIKI_OPTS=%XWIKI_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

REM Jetty requires a logs directory to exist
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM For enabling YourKit Profiling.
REM %3 must the path where Yourkit can find the agent.
REM For example: "C:\PROGRA~1\YOURKI~1.11\bin\win32"
REM No spaces are allowed in this path, 8.3 DOS path format should be used.
REM Use the "dir /X" command (in command prompt) within a directory to get 8.3 DOS path
if "%2"=="profiler" (
  set OLD_PATH=%PATH%
  set PATH=%3;%PATH%
  set JAVA_TOOL_OPTIONS=-agentlib:yjpagent
)

REM Specify port on which HTTP requests will be handled
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.port=%JETTY_PORT%

REM Specify Jetty's home directory
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home=%JETTY_HOME%

REM Specify port and key to stop a running Jetty instance
set XWIKI_OPTS=%XWIKI_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=8079

REM Specify the encoding to use
set XWIKI_OPTS=%XWIKI_OPTS% -Dfile.encoding=UTF8

java %XWIKI_OPTS% %4 %5 %6 %7 %8 %9 -jar %JETTY_HOME%/start.jar

if %2==profiler set PATH=%OLD_PATH%