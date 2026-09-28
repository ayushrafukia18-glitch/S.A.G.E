cmake_minimum_required(VERSION 3.22.1)
project("sage_needle_jni")

add_library(needle STATIC IMPORTED)
set_target_properties(needle PROPERTIES
    IMPORTED_LOCATION "${CMAKE_CURRENT_SOURCE_DIR}/libneedle.a"
    INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}"
)

add_library(sage_needle_jni SHARED NeedleJNI.cpp)
find_library(log-lib log)
find_library(android-lib android)
find_library(dl-lib dl)
find_library(m-lib m)

target_compile_features(sage_needle_jni PRIVATE cxx_std_17)
target_link_libraries(sage_needle_jni
    needle
    ${log-lib}
    ${android-lib}
    ${dl-lib}
    ${m-lib}
    c++_shared
)
