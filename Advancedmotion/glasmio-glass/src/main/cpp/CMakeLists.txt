cmake_minimum_required(VERSION 3.22.1)
project(glasmio_glass LANGUAGES CXX)
add_library(glasmio_liquid_menu_sdf SHARED liquid_menu_sdf.cpp)
target_compile_features(glasmio_liquid_menu_sdf PRIVATE cxx_std_17)
# The field evaluator is pure arithmetic: it never throws and never uses RTTI, so
# dropping both keeps libc++'s unwind/typeinfo tables out of every ABI's .so.
target_compile_options(glasmio_liquid_menu_sdf PRIVATE
    -O3 -fno-math-errno -ffp-contract=off -fvisibility=hidden
    -fno-exceptions -fno-rtti -ffunction-sections -fdata-sections
    -fno-unwind-tables -fno-asynchronous-unwind-tables)
target_link_options(glasmio_liquid_menu_sdf PRIVATE
    -Wl,-z,max-page-size=16384 -Wl,--gc-sections -Wl,--strip-all)
set_target_properties(glasmio_liquid_menu_sdf PROPERTIES CXX_VISIBILITY_PRESET hidden VISIBILITY_INLINES_HIDDEN YES)
