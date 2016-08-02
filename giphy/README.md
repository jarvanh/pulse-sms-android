# Giphy Android Search Library

To create a giphy search activity, you can use `Giphy.Builder`:

```java
new Giphy.Builder(activity, apiKey)
    .setMaxFileSize(1024 * 1024)
    .start(REQUEST_GIPHY);
```

Max file size is optional. In your activity, listen for the results:

```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_GIPHY) {
        if (resultCode == Activity.RESULT_OK) {
            Uri gif = data.getData();
            // do something with the uri.
        }
    } else {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
```

## License

    Copyright (C) 2016 Jake Klinker, Luke Klinker

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
