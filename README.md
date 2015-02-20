# AndroidProxySetter
An android app that sets the proxy settings for a wifi access point by using adb. 
This DOES NOT require root and will work with any device that has USB debugging on or any simulator (including Genymotion).

This app uses the Android Proxy Library from [here](https://github.com/shouldit/android-proxy/tree/master/android-proxy-library)

# Usage

Build and install the apk to your device, then perform the following actions. 

Set proxy by executing this command:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host <host> -e port <port> -e ssid <ssid> -e bypass <bypass string>
    
example:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e host 192.168.56.1 -e port 8080 -e ssid WiredSSID -e bypass test.com,test2.com

The proxy can be cleared for an ssid by executing the following:

	adb shell am start -n tk.elevenk.proxysetter/.MainActivity -e ssid <ssid> -e clear true
