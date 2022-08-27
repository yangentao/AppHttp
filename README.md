# AppHttp
Android Library, http functions.

### config
```kotlin
//allow dump request and response info
HttpConfig.allowDumpByDebugFlag(this)
HttpConfig.debugPrinter = { Log.d("http", it) }
```


### example GET
```kotlin
//run in background thread
val resp = httpGet("https://entao.dev/") {
    "user" arg "tom"
}
val text = resp.valueText
Log.d("http", text ?: "null")
```

### example upload file
```kotlin
 val resp = httpMultipart(this, "https://entao.dev/") {
    "user" arg "tom"
    file("fileA", File("..."))
}
val text = resp.valueText
Log.d("http", text ?: "null")
```