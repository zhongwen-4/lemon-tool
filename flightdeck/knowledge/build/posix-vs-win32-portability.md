# Windows 与 Linux 行为差异（本仓库踩过的）

SUMMARY: `fopen(path, "rb")` 打开**目录**时 Windows 返回 NULL、Linux **成功**。用「fopen 成功
就是文件」判定路径类型，会让 Linux 上把目录当成 zip 去解析，报「读取失败或文件超过 64 MB 上限」，
而同一份代码在 Windows 全绿。判定文件/目录一律用 `stat` + `S_ISREG`。
READ WHEN: when 核心代码要区分文件与目录，或 CI（Linux/g++）通过而本机（Windows/MSVC）行为不同时。
RECHECK WHEN: 新增文件系统相关操作时。

---

## 正确写法（`core/src/scan.cpp`）

```cpp
#include <sys/stat.h>

bool is_regular_file(const std::string& path) {
    struct stat info;
    if (stat(path.c_str(), &info) != 0) return false;
#ifdef _WIN32
    return (info.st_mode & _S_IFMT) == _S_IFREG;   // MSVC 只有下划线版宏可靠
#else
    return S_ISREG(info.st_mode);
#endif
}
```

对外只暴露 `mrs::scan_path()`（自动分流到 `scan_zip`/`scan_dir`），CLI 与 JNI 都调它，
避免两处各写一遍判定逻辑。

## 其它已知差异

- **检出后的权限位**：POSIX 分支拿到的 `st_mode` 是真实值（含 `S_IFREG`），
  `mode & 04000` 判 setuid 才能生效；Windows 分支没有这个信息，只能传 0。
- **路径分隔符**：目录递归时 Windows 用 `\`、POSIX 用 `/`，两边各自拼；判定时统一用 `/` 归一。
- **行尾**：本仓库文件在 Windows 检出会变 CRLF，CI 上是 LF。断言脚本比对的是行内子串，未受影响。
- **大小写**：Windows 文件系统不区分大小写，Linux 区分；`module.prop` 这类固定名按原样拼，别依赖容错。
