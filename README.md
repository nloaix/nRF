一个关于nRF的OTA工程，借用了Leader之前的工程进行了修改（披皮工程哈哈哈哈哈哈ヾ(•ω•`)o）
第一版先这样吧，还存在一些问题，后期慢慢进行修改
目前的问题：
  1.会出现偶然性的传输失败问题，包数据没传输完直接断连后无法开机无法充电
  2.不可以选择文件路径（目前文件路径被固定）
  3.升级后需要重新退出程序后才可以升级下一台，排查后的原因可能是：OTA后Timer Handle被取消掉
  4.有现在用不到的按钮功能，代码不简洁
目前就这些问题，慢慢改吧！！！
实习之后就没碰JAVA了，好难。但是还是要加油啊(╹ڡ╹ )
