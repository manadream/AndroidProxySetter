[![Maven Central](https://maven-badges.herokuapp.com/maven-central/be.shouldit/android-proxy-library/badge.svg)](https://maven-badges.herokuapp.com/maven-central/be.shouldit/android-proxy-library)

# Android Proxy Library (APL) 
**APL** provides an abstraction layer to easily get the proxy settings from an Android device, in order to find a better and easy solution to the [Android's Issue 1273](http://www.android-proxy.com/2011/09/hello-world-issue-1273.html) for both developers and users. Since it has been clarified from [Google's spokesman](http://stackoverflow.com/questions/9446871/how-users-developers-can-set-the-androids-proxy-configuration-for-versions-2-x) that the versions 1.x-2.x won't receive an official proxy support, APL allows to abstract the device version and easily get the proxy settings on every released Android device.

# Core features
* Version agnostic support (supported all Android versions: 1.x - 2.x - 3.x - 4.x)
  * 1.x and 2.x versions support only one global proxy for every Wi-Fi AP.
  * 3.x and greater versions support Wi-Fi AP-based proxy settings.
* Proxy testing utilities (proxy reachability, web reachability)


# Try
* The core features of **APL** are shown on a **[DEMO](https://play.google.com/store/apps/details?id=com.lechucksoftware.proxy.lib.activities)** application on the Play Store
* **[Proxy Settings](https://play.google.com/store/apps/details?id=com.lechucksoftware.proxy.proxysettings)** makes use of all the advanced features of **APL** 

# How to use it
* Getting started [here](https://github.com/shouldit/android-proxy-library/wiki/Getting-Started).
* See how to [make an HTTP request](https://github.com/shouldit/android-proxy-library/wiki/Make-a-HTTP-Request) using the proxy.
* See how to [open a WebView](https://github.com/shouldit/android-proxy-library/wiki/Using-WebView-with-Proxy) that support the proxy on 1.x and 2.x devices.

# Source & Issues
If you have isolated a problem or want a new feature to be included in the **Android Proxy Library (APL)**, please [submit an issue](https://github.com/shouldit/android-proxy-library/issues/new). Make sure to include all the relevant information when you submit the issue such as:

* **Android Proxy Library (APL)** version
* Android device used (or emulator) with OS version
* One line of issue summary and a detailed description
* Any workarounds if you have them.

The more information you provide, the quicker the issue can be verified and prioritized. A test case (source code) that demonstrates the problem is greatly preferred.

# Project's resources

* All further information regarding this project can be found at: [www.android-proxy.com](www.android-proxy.com)
* If you have questions, write feedbacks, or just if you want to discuss regarding the Android's proxy issue topic, here you can find the official discussion group: [https://groups.google.com/d/forum/android-proxy-project](https://groups.google.com/d/forum/android-proxy-project)
