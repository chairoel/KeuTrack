cmake_minimum_required(VERSION 3.22.1)

project("network_config")

add_library(${CMAKE_PROJECT_NAME} SHARED network_native_config.cpp)

if(NOT DEFINED TYPE)
    set(TYPE "dev")  # default value
endif()

add_definitions(-DTYPE="${TYPE}")