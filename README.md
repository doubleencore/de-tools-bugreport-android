#Bug Reporter

A simple library to collect the database, cache, files and basic device information from an application.

## Getting started

In your `build.gradle`:

```gradle
repositories {

    maven {
        url "http://champa.dblenc.net:8081/nexus/content/repositories/releases/"
    }
}

 dependencies {
    debugCompile 'com.doubleencore:de-tools-bugreport-android:0.4.0'
    releaseCompile 'com.doubleencore:de-tools-bugreport-no-op-android:0.4.0'
 }
```

In your `Application` class:

```java
public class ExampleApplication extends Application {

  @Override public void onCreate() {
    super.onCreate();
    BugReport.setup(this);
  }
}
```

If you want to trigger a report from a screenshot, in your `Activity` or `Fragment` class:
```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onStart() {
        super.onStart();
        BugReport.enableObserver(this);
    }

    @Override
    protected void onStop() {
        BugReport.disableObserver(this);
        super.onStop();
    }
}
```

## Permissions

The library which should be included with debug builds requires `WRITE_EXTERNAL_STORAGE` and `READ_EXTERNAL_STORAGE`.  The no-op version does not have any permissions requirements.

If supporting M, the activity which enables the observer, or executes a collection will need to implement `onRequestPermissionResult()`:
```java
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        boolean granted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        switch (requestCode) {
            case BugReport.ENABLE_OBSERVER:
                if (granted) BugReport.enableObserver(this);
                break;
            case BugReport.EXECUTE_COLLECTION:
                if (granted) BugReport.executeCollection(this);
                break;
        }
    }
```

## Generating a report

To manually trigger a report:

```java
    BugReport.executeCollection(this);
```

Or if you have enabled screenshot monitoring, simply trigger a screenshot on the device.

Both will result in a notification displaying which will allow you send a .zip file of the contents.
