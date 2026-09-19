cmake_minimum_required(VERSION 3.22)
project(mrs_core CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

option(MRS_BUILD_CLI "Build the command line scanner" ON)

add_library(mrs_core STATIC
    src/escalate.cpp
    src/inflate.cpp
    src/report.cpp
    src/rules.cpp
    src/scan.cpp
    src/util.cpp
    src/zip.cpp
)
target_include_directories(mrs_core PUBLIC ${CMAKE_CURRENT_SOURCE_DIR}/include)

if(MSVC)
    target_compile_options(mrs_core PRIVATE /O1 /GR- /Gy /Gw /utf-8)
else()
    target_compile_options(mrs_core PRIVATE
        -Oz -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fvisibility=hidden)
endif()

if(MRS_BUILD_CLI)
    add_executable(mrs tools/mrs_cli.cpp)
    target_link_libraries(mrs PRIVATE mrs_core)
    if(NOT MSVC)
        target_link_options(mrs PRIVATE -Wl,--gc-sections)
    endif()
endif()
