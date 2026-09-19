# ⚠ MSVC 编译带中文的源码必须加 /utf-8

SUMMARY: MSVC 默认按系统代码页（本机 936/GBK）读源文件；UTF-8 的中文字面量会被读坏，
报一串看起来毫无关系的语法错。构建里必须显式加 `/utf-8`。
READ WHEN: when MSVC 报 C2001「常量中有换行符」、C4819「该文件包含不能在当前代码页中表示的字符」、
或新增了含中文的 C++ 源码之后。
RECHECK WHEN: 换编译器、或工程改用 clang（clang/g++ 默认就是 UTF-8，不需要这个开关）。

---

## 症状

第一次编 `mrs_core` 时，凡是含中文字符串的文件（rules.cpp、report.cpp、scan.cpp、zip.cpp）
全部失败，报错是这些：

- `error C2001: 常量中有换行符`
- `error C4819: 该文件包含不能在当前代码页(936)中表示的字符`
- `error C3872: "0xe045": 此字符不允许在标识符中使用`
- 连带 `C2143 / C2065 / C3688` 等一堆语法错，指向的行本身其实完全正确

## 修法

在 CMake 里给 MSVC 分支加 `/utf-8`（同时指定源与执行字符集）：

```cmake
if(MSVC)
    target_compile_options(mrs_core PRIVATE /O1 /GR- /Gy /Gw /utf-8)
endif()
```

`core/CMakeLists.txt` 里已经加好。Android/clang 侧不需要这个开关。
