# 自动优选CF-IP并添加到CF对应的优选域名中

<img src="https://app.dartnode.com/assets/dash/images/brand/logo.png" class="header-brand-img desktop-logo" style="margin: 0; background-color: #333;" alt="DartVPS Logo">

Welcome to my open-source project! This project is proudly sponsored by [DartNode](https://dartnode.com/) as a part of their Open Source server program.

## About DartNode

[DartNode](https://dartnode.com/) is a division of Snaju® Inc.

Based in Houston, DartNode revolutionizes hosting with our Wholesale Cloud approach, combining affordability with high-performance in a state-of-the-art data center. As a dedicated ARIN Member and network operator, we ensure 24/7 reliability and superior service, empowering your digital journey with cost-effective solutions.

## Project Description

本程序目前仅支持在有Java环境的计算机中手动执行，有能力的可自行编译打包或改成shell脚本，放到软路由中自动执行

```
本地开发环境： IntelliJ IDEA 2023.2.2 | JDK 1.8.0_191 | Maven 3.6.1 | Git 2.25.0
```

## Usage

- 项目拉下来后，用IDEA打开，读取/设置为Maven项目，等待Maven依赖加载完毕
- 项目加载完毕后，在IDEA内双击shift，在弹框内搜索`cf.conf`，并打开，根据注释提示填写参数
- 参数填写完毕后，双击shift，在弹框内搜索`CloudflareApiTest.java`，并打开，在当前文件继续 Ctrl + F
  搜索 `autoPreferredDomain`，定位到方法入口，点击左边绿色三角，选择第一项，执行
- 参数填写无异常的情况下，等待执行结束即可

---

Please note that this project is proudly sponsored by [DartNode](https://dartnode.com/). Their support helps to keep this project alive and running.
