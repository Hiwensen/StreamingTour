# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("streaming")
set(my_lib_path ${CMAKE_SOURCE_DIR}/../../../libs)

#include_directories(include)
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}")
#
#message(-------------)
#message(${CMAKE_SOURCE_DIR})
aux_source_directory(. SOURCE)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        streaming

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${SOURCE})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

add_library(libavcodec
        SHARED
        IMPORTED )
set_target_properties(libavcodec
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libavcodec.so )

add_library(libavfilter
        SHARED
        IMPORTED )
set_target_properties(libavfilter
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libavfilter.so )

add_library(libavformat
        SHARED
        IMPORTED )
set_target_properties(libavformat
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libavformat.so )

add_library(libavutil
        SHARED
        IMPORTED)
set_target_properties(libavutil
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libavutil.so )

add_library(libswresample
        SHARED
        IMPORTED)
set_target_properties(libswresample
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libswresample.so )

add_library(libswscale
        SHARED
        IMPORTED)
set_target_properties(libswscale
        PROPERTIES IMPORTED_LOCATION
        ${my_lib_path}/armeabi-v7a/libswscale.so )

include_directories( ${my_lib_path}/include)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries( # Specifies the target library.
        streaming
        -landroid
        libavcodec libavfilter libavformat libavutil libswresample libswscale

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        OpenSLES
        android)