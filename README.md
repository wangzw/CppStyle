CppStyle
========================
[![Build Status](https://travis-ci.org/wangzw/cppstyle.svg?branch=master)](https://travis-ci.org/wangzw/cppstyle)

**An eclipse plugin that integrate clang-format as an alternative c/c++ code formatter and check c++ coding style with cpplint.py**

## Description
A consistent coding style is important for a project. And many projects use tools to format the code and check coding style. Many developers use eclipse as a c/c++ IDE, but it is a little difficult to integrate an external tool to eclipse. People have to switch to a command line and run the tools to format the code and check coding style. And switch back to eclipse to find the line and fix the coding style issue based on the tool's output. For the "lazy" people like me, it is irritating. 

The expected behavious is that people just format the code fragment by first select it and then push "command + shift + f" on MacOS or "ctrl + shift + f" on other system. And further more, run the coding style checker on save and mark all issues on the editor. That is exactly what CppStyle does.

There are many c/c++ code format tools such as "[astyle](http://astyle.sourceforge.net/)" but currently **"[clang-format](http://clang.llvm.org/docs/ClangFormat.html)"** is my favorite. It has several pre-defined styles and is highly configurable.

**[cpplint.py](http://google-styleguide.googlecode.com/svn/trunk/cppguide.html#cpplint)** is a c++ coding style checker provided by google. It can be used to check the c++ code against the [google c++ coding style](http://google-styleguide.googlecode.com/svn/trunk/cppguide.html). It can detect many style errors and keep the consistency of coding style.

## Requirement
    cpplint.py     http://google-styleguide.googlecode.com/svn/trunk/cppguide.html#cpplint
    clang-format   http://clang.llvm.org/docs/ClangFormat.html

### Install cpplint.py on Linux/MacOS

    sudo curl -L "http://google-styleguide.googlecode.com/svn/trunk/cpplint/cpplint.py" -o /usr/bin/cpplint.py
    sudo chmod a+x /usr/bin/cpplint.py

### Install clang-format on Linux/MacOS
clanf-format can be built from llvm/clang source. But installing from binary is much easier.

For Ubuntu

    sudo apt-get install clang-format-3.4
    sudo ln -s /usr/bin/clang-format-3.4 /usr/bin/clang-format

On 64 bit platform, clang-format can also be downloaded from this [page](https://sublime.wbond.net/packages/Clang%20Format).

If you prefer, you can download the [entire LLVM toolchain](http://llvm.org/releases/download.html) and extract the clang-format binary yourself. Just extract the .tar.xz file and copy bin/clang-format into your PATH (e.g. /usr/local/bin). - Set the path to the clang-format binaries. 

## Installation

CppStyle can be installed like other eclipse plugins from the this site.

http://wangzw.github.io/cppstyle/update

## Configure
To configure CppStyle globally, go to **eclipse -> preferences -> CppStyle** page

To configure CppSytle for a c/c++ project, go to **project properties -> CppStyle** page.

To enable CppStyle(clang-format) as default c/c++ code formatter, go to **eclipse ->  preferences -> C/C++ -> Code Style -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"CppStyle (clang-format)"**

To enable CppStyle(clang-format) as c/c++ code formatter for a project, go to **project properties -> C/C++ General -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"CppStyle (clang-format)"**