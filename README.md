CppStyle
========================
[![Build Status](https://travis-ci.org/wangzw/CppStyle.svg?branch=master)](https://travis-ci.org/wangzw/CppStyle)
[![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2192883 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

**An Eclipse plugin that integrates the clang-format tool as an alternative C/C++ code formatter and checks C++ coding style with the cpplint.py tool.**

## Description
A consistent coding style is important for a project. And many projects use tools to format the code and check coding style. Many developers use Eclipse as a C/C++ IDE, but it is a little difficult to integrate an external tool to Eclipse. People have to switch to a command line and run the tools to format the code and check the coding style. And then they need to switch back to Eclipse to find the line and fix the coding style issue based on the tool's output. For the "lazy" people like me, this is irritating. 

The expected behavious is that people just format the code fragment by first selecting it and then pressing `Command + Shift + f` on MacOS or `Ctrl + Shift + f` on Linux and other systems. Further more, the coding style checker is run whenever a file is saved and all the issues are marked on the editor. That is exactly what CppStyle does.

There are many C/C++ code format tools such as "[astyle](http://astyle.sourceforge.net/)" but currently **"[clang-format](http://clang.llvm.org/docs/ClangFormat.html)"** is my favorite. It has several pre-defined styles and is highly configurable.

**[cpplint.py](https://google.github.io/styleguide/cppguide.html#cpplint)** is a C++ coding style checker provided by google. It can be used to check the C++ code against the [Google C++ coding style](http://google-styleguide.googlecode.com/svn/trunk/cppguide.html). It can detect many style errors and maintain the consistency of coding style.

## Requirement
    cpplint.py     https://google.github.io/styleguide/cppguide.html#cpplint
    clang-format   http://clang.llvm.org/docs/ClangFormat.html

### Install cpplint.py on Linux/MacOS

    sudo curl -L "https://raw.githubusercontent.com/google/styleguide/gh-pages/cpplint/cpplint.py" -o /usr/bin/cpplint.py
    sudo chmod a+x /usr/bin/cpplint.py

### Install clang-format on Linux/MacOS
clanf-format can be built from llvm/clang source. But installing from binary is much easier.

For Ubuntu

    sudo apt-get install clang-format-3.4
    sudo ln -s /usr/bin/clang-format-3.4 /usr/bin/clang-format

On 64 bit platform, clang-format can also be downloaded from this [page](https://sublime.wbond.net/packages/Clang%20Format).

If you prefer, you can download the [entire LLVM toolchain](http://llvm.org/releases/download.html) and extract the clang-format binary yourself. Just extract the .tar.xz file and copy bin/clang-format into your PATH (e.g. /usr/local/bin). - Set the path to the clang-format binaries. 

## Installation

### Install from Eclipse Marketplace (Recommend)

[![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.png)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=2192883 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

**Drag the above button to your running Eclipse workspace**

[Go to Eclipse Marketplace page] (https://marketplace.eclipse.org/content/cppstyle) 

### Install from update site

CppStyle can be installed like other eclipse plugins from this site.

    http://www.cppstyle.com/update (Latest)
    http://www.cppstyle.com/oxygen (Eclipse Oxygen)
    http://www.cppstyle.com/mars (Eclipse Mars)
    http://www.cppstyle.com/luna (Eclipse Luna)
    http://www.cppstyle.com/kepler (Eclipse Kepler)

Go to **Help -> Install New Software** page, click **Add** button and then enter a name (`CppStyle`) and the above URL, and then click **OK**.

Select **CppStyle** from drop-down list and then check the name **CppStyle** listed in the page. And then click **Next** and **OK** until restart.

### Manual

* Build CppStye with maven first. ```mvn clean package```
* Install CppStyle with local update site ```file:///<YOUR_CODE_PATH>/update/target/site```

Restart Eclipse.

## Configure CppStyle

To configure CppStyle globally, go to **Preferences -> C/C++ -> CppStyle** dialog.

To configure CppStyle for a C/C++ project, go to **Project properties -> CppStyle** dialog.

To enable CppStyle(clang-format) as default C/C++ code formatter, go to **Preferences -> C/C++ -> Code Style -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"CppStyle (clang-format)"**

To enable CppStyle(clang-format) as C/C++ code formatter for a project, go to **Project properties -> C/C++ General -> Formatter** page and switch **"Code Formatter"** from **[built-in]** to **"CppStyle (clang-format)"**

## To configure clang-format

CppStyle does not support appending command line parameters to clang-format and cpplint.py. So, use their respective configuration files to do this.

CppStyle will pass the full absolute path of the source file to clang-format in command line. And clang-format will try to find the configuration file named **.clang-format** in the source file's path, and its parent's path if not found previously, and parent's path of the parent and so on.

So put the configuration file **.clang-format** into the project's root direcotry can make it work for all source files in the project.

Further more, you can also add the configuration file **.clang-format** into Eclipse workspace root directory to make it work for all projects in the workspace.

To generate the clang-format configuration file **.clang-format**:

    clang-format -dump-config -style=Google > .clang-format

**If no configure file named .clang-format is found, "-style=Google" will be passed to clang-format and Google style will be used by default.**

## To configure cpplint.py

By default, if you enable `cpplint.py` in **CppStyle** page, `cpplint.py` will be triggered everytime when you save a file. You can also trigger `cpplint.py` by click the button **Run C/C++ Code Analysis** in the popup menu when you right click the file, directory or the entire project in **Project Explorer**.

CppStyle will pass source file's full absolute path to `cpplint.py` in the command line. And `cpplint.py` also supports per-directory configuration by the configuration file named `CPPLINT.cfg`.

If **Root** is set in project property page, **--root=Root** will pass to `cpplint.py`.

`CPPLINT.cfg` file can contain a number of key=value pairs.
    Currently the following options are supported:

      set noparent
      filter=+filter1,-filter2,...
      exclude_files=regex
      linelength=80

To get the details of these options you can run the command:

    cpplint.py --help

## To enable or disable cpplint.py on specific issues or files

There are two ways to enable or disable cpplint.py on specific issues or files. The first and recommended one is to use configure file named `CPPLINT.cfg`. The benefit of using configure file is that it can be version controled and shared with others in a team. It also can produce the consistent result if you use `cpplint.py` in command line instead of CppStyle in Eclipse.

    The "filter" option is similar in function to --filter flag. It specifies
    message filters in addition to the |_DEFAULT_FILTERS| and those specified
    through --filter command-line flag.

    "exclude_files" allows to specify a regular expression to be matched against
    a file name. If the expression matches, the file is skipped and not run
    through liner.

The other way is to configure **Code Analysis** in **Perference -> C/C++ -> Code Analysis -> Cpplint Issues** globally, or in **Project property -> C/C++ General -> Code Analysis -> Cpplint Issues** for a C/C++ project. Eclipse Mars has [bug](https://bugs.eclipse.org/bugs/show_bug.cgi?id=471967) to prevent opening **Code Analysis** page in peoject's property.
