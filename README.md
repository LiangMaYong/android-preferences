# android-preferences
this is android preferences

Welcome Star and Issues

## Use this
1,set
```
Preferences.getDefaultPreferences()
    .setInt("IntKey", 1)
    .setString("StringKey", "hi,android")
    .setString("StringKey", "hi,preferences");
```
2,get
```
Preferences.getDefaultPreferences().getString("StringKey");//hi,preferences


Preferences.getDefaultPreferences().getInt("IntKey",0);//1
```
3,register listener
```
Preferences.getDefaultPreferences().registerOnPreferenceChangeListener(new Preferences.OnPreferenceChangeListener() {
    @Override
    public void onChange(Preferences preference, String key) {
        //codeing
    }
});
```
4,unregister listener
```
unregisterOnPreferenceChangeListener(OnPreferenceChangeListener preferenceChangeListener);
```
##License
```
Copyright 2016 LiangMaYong

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
