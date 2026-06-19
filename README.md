# EhViewer CN FPlus
## 本APP有且仅在Github更新，所有自诩“官网”的均属虚假信息，请注意甄别

![Icon](fastlane/metadata/android/en-US/images/icon.png)

这是一个 E-Hentai Android 平台的浏览器（的修改版本，提供了更多至少对我很有帮助的功能）。

由于我是一个内向的孩子，不敢发PR——而且有很多修改我觉得不适合所有人。

而且在许多地方使用了vibe coding来修复bug。毕竟我不是很擅长android。

如果你喜欢这个作品，请给予最初的奠基者，CN_SXJ的仓库所有者一个大大的支持，我只是一个后来人。详见下面感谢名单。

# 相较上游的主要改动

## 阅读体验
- **页面书签**：在阅读器中可对任意页面添加书签并附加文字笔记；进度条上实时显示书签位置标记；通过左侧导航抽屉"书签"入口统一管理所有书签
- **失败重试**：可配置失败的自动重试次数
- **无上限缓存**：可配置大于原版640MB的无上限阅读缓存。（对我用处很大）

## 下载功能
- **下载重试设置**：可配置失败的自动重试次数
- **边看边下**：你可以边阅读边下载，但预加载不会被当作下载。
- 

## 搜索与发现
- **标签追踪**：可订阅指定标签，自动追踪新画廊并通知
- **收藏夹自动同步**：可选开启云收藏夹定时自动同步（从云端下载到本地）

## 其它
- **统计功能**：新增阅读统计页面，查看历史阅读数据

以及一些可能未写入这里的改动。

# 其它

如果你希望为这个项目发起issue，请提前想好：

1. 您的网络是否通畅（拥有连接墙外网站的能力）
2. 您的bug或问题是否可以复现（或者提供一种解决思路）
3. 您可以先问一问AI，或者确信是项目的锅再来。

# Screenshot

![screenshot-01](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png)


# Build

Windows

    > git clone https://github.com/tianriw/Ehviewer_CN_SXJ_FPlus.git
    > cd EhViewer
    > gradlew app:assembleDebug

Linux

    $ git clone https://github.com/tianriw/Ehviewer_CN_SXJ_FPlus.git
    $ cd EhViewer
    $ ./gradlew app:assembleDebug

生成的 apk 文件在 app\build\outputs\apk 目录下

The apk is in app\build\outputs\apk

# Thanks

## [感谢名单](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/blob/BiLi_PC_Gamer/feedauthor/thankyou.md) 

感谢Ehviewer奠基人[Hippo/seven332](https://github.com/seven332)    
Thanks to [Hippo/seven332](https://github.com/seven332), the founder of Ehviewer    

感谢Ehviewer_CN_SXJ的仓库所有者以及全部贡献者[xiaojieonly/Ehviewer_CN_SXJ](https://github.com/xiaojieonly/Ehviewer_CN_SXJ)。
Thanks to [xiaojieonly/Ehviewer_CN_SXJ](https://github.com/xiaojieonly/Ehviewer_CN_SXJ)'s owner and all contributors.

本项目受到了诸多开源项目的帮助  
This project has received help from many open source projects  

这是部分库
Here is the libraries  
- [AOSP](http://source.android.com/)
- [android-advancedrecyclerview](https://github.com/h6ah4i/android-advancedrecyclerview)
- [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/)
- [apng](http://apng.sourceforge.net/)
- [giflib](http://giflib.sourceforge.net)
- [greenDAO](https://github.com/greenrobot/greenDAO)
- [jsoup](https://github.com/jhy/jsoup)
- [libjpeg-turbo](http://libjpeg-turbo.virtualgl.org/)
- [libpng](http://www.libpng.org/pub/png/libpng.html)
- [okhttp](https://github.com/square/okhttp)
- [roaster](https://github.com/forge/roaster)
- [ShowcaseView](https://github.com/amlcurran/ShowcaseView)
- [Slabo](https://github.com/TiroTypeworks/Slabo)
- [TagSoup](http://home.ccil.org/~cowan/tagsoup/)