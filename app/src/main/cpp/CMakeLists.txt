cmake_minimum_required(VERSION 3.22)
project(mrs_jni CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(MRS_BUILD_CLI OFF CACHE BOOL "" FORCE)

add_subdirectory(${CMAKE_CURRENT_LIST_DIR}/../../../../core ${CMAKE_BINARY_DIR}/mrs_core)

add_library(mrs_jni SHARED jni_bridge.cpp)
target_link_libraries(mrs_jni PRIVATE mrs_core)
target_compile_options(mrs_jni PRIVATE
    -Oz -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections -fvisibility=hidden)
target_link_options(mrs_jni PRIVATE
    -Wl,--gc-sections -Wl,-z,max-page-size=16384 -Wl,--exclude-libs,ALL)
